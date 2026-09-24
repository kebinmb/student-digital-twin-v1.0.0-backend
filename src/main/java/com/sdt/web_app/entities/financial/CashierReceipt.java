package com.sdt.web_app.entities.financial;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cashier_receipts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CashierReceipt {

    public enum PaymentMethod {
        CASH, ONLINE_BANKING, GCASH, MAYA, LINKBIZ, CHECK, BANK_TRANSFER, SCHOLARSHIP
    }

    public enum ReceiptStatus {
        VALID, VOID
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "or_number", nullable = false, unique = true, length = 50)
    private String orNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_invoice_id")
    private StudentAssessmentInvoice assessmentInvoice;

    @Column(name = "amount_tendered", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal amountTendered = BigDecimal.ZERO;

    @Column(name = "amount_paid", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "change_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal changeAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "check_number", length = 50)
    private String checkNumber;

    @Column(name = "drawee_bank", length = 100)
    private String draweeBank;

    @Column(length = 255)
    private String remarks;

    @Column(name = "fund_cluster_code", length = 20)
    @Builder.Default
    private String fundClusterCode = "FUND_164";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReceiptStatus status = ReceiptStatus.VALID;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_user_id", nullable = false)
    private User cashierUser;

    @Column(name = "issued_at", updatable = false)
    @Builder.Default
    private Instant issuedAt = Instant.now();

    @Column(name = "voided_at")
    private Instant voidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voided_by_user_id")
    private User voidedByUser;

    @Column(name = "void_reason", length = 255)
    private String voidReason;
}
