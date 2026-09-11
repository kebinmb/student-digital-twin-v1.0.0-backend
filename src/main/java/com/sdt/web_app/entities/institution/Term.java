package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(
        name = "terms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ay_term", columnNames = {
                        "academic_year_id", "term_type"
                })
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = "academicYear")
@BatchSize(size = 50)
public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false, updatable = false)
    private AcademicYear academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "term_type", nullable = false, length = 20, updatable = false)
    private TermType termType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "enrollment_open", nullable = false)
    @Builder.Default
    private boolean enrollmentOpen = false;

    @Column(name = "grading_open", nullable = false)
    @Builder.Default
    private boolean gradingOpen = false;

    @Column(name = "add_drop_open", nullable = false)
    @Builder.Default
    private boolean addDropOpen = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = false;

    @Column(name = "max_hours_per_class", nullable = false, precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal maxHoursPerClass = new BigDecimal("3.0");

    public void updateMaxHoursPerClass(BigDecimal maxHours) {
        if (maxHours == null || maxHours.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Maximum hours per class session must be greater than 0");
        }
        this.maxHoursPerClass = maxHours;
    }

    public void updateSchedule(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be prior to start date");
        }
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void openEnrollment() {
        this.enrollmentOpen = true;
    }

    public void closeEnrollment() {
        this.enrollmentOpen = false;
    }

    public void openGrading() {
        this.gradingOpen = true;
    }

    public void closeGrading() {
        this.gradingOpen = false;
    }

    public void openAddDrop() {
        this.addDropOpen = true;
    }

    public void closeAddDrop() {
        this.addDropOpen = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Term term)) return false;

        String thisAyCode = academicYear != null ? academicYear.getCode() : null;
        String otherAyCode = term.academicYear != null ? term.academicYear.getCode() : null;

        return termType != null
                && Objects.equals(termType, term.termType)
                && Objects.equals(thisAyCode, otherAyCode);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}