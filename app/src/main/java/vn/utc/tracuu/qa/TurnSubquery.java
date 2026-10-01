package vn.utc.tracuu.qa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Một truy vấn con của lượt. Nguồn cho khối "các bước đã thực hiện". */
@Entity
@Table(name = "turn_subqueries")
@Getter @Setter @NoArgsConstructor
public class TurnSubquery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turn_id", nullable = false)
    private Long turnId;

    @Column(nullable = false)
    private int ordinal;

    @Column(name = "depends_on_ordinal")
    private Integer dependsOnOrdinal;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @Column(name = "hit_count", nullable = false)
    private int hitCount;
}
