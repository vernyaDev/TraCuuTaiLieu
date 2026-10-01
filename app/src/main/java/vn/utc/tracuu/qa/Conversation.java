package vn.utc.tracuu.qa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Một phiên hội thoại.
 *
 * <p>Hội thoại sống theo PHIÊN và không giữ qua phiên — ranh giới phạm vi đã chốt. Cột tóm tắt
 * phục vụ hội thoại dài: khi vượt số lượt giữ lại, phần cũ được cuộn thành một đoạn tóm tắt
 * thay vì cắt bỏ, để câu hỏi tiếp nối vẫn còn chỗ bám.
 */
@Entity
@Table(name = "conversations")
@Getter @Setter @NoArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt = Instant.now();

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "turn_count", nullable = false)
    private int turnCount = 0;

    public Conversation(Long userId) {
        this.userId = userId;
    }
}
