package com.sdt.web_app.entities.enrollment;

import com.sdt.web_app.entities.institution.Term;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "student_enrollments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"student", "term", "items"})
public class StudentEnrollment {

    public enum Status {
        DRAFT, ENLISTED, ASSESSED, ENROLLED, DROPPED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(name = "enrollment_date", nullable = false)
    @Builder.Default
    private Instant enrollmentDate = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Column(name = "total_credit_units", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal totalCreditUnits = BigDecimal.ZERO;

    @Column(name = "is_overload_approved", nullable = false)
    @Builder.Default
    private boolean isOverloadApproved = false;

    @OneToMany(mappedBy = "enrollment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<EnrollmentCourseItem> items = new LinkedHashSet<>();

    public void updateStatus(Status newStatus) {
        this.status = newStatus;
    }

    public void setOverloadApproved(boolean approved) {
        this.isOverloadApproved = approved;
    }

    public void addItem(EnrollmentCourseItem item) {
        items.add(item);
        item.setEnrollment(this);
    }

    public void removeItem(EnrollmentCourseItem item) {
        items.remove(item);
        item.setEnrollment(null);
    }

    public void recalculateUnits() {
        this.totalCreditUnits = items.stream()
                .map(item -> item.getSection().getCourse().getCreditUnits())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentEnrollment that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
