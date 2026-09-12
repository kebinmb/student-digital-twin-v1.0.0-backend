package com.sdt.web_app.entities.financial;

import com.sdt.web_app.entities.enrollment.StudentProfile;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "unifast_fhe_claim_items")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UnifastFheClaimItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_batch_id", nullable = false)
    private UnifastFheClaim claimBatch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_invoice_id", nullable = false)
    private StudentAssessmentInvoice assessmentInvoice;

    @Column(name = "enrolled_units", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal enrolledUnits = BigDecimal.ZERO;

    @Column(name = "tuition_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal tuitionAmount = BigDecimal.ZERO;

    @Column(name = "misc_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal miscAmount = BigDecimal.ZERO;

    @Column(name = "lab_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal labAmount = BigDecimal.ZERO;

    @Column(name = "total_claimed_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalClaimedAmount = BigDecimal.ZERO;

    @Column(name = "verification_status", nullable = false, length = 30)
    @Builder.Default
    private String verificationStatus = "VERIFIED";
}
