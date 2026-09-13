package com.sdt.web_app.controller.financial;

import com.sdt.web_app.dto.financial.FinancialDtos.*;
import com.sdt.web_app.service.financial.CashieringService;
import com.sdt.web_app.service.financial.FeeAssessmentService;
import com.sdt.web_app.service.financial.UnifastBillingService;
import com.sdt.web_app.service.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinancialManagementController {

    private final FeeAssessmentService feeAssessmentService;
    private final CashieringService cashieringService;
    private final UnifastBillingService unifastBillingService;
    private final SecurityUtils securityUtils;

    @PostMapping("/fee-templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN', 'ACCOUNTANT')")
    public ResponseEntity<FeeTemplateDto> createFeeTemplate(@Valid @RequestBody CreateFeeTemplateRequest request, Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        FeeTemplateDto result = feeAssessmentService.createFeeTemplate(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/fee-templates/active")
    public ResponseEntity<FeeTemplateDto> getActiveFeeTemplate(@RequestParam(name = "academicYearId", required = false) Long academicYearId) {
        FeeTemplateDto result = feeAssessmentService.getActiveFeeTemplate(academicYearId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/assess/{enrollmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'ACCOUNTANT', 'CASHIER')")
    public ResponseEntity<StudentAssessmentInvoiceDto> assessEnrollment(@PathVariable Long enrollmentId, Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        StudentAssessmentInvoiceDto result = feeAssessmentService.assessEnrollment(enrollmentId, actorUserId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/invoices/enrollment/{enrollmentId}")
    public ResponseEntity<StudentAssessmentInvoiceDto> getInvoiceByEnrollmentId(@PathVariable Long enrollmentId) {
        StudentAssessmentInvoiceDto result = feeAssessmentService.getInvoiceByEnrollmentId(enrollmentId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/invoices/student/{studentProfileId}/term/{termId}")
    public ResponseEntity<StudentAssessmentInvoiceDto> getInvoiceByStudentAndTerm(
            @PathVariable Long studentProfileId,
            @PathVariable Long termId) {
        StudentAssessmentInvoiceDto result = feeAssessmentService.getInvoiceByStudentAndTerm(studentProfileId, termId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/ledgers/student/{studentProfileId}")
    public ResponseEntity<List<StudentAccountLedgerDto>> getStudentLedgerHistory(@PathVariable Long studentProfileId) {
        List<StudentAccountLedgerDto> result = feeAssessmentService.getStudentLedgerHistory(studentProfileId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'CASHIER')")
    public ResponseEntity<CashierReceiptDto> processPayment(@Valid @RequestBody ProcessPaymentRequest request, Authentication authentication) {
        Long cashierUserId = securityUtils.resolveUserId(authentication);
        CashierReceiptDto result = cashieringService.processPayment(request, cashierUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/receipts/{orNumber}")
    public ResponseEntity<CashierReceiptDto> getReceiptByOrNumber(@PathVariable String orNumber) {
        CashierReceiptDto result = cashieringService.getReceiptByOrNumber(orNumber);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/receipts/student/{studentProfileId}")
    public ResponseEntity<List<CashierReceiptDto>> getReceiptsByStudentProfile(@PathVariable Long studentProfileId) {
        List<CashierReceiptDto> result = cashieringService.getReceiptsByStudentProfile(studentProfileId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/receipts/student/{studentProfileId}/slice")
    public ResponseEntity<com.sdt.web_app.dto.common.SliceResponse<CashierReceiptDto>> getReceiptsByStudentProfileSlice(
            @PathVariable Long studentProfileId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "DESC") String sortDir) {
        return ResponseEntity.ok(cashieringService.getReceiptsByStudentProfileSlice(studentProfileId, page, size, sortBy, sortDir));
    }

    @PostMapping("/unifast/claims")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'REGISTRAR')")
    public ResponseEntity<UnifastFheClaimDto> generateUnifastClaimBatch(@Valid @RequestBody CreateUnifastClaimRequest request, Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        UnifastFheClaimDto result = unifastBillingService.generateUnifastClaimBatch(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/unifast/claims/term/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'REGISTRAR')")
    public ResponseEntity<List<UnifastFheClaimDto>> getClaimsByTerm(@PathVariable Long termId) {
        List<UnifastFheClaimDto> result = unifastBillingService.getClaimsByTerm(termId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/unifast/claims/{claimBatchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'REGISTRAR')")
    public ResponseEntity<UnifastFheClaimDto> getClaimBatchDetails(@PathVariable Long claimBatchId) {
        UnifastFheClaimDto result = unifastBillingService.getClaimBatch(claimBatchId);
        return ResponseEntity.ok(result);
    }
}
