package com.sdt.web_app.entities.enrollment;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Program;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "student_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
@BatchSize(size = 50)
public class StudentProfile {

    public enum EnrollmentStatus {
        REGULAR, IRREGULAR, PROBATION, LOA, GRADUATED
    }

    public enum StudentClassification {
        INCOMING_FIRST_YEAR, TRANSFEREE, RETURNEE, CONTINUING
    }

    public enum ClearanceStatus {
        CLEARED, PENDING, BLOCKED, HOLD
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @ToString.Exclude
    @JsonIgnore
    private User user;

    @Column(name = "student_number", nullable = false, unique = true, length = 30)
    private String studentNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curriculum_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Curriculum curriculum;

    @Column(name = "year_level", nullable = false)
    @Builder.Default
    private int yearLevel = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "enrollment_status", nullable = false, length = 20)
    @Builder.Default
    private EnrollmentStatus enrollmentStatus = EnrollmentStatus.REGULAR;

    @Enumerated(EnumType.STRING)
    @Column(name = "student_classification", nullable = false, length = 30)
    @Builder.Default
    private StudentClassification classification = StudentClassification.CONTINUING;

    @Column(name = "is_graduating", nullable = false)
    @Builder.Default
    private boolean isGraduating = false;

    @Column(name = "total_units_earned", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal totalUnitsEarned = BigDecimal.ZERO;

    @Column(name = "cumulative_gpa", precision = 3, scale = 2)
    private BigDecimal cumulativeGpa;

    @Enumerated(EnumType.STRING)
    @Column(name = "financial_clearance", nullable = false, length = 20)
    @Builder.Default
    private ClearanceStatus financialClearance = ClearanceStatus.CLEARED;

    @Enumerated(EnumType.STRING)
    @Column(name = "departmental_clearance", nullable = false, length = 20)
    @Builder.Default
    private ClearanceStatus departmentalClearance = ClearanceStatus.CLEARED;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public void updateAcademicStanding(int yearLevel, EnrollmentStatus status, boolean isGraduating) {
        this.yearLevel = yearLevel;
        this.enrollmentStatus = status;
        this.isGraduating = isGraduating;
    }

    public void updateAcademicStanding(int yearLevel, EnrollmentStatus status, boolean isGraduating, StudentClassification classification) {
        this.yearLevel = yearLevel;
        this.enrollmentStatus = status;
        this.isGraduating = isGraduating;
        if (classification != null) {
            this.classification = classification;
        }
    }

    public void updateClassification(StudentClassification classification) {
        if (classification != null) {
            this.classification = classification;
        }
    }

    public void updateProgress(BigDecimal totalUnitsEarned, BigDecimal cumulativeGpa) {
        this.totalUnitsEarned = totalUnitsEarned;
        this.cumulativeGpa = cumulativeGpa;
    }

    public boolean isClearedForEnrollment() {
        return financialClearance == ClearanceStatus.CLEARED && departmentalClearance == ClearanceStatus.CLEARED;
    }

    public void updateClearance(ClearanceStatus financialClearance, ClearanceStatus departmentalClearance) {
        if (financialClearance != null) {
            this.financialClearance = financialClearance;
        }
        if (departmentalClearance != null) {
            this.departmentalClearance = departmentalClearance;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        StudentProfile that = (StudentProfile) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
