package vn.utc.tracuu.qa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cảnh báo kèm lượt: tài liệu hết hiệu lực, đã bị thay thế, hoặc chồng lấn. */
@Entity
@Table(name = "turn_warnings")
@Getter @Setter @NoArgsConstructor
public class TurnWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turn_id", nullable = false)
    private Long turnId;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(name = "document_id")
    private Long documentId;

    @Column(length = 500)
    private String detail;
}
