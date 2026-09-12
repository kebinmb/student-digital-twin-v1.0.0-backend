package com.sdt.web_app.entities.compliance;

import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Term;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "graduation_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraduationApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curriculum_id", nullable = false)
    private Curriculum curriculum;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    @Column(name = "degree_audit_status", nullable = false, length = 30)
    @Builder.Default
    private String degreeAuditStatus = "PENDING";

    @Column(name = "total_units_completed", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal totalUnitsCompleted = BigDecimal.ZERO;

    @Column(name = "cumulative_gpa", precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal cumulativeGpa = BigDecimal.ZERO;

    @Column(name = "honors_status", length = 50)
    @Builder.Default
    private String honorsStatus = "NONE";

    @Column(name = "special_order_number", length = 100)
    private String specialOrderNumber;

    @Column(name = "special_order_issued_at")
    private LocalDate specialOrderIssuedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
