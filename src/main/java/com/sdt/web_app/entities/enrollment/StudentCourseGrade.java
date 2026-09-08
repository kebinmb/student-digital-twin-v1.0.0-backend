package com.sdt.web_app.entities.enrollment;

import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.Term;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "student_course_grades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"student", "course", "term"})
public class StudentCourseGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private Term term;

    @Column(name = "numerical_grade", nullable = false, precision = 3, scale = 2)
    private BigDecimal numericalGrade;

    @Column(name = "completion_status", nullable = false, length = 20)
    @Builder.Default
    private String completionStatus = "PASSED";

    @Column(name = "is_credited", nullable = false)
    @Builder.Default
    private boolean isCredited = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public void updateGrade(BigDecimal numericalGrade, String completionStatus) {
        this.numericalGrade = numericalGrade;
        if (completionStatus != null) {
            this.completionStatus = completionStatus;
        }
    }

    public boolean isPassed() {
        return "PASSED".equalsIgnoreCase(this.completionStatus)
                && this.numericalGrade != null
                && this.numericalGrade.compareTo(new BigDecimal("3.00")) <= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentCourseGrade that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
