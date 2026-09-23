package com.sdt.web_app.entities.financial;

import com.sdt.web_app.entities.enrollment.StudentEnrollment;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Term;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "student_assessment_invoices")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StudentAssessmentInvoice {

    public enum InvoiceStatus {
        UNPAID, PARTIAL, PAID, FHE_COVERED, VOID
    }

    @Converter(autoApply = true)
    public static class InvoiceStatusConverter implements AttributeConverter<InvoiceStatus, String> {
        @Override
        public String convertToDatabaseColumn(InvoiceStatus attribute) {
            return attribute != null ? attribute.name() : InvoiceStatus.UNPAID.name();
        }

        @Override
        public InvoiceStatus convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) return InvoiceStatus.UNPAID;
            String norm = dbData.trim().toUpperCase();
            return switch (norm) {
                case "PARTIALLY_PAID" -> InvoiceStatus.PARTIAL;
                case "CANCELLED" -> InvoiceStatus.VOID;
                default -> {
                    try {
                        yield InvoiceStatus.valueOf(norm);
                    } catch (IllegalArgumentException e) {
                        yield InvoiceStatus.UNPAID;
                    }
                }
            };
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_enrollment_id", nullable = false)
    private StudentEnrollment studentEnrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(name = "total_tuition_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalTuitionFee = BigDecimal.ZERO;

    @Column(name = "total_lab_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalLabFee = BigDecimal.ZERO;

    @Column(name = "total_misc_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalMiscFee = BigDecimal.ZERO;

    @Column(name = "total_gross_assessment", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalGrossAssessment = BigDecimal.ZERO;

    @Column(name = "fhe_subsidy_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal fheSubsidyAmount = BigDecimal.ZERO;

    @Column(name = "scholarship_discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal scholarshipDiscountAmount = BigDecimal.ZERO;

    @Column(name = "net_assessed_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal netAssessedAmount = BigDecimal.ZERO;

    @Column(name = "total_paid_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalPaidAmount = BigDecimal.ZERO;

    @Column(name = "outstanding_balance", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    @Convert(converter = InvoiceStatusConverter.class)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.UNPAID;

    @Column(name = "is_fhe_eligible", nullable = false)
    @Builder.Default
    private boolean fheEligible = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
