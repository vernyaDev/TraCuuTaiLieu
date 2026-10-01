package vn.utc.tracuu.access;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Một mục kiểm soát truy cập trên một tài liệu — cho phép hoặc từ chối, cho người hoặc phòng ban. */
@Entity
@Table(name = "document_acl")
@Getter @Setter @NoArgsConstructor
public class DocumentAcl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 20)
    private SubjectType subjectType;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AclEffect effect;

    @Column(name = "granted_by")
    private Long grantedBy;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt = Instant.now();

    public DocumentAcl(Long documentId, SubjectType subjectType, Long subjectId,
                       AclEffect effect, Long grantedBy) {
        this.documentId = documentId;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.effect = effect;
        this.grantedBy = grantedBy;
    }
}
