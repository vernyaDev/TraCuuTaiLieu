package vn.utc.tracuu.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.utc.tracuu.access.PermissionResolver;
import vn.utc.tracuu.access.PermissionScope;
import vn.utc.tracuu.access.ReadableDocuments;
import vn.utc.tracuu.ingestion.FileStorage;
import vn.utc.tracuu.platform.web.BusinessException;

import java.nio.file.Path;
import java.util.List;

/**
 * Đọc nội dung tài liệu — <strong>mọi đường đều qua đây, không có ngoại lệ</strong>.
 *
 * <p>Nguyên tắc bốn của hợp đồng giao diện: mọi đường dẫn tới nội dung tài liệu đều đi qua cùng
 * một bộ giải quyết quyền. Không có ngoại lệ cho "đường tải tệp" hay "đường xem trước" — đó
 * chính là hai chỗ mà một hệ thống có phân quyền đúng vẫn rò rỉ, vì chúng trông như chức năng
 * phụ trợ nên hay được viết tắt.
 *
 * <p><strong>Quyền kiểm TẠI THỜI ĐIỂM GỌI</strong>, không dựa vào việc trích dẫn đã hiện trước
 * đó. Một trích dẫn nằm trong lịch sử hội thoại từ hôm qua không phải là giấy phép đọc hôm nay:
 * quyền có thể đã bị thu hồi trong khoảng giữa, và {@code AC-007} của US-008 đòi đúng điều đó.
 */
@Service
public class DocumentReadService {

    /** Số đoạn lấy quanh đoạn được trích dẫn, mỗi bên. */
    private static final int WINDOW = 2;

    private final DocumentRepository documents;
    private final ChunkRepository chunks;
    private final PermissionResolver permissions;
    private final FileStorage storage;

    public DocumentReadService(DocumentRepository documents, ChunkRepository chunks,
                               PermissionResolver permissions, FileStorage storage) {
        this.documents = documents;
        this.chunks = chunks;
        this.permissions = permissions;
        this.storage = storage;
    }

    public record ChunkView(Long id, int ordinal, String breadcrumb, String content,
                            boolean table, Integer tablePart, Integer tablePartsTotal,
                            boolean highlighted) {}

    public record DocumentView(Long id, String title, String originalFilename,
                               String category, String sensitivityLabel,
                               java.time.LocalDate effectiveFrom, java.time.LocalDate effectiveTo,
                               boolean hasNoExpiry, boolean expired,
                               List<ChunkView> chunks) {}

    /**
     * Nội dung tài liệu quanh một đoạn được trích dẫn.
     *
     * <p>Không đọc được thì ném <em>không tìm thấy</em>, giống hệt trường hợp tài liệu không tồn
     * tại. Trả <em>không đủ quyền</em> ở đây là xác nhận tài liệu đó có thật — và người hỏi chỉ
     * cần so hai phản hồi khác nhau là biết được điều họ không được biết.
     */
    @Transactional(readOnly = true)
    public DocumentView readAround(long chunkId, PermissionScope scope, long chunkSetId) {
        Chunk anchor = chunks.findById(chunkId).orElseThrow(BusinessException::khongTimThay);
        requireReadable(anchor.getDocumentId(), scope);

        Document doc = documents.findById(anchor.getDocumentId())
                .orElseThrow(BusinessException::khongTimThay);

        List<Chunk> window = chunks.findWindow(chunkSetId, doc.getId(),
                Math.max(0, anchor.getOrdinal() - WINDOW), anchor.getOrdinal() + WINDOW);

        return toView(doc, window, chunkId);
    }

    /** Toàn bộ đoạn của một tài liệu, để xem trước bản đã tách. */
    @Transactional(readOnly = true)
    public DocumentView readAll(long documentId, PermissionScope scope, long chunkSetId) {
        requireReadable(documentId, scope);
        Document doc = documents.findById(documentId).orElseThrow(BusinessException::khongTimThay);
        List<Chunk> all = chunks.findByChunkSetIdAndDocumentIdOrderByOrdinal(chunkSetId, documentId);
        return toView(doc, all, null);
    }

    /**
     * Tệp gốc để tải về.
     *
     * <p>Kiểm quyền LẦN NỮA ở đây. Không có đường tải nào bỏ qua bước này, kể cả khi người gọi
     * vừa mở được phần nội dung một giây trước.
     */
    @Transactional(readOnly = true)
    public DownloadTarget originalFile(long documentId, PermissionScope scope) {
        requireReadable(documentId, scope);
        Document doc = documents.findById(documentId).orElseThrow(BusinessException::khongTimThay);
        Path path = storage.resolve(doc.getStoragePath());
        if (!java.nio.file.Files.isReadable(path)) {
            throw BusinessException.khongTimThay();
        }
        return new DownloadTarget(path, doc.getOriginalFilename());
    }

    public record DownloadTarget(Path path, String filename) {}

    /** Cửa kiểm duy nhất. Mọi phương thức công khai ở trên đều bắt đầu bằng một lời gọi tới đây. */
    private void requireReadable(long documentId, PermissionScope scope) {
        ReadableDocuments readable = permissions.resolve(scope);
        if (!readable.permits(documentId)) {
            throw BusinessException.khongTimThay();
        }
    }

    private DocumentView toView(Document doc, List<Chunk> list, Long highlightChunkId) {
        List<ChunkView> views = list.stream()
                .map(c -> new ChunkView(c.getId(), c.getOrdinal(), c.getBreadcrumb(), c.getContent(),
                        c.isTable(), c.getTablePart(), c.getTablePartsTotal(),
                        highlightChunkId != null && highlightChunkId.equals(c.getId())))
                .toList();

        return new DocumentView(
                doc.getId(), doc.getTitle(), doc.getOriginalFilename(),
                doc.getCategory() == null ? null : doc.getCategory().getName(),
                null,
                doc.getEffectiveFrom(), doc.getEffectiveTo(), doc.isHasNoExpiry(),
                doc.isExpired(java.time.LocalDate.now()),
                views);
    }
}
