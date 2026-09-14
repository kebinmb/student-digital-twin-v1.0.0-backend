package com.sdt.web_app.entities.enrollment;

import com.sdt.web_app.entities.scheduling.ClassSection;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "enrollment_course_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"enrollment", "section"})
public class EnrollmentCourseItem {

    public enum CompletionStatus {
        ENROLLED, IN_PROGRESS, PASSED, FAILED, INCOMPLETE, DROPPED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enrollment_id", nullable = false)
    @Setter
    private StudentEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private ClassSection section;

    @Column(name = "final_numerical_grade", precision = 3, scale = 2)
    private BigDecimal finalNumericalGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status", nullable = false, length = 20)
    @Builder.Default
    private CompletionStatus completionStatus = CompletionStatus.ENROLLED;

    public void updateGrade(BigDecimal grade, CompletionStatus status) {
        this.finalNumericalGrade = grade;
        this.completionStatus = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EnrollmentCourseItem that)) return false;
        if (id != null && that.id != null) {
            return Objects.equals(id, that.id);
        }
        return Objects.equals(section, that.section);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        return Objects.hash(section);
    }
}
