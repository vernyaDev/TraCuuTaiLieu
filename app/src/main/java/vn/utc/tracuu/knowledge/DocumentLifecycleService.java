package vn.utc.tracuu.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.utc.tracuu.access.PermissionAdminService;
import vn.utc.tracuu.ingestion.ChunkWriter;
import vn.utc.tracuu.ingestion.FileStorage;
import vn.utc.tracuu.platform.web.BusinessException;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Vòng đời tài liệu: khai bản thay thế, gỡ khỏi kho, xoá vĩnh viễn.
 *
 * <p>Nguyên tắc nền của cả nhóm chức năng này: <strong>văn bản không biến mất, chúng bị thay
 * thế</strong>. Tài liệu đã gỡ vẫn nằm trong cơ sở dữ liệu, chỉ là không tham gia truy hồi —
 * nhờ vậy một trích dẫn cũ vẫn tra ngược được, và câu hỏi "quy định này trước đây ra sao" vẫn
 * trả lời được.
 */
@Service
public class DocumentLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(DocumentLifecycleService.class);

    /** Độ sâu tối đa khi dò chuỗi thay thế — chặn vòng lặp cả khi dữ liệu đã hỏng từ trước. */
    private static final int MAX_CHAIN_DEPTH = 50;

    private final DocumentRepository documents;
    private final ChunkWriter chunkWriter;
    private final FileStorage storage;
    private final PermissionAdminService permissionAdmin;

    public DocumentLifecycleService(DocumentRepository documents, ChunkWriter chunkWriter,
                                    FileStorage storage, PermissionAdminService permissionAdmin) {
        this.documents = documents;
        this.chunkWriter = chunkWriter;
        this.storage = storage;
        this.permissionAdmin = permissionAdmin;
    }

    /**
     * Khai rằng {@code newId} thay thế {@code oldId}.
     *
     * <p><strong>Vòng lặp bị chặn khi ghi, không chặn bằng ràng buộc cơ sở dữ liệu.</strong> Lý
     * do: ràng buộc bắt được A thay thế chính A, nhưng không bắt được A thay B, B thay C, C thay
     * A — chu trình nhiều bước cần đi hết chuỗi mới thấy. Bỏ qua nó thì mọi truy vấn lần theo
     * chuỗi thay thế sẽ chạy vô hạn.
     */
    @Transactional
    public void declareSupersession(long newId, long oldId, long actorId) {
        if (newId == oldId) {
            throw new BusinessException(ErrorCode.VONG_THAY_THE, null,
                    "Tài liệu không thể thay thế chính nó");
        }
        Document newDoc = documents.findById(newId).orElseThrow(BusinessException::khongTimThay);
        documents.findById(oldId).orElseThrow(BusinessException::khongTimThay);

        if (createsCycle(newId, oldId)) {
            throw new BusinessException(ErrorCode.VONG_THAY_THE, null,
                    "Khai báo này tạo vòng lặp trong chuỗi thay thế");
        }

        newDoc.setSupersedesDocumentId(oldId);
        documents.save(newDoc);
        log.info("Tài liệu {} được khai là bản thay thế của {}", newId, oldId);
    }

    /**
     * Đi ngược chuỗi từ {@code oldId} xem có gặp lại {@code newId} không.
     *
     * <p>Kèm giới hạn độ sâu: nếu dữ liệu đã có vòng lặp từ trước (do nhập tay, do khôi phục
     * bản sao lưu), phép dò này vẫn phải dừng chứ không được treo.
     */
    private boolean createsCycle(long newId, long oldId) {
        Set<Long> seen = new HashSet<>();
        Long cursor = oldId;
        int depth = 0;

        while (cursor != null && depth++ < MAX_CHAIN_DEPTH) {
            if (cursor == newId) return true;
            if (!seen.add(cursor)) return true;   // vòng lặp có sẵn trong dữ liệu
            cursor = documents.findById(cursor)
                    .map(Document::getSupersedesDocumentId)
                    .orElse(null);
        }
        return depth >= MAX_CHAIN_DEPTH;
    }

    /** Chuỗi thay thế đầy đủ, từ bản mới nhất ngược về bản gốc. */
    @Transactional(readOnly = true)
    public List<Document> supersessionChain(long documentId) {
        List<Document> chain = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        Long cursor = documentId;

        while (cursor != null && chain.size() < MAX_CHAIN_DEPTH && seen.add(cursor)) {
            Document d = documents.findById(cursor).orElse(null);
            if (d == null) break;
            chain.add(d);
            cursor = d.getSupersedesDocumentId();
        }
        return chain;
    }

    /**
     * Gỡ tài liệu khỏi kho.
     *
     * <p>Đoạn vẫn giữ nguyên — chỉ trạng thái đổi, và truy hồi lọc theo trạng thái nên tài liệu
     * này thôi xuất hiện trong câu trả lời mới. Trích dẫn cũ vẫn mở được, vì người đọc cần hiểu
     * câu trả lời cũ dựa trên cái gì.
     */
    @Transactional
    public void withdraw(long documentId, long actorId) {
        Document doc = documents.findById(documentId).orElseThrow(BusinessException::khongTimThay);
        if (doc.getStatus() == DocumentStatus.DA_GO) return;

        doc.setStatus(DocumentStatus.DA_GO);
        documents.save(doc);
        // Trạng thái tài liệu đổi làm tập được phép đổi theo — đây là thay đổi quyền.
        permissionAdmin.setSensitivity(documentId, doc.getSensitivity(), actorId);
        log.info("Gỡ tài liệu {} khỏi kho", documentId);
    }

    @Transactional
    public void restore(long documentId, long actorId) {
        Document doc = documents.findById(documentId).orElseThrow(BusinessException::khongTimThay);
        if (doc.getStatus() != DocumentStatus.DA_GO) {
            throw new BusinessException(ErrorCode.TRANG_THAI_KHONG_HOP_LE, null,
                    "Chỉ khôi phục được tài liệu đang ở trạng thái đã gỡ");
        }
        doc.setStatus(doc.getIndexedAt() == null
                ? DocumentStatus.CHO_XAC_NHAN : DocumentStatus.DA_LAP_CHI_MUC);
        documents.save(doc);
        permissionAdmin.setSensitivity(documentId, doc.getSensitivity(), actorId);
    }

    /** Con số hậu quả của việc xoá vĩnh viễn — hiện TRƯỚC khi hỏi xác nhận. */
    public record DeleteImpact(long documentId, String title, long citationCount,
                               long supersededByCount, boolean isPartOfChain) {}

    @Transactional(readOnly = true)
    public DeleteImpact deleteImpact(long documentId) {
        Document doc = documents.findById(documentId).orElseThrow(BusinessException::khongTimThay);
        long citations = documents.countCitations(documentId);
        long successors = documents.findBySupersedesDocumentId(documentId).size();
        return new DeleteImpact(documentId, doc.getTitle(), citations, successors,
                successors > 0 || doc.getSupersedesDocumentId() != null);
    }

    /**
     * Xoá vĩnh viễn: xoá thật, cả tệp gốc lẫn các đoạn.
     *
     * <p>Từng tài liệu một, có xác nhận riêng. Hệ thống <strong>cố ý không có đường xoá hàng
     * loạt</strong> — xoá hàng loạt là thứ chỉ cần sai một lần là mất cả kho, và không có thao
     * tác hoàn tác nào bù lại được.
     */
    @Transactional
    public void deletePermanently(long documentId, long actorId) {
        Document doc = documents.findById(documentId).orElseThrow(BusinessException::khongTimThay);

        // Gỡ liên kết ở các bản kế nhiệm trước: xoá thẳng sẽ để lại con trỏ tới một tài liệu
        // không còn tồn tại, và chuỗi thay thế đứt giữa chừng mà không ai biết vì sao.
        for (Document successor : documents.findBySupersedesDocumentId(documentId)) {
            successor.setSupersedesDocumentId(null);
            documents.save(successor);
        }

        chunkWriter.deleteForDocument(documentId);
        String path = doc.getStoragePath();
        documents.delete(doc);
        storage.delete(path);

        log.warn("Xoá vĩnh viễn tài liệu {} ({}) bởi người dùng {}",
                documentId, doc.getTitle(), actorId);
    }

    /** Xác nhận metadata hệ thống đề xuất, đưa tài liệu vào chỉ mục. */
    @Transactional
    public void confirmMetadata(long documentId, long actorId) {
        Document doc = documents.findById(documentId).orElseThrow(BusinessException::khongTimThay);
        doc.setMetadataConfirmed(true);
        if (doc.getStatus() == DocumentStatus.CHO_XAC_NHAN) {
            doc.setStatus(DocumentStatus.DA_LAP_CHI_MUC);
            doc.setIndexedAt(Instant.now());
        }
        documents.save(doc);
    }
}
