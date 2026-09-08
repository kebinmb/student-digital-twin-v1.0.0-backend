package com.sdt.web_app.entities.enrollment;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.institution.Course;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "course_equivalencies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"student", "internalCourse", "approvedBy"})
public class CourseEquivalency {

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @Column(name = "external_institution", nullable = false, length = 150)
    private String externalInstitution;

    @Column(name = "external_course_code", nullable = false, length = 30)
    private String externalCourseCode;

    @Column(name = "external_course_title", nullable = false, length = 150)
    private String externalCourseTitle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "internal_course_id", nullable = false)
    private Course internalCourse;

    @Column(name = "external_numerical_grade", nullable = false, precision = 3, scale = 2)
    private BigDecimal externalNumericalGrade;

    @Column(name = "credits_granted", nullable = false, precision = 4, scale = 2)
    private BigDecimal creditsGranted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.APPROVED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @Column(length = 255)
    private String remarks;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public void updateStatus(Status status, User approvedBy, String remarks) {
        this.status = status;
        this.approvedBy = approvedBy;
        this.remarks = remarks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CourseEquivalency that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
