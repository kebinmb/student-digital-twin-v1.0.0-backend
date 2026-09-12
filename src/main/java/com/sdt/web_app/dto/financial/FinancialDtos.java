package com.sdt.web_app.dto.financial;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class FinancialDtos {

    public record FeeTemplateDto(
            Long id,
            String name,
            Long academicYearId,
            Long campusId,
            BigDecimal tuitionPerUnit,
            BigDecimal labFeePerUnit,
            BigDecimal miscellaneousFlatFee,
            BigDecimal athleticFlatFee,
            boolean active
    ) {}

    public record CreateFeeTemplateRequest(
            @NotBlank(message = "Template name is required")
            String name,

            @NotNull(message = "Academic year ID is required")
            Long academicYearId,

            Long campusId,

            @NotNull @DecimalMin(value = "0.00")
            BigDecimal tuitionPerUnit,

            @NotNull @DecimalMin(value = "0.00")
            BigDecimal labFeePerUnit,

            @NotNull @DecimalMin(value = "0.00")
            BigDecimal miscellaneousFlatFee,

            @NotNull @DecimalMin(value = "0.00")
            BigDecimal athleticFlatFee
    ) {}

    public record StudentAssessmentInvoiceDto(
            Long id,
            String invoiceNumber,
            Long studentEnrollmentId,
            Long studentProfileId,
            String studentNumber,
            String studentName,
            Long termId,
            String termName,
            BigDecimal totalTuitionFee,
            BigDecimal totalLabFee,
            BigDecimal totalMiscFee,
            BigDecimal totalGrossAssessment,
            BigDecimal fheSubsidyAmount,
            BigDecimal scholarshipDiscountAmount,
            BigDecimal netAssessedAmount,
            BigDecimal totalPaidAmount,
            BigDecimal outstandingBalance,
            String status,
            boolean fheEligible
    ) {}

    public record StudentAccountLedgerDto(
            Long id,
            String transactionNumber,
            Long studentProfileId,
            Long termId,
            Long assessmentInvoiceId,
            String transactionType,
            String transactionDate,
            String description,
            BigDecimal debitAmount,
            BigDecimal creditAmount,
            BigDecimal runningBalance,
            String referenceNumber
    ) {}

    public record ProcessPaymentRequest(
            @NotNull(message = "Student profile ID is required")
            Long studentProfileId,

            Long assessmentInvoiceId,

            @NotNull @DecimalMin(value = "0.01", message = "Amount tendered must be greater than zero")
            BigDecimal amountTendered,

            @NotNull @DecimalMin(value = "0.01", message = "Amount paid must be greater than zero")
            BigDecimal amountPaid,

            @NotBlank(message = "Payment method is required")
            String paymentMethod,

            String referenceNumber,
            String remarks
    ) {}

    public record CashierReceiptDto(
            Long id,
            String orNumber,
            Long studentProfileId,
            String studentNumber,
            String studentName,
            Long assessmentInvoiceId,
            BigDecimal amountTendered,
            BigDecimal amountPaid,
            BigDecimal changeAmount,
            String paymentMethod,
            String referenceNumber,
            String remarks,
            String status,
            Long cashierUserId,
            String cashierUsername,
            String issuedAt
    ) {}

    public record UnifastFheClaimDto(
            Long id,
            String claimBatchNumber,
            Long termId,
            String termName,
            Long campusId,
            String campusName,
            Integer totalBeneficiaries,
            BigDecimal totalTuitionClaimed,
            BigDecimal totalTosfClaimed,
            BigDecimal totalClaimAmount,
            String status,
            String createdByUsername,
            String createdAt,
            List<UnifastFheClaimItemDto> items
    ) {}

    public record UnifastFheClaimItemDto(
            Long id,
            Long claimBatchId,
            Long studentProfileId,
            String studentNumber,
            String studentName,
            String programCode,
            BigDecimal enrolledUnits,
            BigDecimal tuitionAmount,
            BigDecimal miscAmount,
            BigDecimal labAmount,
            BigDecimal totalClaimedAmount,
            String verificationStatus
    ) {}

    public record CreateUnifastClaimRequest(
            @NotNull(message = "Term ID is required")
            Long termId,

            @NotNull(message = "Campus ID is required")
            Long campusId
    ) {}
}
