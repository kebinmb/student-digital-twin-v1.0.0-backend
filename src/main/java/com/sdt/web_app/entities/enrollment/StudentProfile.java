package com.sdt.web_app.entities.enrollment;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Program;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "student_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"user", "program", "curriculum"})
public class StudentProfile {

    public enum EnrollmentStatus {
        REGULAR, IRREGULAR, PROBATION, LOA, GRADUATED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "student_number", nullable = false, unique = true, length = 30)
    private String studentNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curriculum_id", nullable = false)
    private Curriculum curriculum;

    @Column(name = "year_level", nullable = false)
    @Builder.Default
    private int yearLevel = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "enrollment_status", nullable = false, length = 20)
    @Builder.Default
    private EnrollmentStatus enrollmentStatus = EnrollmentStatus.REGULAR;

    @Column(name = "is_graduating", nullable = false)
    @Builder.Default
    private boolean isGraduating = false;

    @Column(name = "total_units_earned", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal totalUnitsEarned = BigDecimal.ZERO;

    @Column(name = "cumulative_gpa", precision = 3, scale = 2)
    private BigDecimal cumulativeGpa;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public void updateAcademicStanding(int yearLevel, EnrollmentStatus status, boolean isGraduating) {
        this.yearLevel = yearLevel;
        this.enrollmentStatus = status;
        this.isGraduating = isGraduating;
    }

    public void updateProgress(BigDecimal totalUnitsEarned, BigDecimal cumulativeGpa) {
        this.totalUnitsEarned = totalUnitsEarned;
        this.cumulativeGpa = cumulativeGpa;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentProfile that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
