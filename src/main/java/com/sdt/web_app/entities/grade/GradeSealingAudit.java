package com.sdt.web_app.entities.grade;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.scheduling.ClassSection;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "grade_sealing_audits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"section", "registrarUser"})
public class GradeSealingAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private ClassSection section;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrar_user_id", nullable = false)
    private User registrarUser;

    @Column(name = "student_records_sealed", nullable = false)
    private int studentRecordsSealed;

    @Column(name = "section_code", nullable = false, length = 30)
    private String sectionCode;

    @Column(name = "course_code", nullable = false, length = 30)
    private String courseCode;

    @Column(name = "sealed_at", updatable = false)
    @Builder.Default
    private Instant sealedAt = Instant.now();

    @Column(name = "checksum_hash", length = 64)
    private String checksumHash;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GradeSealingAudit that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
