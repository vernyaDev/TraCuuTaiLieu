package vn.utc.tracuu.knowledge;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một đoạn văn bản đã tách, thuộc đúng một tập đoạn.
 *
 * <p><strong>Cột {@code embedding} cố ý KHÔNG ánh xạ ở đây.</strong> Kiểu vector là kiểu riêng
 * của phần mở rộng cơ sở dữ liệu; ép nó qua tầng ánh xạ quan hệ đối tượng thì vừa phải đăng ký
 * kiểu tuỳ biến, vừa nạp cả mảng 768 số mỗi lần đọc một đoạn — trong khi mọi truy vấn thật sự
 * cần vector đều là truy vấn gốc nhiều mệnh đề (xem mô hình dữ liệu Mục 7). Ghi và đọc vector
 * đi thẳng qua tầng JDBC ở {@code ChunkWriter} và {@code Retriever}.
 */
@Entity
@Table(name = "chunks")
@Getter @Setter @NoArgsConstructor
public class Chunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chunk_set_id", nullable = false)
    private Long chunkSetId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private int ordinal;

    /** Đường dẫn phân cấp: {@code Chương VII › Điều 113 › Khoản 2}. */
    @Column(length = 700)
    private String breadcrumb;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    /** Văn bản đã tách từ tiếng Việt. Chỉ mục toàn văn sinh từ cột NÀY, không từ {@code content}. */
    @Column(name = "content_segmented", nullable = false, columnDefinition = "text")
    private String contentSegmented;

    @Column(name = "token_count", nullable = false)
    private int tokenCount;

    @Column(name = "is_table", nullable = false)
    private boolean table = false;

    @Column(name = "table_part")
    private Integer tablePart;

    @Column(name = "table_parts_total")
    private Integer tablePartsTotal;

    /** Vị trí trong tài liệu GỐC — cầu nối để đáp án chuẩn ánh xạ sang từng tập đoạn. */
    @Column(name = "char_start")
    private Integer charStart;

    @Column(name = "char_end")
    private Integer charEnd;
}
