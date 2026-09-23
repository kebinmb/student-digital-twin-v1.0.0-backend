package com.sdt.web_app.entities.financial;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Term;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "student_account_ledgers")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StudentAccountLedger {

    public enum TransactionType {
        CHARGE, PAYMENT, ADJUSTMENT, FHE_SUBSIDY, DISCOUNT
    }

    @Converter(autoApply = true)
    public static class TransactionTypeConverter implements AttributeConverter<TransactionType, String> {
        @Override
        public String convertToDatabaseColumn(TransactionType attribute) {
            return attribute != null ? attribute.name() : TransactionType.CHARGE.name();
        }

        @Override
        public TransactionType convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) return TransactionType.CHARGE;
            String norm = dbData.trim().toUpperCase();
            return switch (norm) {
                case "UNIFAST_SUBSIDY", "FHE_SUBSIDY_CREDIT" -> TransactionType.FHE_SUBSIDY;
                case "ASSESSMENT", "GROSS_ASSESSMENT" -> TransactionType.CHARGE;
                case "CASHIER_PAYMENT" -> TransactionType.PAYMENT;
                default -> {
                    try {
                        yield TransactionType.valueOf(norm);
                    } catch (IllegalArgumentException e) {
                        yield TransactionType.CHARGE;
                    }
                }
            };
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_number", nullable = false, unique = true, length = 50)
    private String transactionNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile studentProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_invoice_id")
    private StudentAssessmentInvoice assessmentInvoice;

    @Convert(converter = TransactionTypeConverter.class)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    @Column(name = "transaction_date", nullable = false, updatable = false)
    @Builder.Default
    private Instant transactionDate = Instant.now();

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "debit_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal debitAmount = BigDecimal.ZERO;

    @Column(name = "credit_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(name = "running_balance", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal runningBalance = BigDecimal.ZERO;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
