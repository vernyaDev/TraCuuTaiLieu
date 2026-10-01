package vn.utc.tracuu.knowledge;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.utc.tracuu.platform.security.CurrentUser;
import vn.utc.tracuu.platform.web.BusinessException;
import vn.utc.tracuu.platform.web.ErrorCode;

import java.nio.charset.StandardCharsets;

/**
 * Đọc tài liệu từ một trích dẫn — hợp đồng giao diện Mục 4.
 *
 * <p>Đây là đường dành cho <strong>mọi người dùng</strong>, không phải khu quản trị. Nó chỉ có
 * ba việc: mở nội dung quanh một đoạn, xem toàn bộ đoạn, và tải tệp gốc. Cả ba đều đi qua cùng
 * một bộ giải quyết quyền.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentReadService reader;
    private final ChunkSetRepository chunkSets;

    public DocumentController(DocumentReadService reader, ChunkSetRepository chunkSets) {
        this.reader = reader;
        this.chunkSets = chunkSets;
    }

    /** Nội dung quanh một đoạn được trích dẫn, đoạn ấy được đánh dấu để giao diện làm nổi bật. */
    @GetMapping("/by-chunk/{chunkId}")
    public DocumentReadService.DocumentView aroundChunk(@PathVariable Long chunkId) {
        return reader.readAround(chunkId, CurrentUser.requireScope(), activeChunkSetId());
    }

    @GetMapping("/{documentId}/chunks")
    public DocumentReadService.DocumentView allChunks(@PathVariable Long documentId) {
        return reader.readAll(documentId, CurrentUser.requireScope(), activeChunkSetId());
    }

    /**
     * Tải tệp gốc.
     *
     * <p>Tên tệp mã hoá theo chuẩn cho phép ký tự ngoài bảng mã ASCII — tên tài liệu tiếng Việt
     * có dấu sẽ hỏng nếu đặt thẳng vào phần tiêu đề phản hồi.
     */
    @GetMapping("/{documentId}/file")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long documentId) {
        DocumentReadService.DownloadTarget target =
                reader.originalFile(documentId, CurrentUser.requireScope());

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(target.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(target.path()));
    }

    private long activeChunkSetId() {
        return chunkSets.findByActiveTrue()
                .map(ChunkSet::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOI_HE_THONG, null,
                        "Chưa có tập đoạn hoạt động"));
    }
}
