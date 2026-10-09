package com.sdt.web_app.controller.financial;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.financial.FinancialDtos.*;
import com.sdt.web_app.service.financial.CashieringService;
import com.sdt.web_app.service.financial.FeeAssessmentService;
import com.sdt.web_app.service.financial.OrBookletService;
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
    private final OrBookletService orBookletService;
    private final SecurityUtils securityUtils;

    @Auditable(action = "CREATE_FEE_TEMPLATE", entityName = "FeeTemplate")
    @PostMapping("/fee-templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN', 'ACCOUNTANT')")
    public ResponseEntity<FeeTemplateDto> createFeeTemplate(@Valid @RequestBody CreateFeeTemplateRequest request, Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        FeeTemplateDto result = feeAssessmentService.createFeeTemplate(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Auditable(action = "READ_ACTIVE_FEE_TEMPLATE", entityName = "FeeTemplate")
    @GetMapping("/fee-templates/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'ACCOUNTANT', 'CASHIER', 'DEAN', 'CHAIRPERSON', 'STUDENT')")
    public ResponseEntity<FeeTemplateDto> getActiveFeeTemplate(@RequestParam(name = "academicYearId", required = false) Long academicYearId) {
        FeeTemplateDto result = feeAssessmentService.getActiveFeeTemplate(academicYearId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "ASSESS_ENROLLMENT", entityName = "StudentAssessmentInvoice", entityId = "#enrollmentId")
    @PostMapping("/assess/{enrollmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'ACCOUNTANT', 'CASHIER')")
    public ResponseEntity<StudentAssessmentInvoiceDto> assessEnrollment(@PathVariable Long enrollmentId, Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        StudentAssessmentInvoiceDto result = feeAssessmentService.assessEnrollment(enrollmentId, actorUserId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "ADJUST_ASSESSMENT_ADD_DROP", entityName = "StudentAssessmentInvoice", entityId = "#enrollmentId")
    @PostMapping("/assess/{enrollmentId}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'ACCOUNTANT', 'CASHIER')")
    public ResponseEntity<StudentAssessmentInvoiceDto> adjustAssessmentForAddDrop(@PathVariable Long enrollmentId, Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        StudentAssessmentInvoiceDto result = feeAssessmentService.adjustAssessmentForAddDrop(enrollmentId, actorUserId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "READ_INVOICE_BY_ENROLLMENT", entityName = "StudentAssessmentInvoice", entityId = "#enrollmentId")
    @GetMapping("/invoices/enrollment/{enrollmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'ACCOUNTANT', 'CASHIER', 'STUDENT')")
    public ResponseEntity<StudentAssessmentInvoiceDto> getInvoiceByEnrollmentId(@PathVariable Long enrollmentId) {
        StudentAssessmentInvoiceDto result = feeAssessmentService.getInvoiceByEnrollmentId(enrollmentId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "READ_INVOICE_BY_STUDENT_TERM", entityName = "StudentAssessmentInvoice", entityId = "#studentProfileId")
    @GetMapping("/invoices/student/{studentProfileId}/term/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'CASHIER', 'REGISTRAR') or @enrollmentSecurity.canAccessStudentAdvising(authentication, #studentProfileId)")
    public ResponseEntity<StudentAssessmentInvoiceDto> getInvoiceByStudentAndTerm(
            @PathVariable Long studentProfileId,
            @PathVariable Long termId) {
        StudentAssessmentInvoiceDto result = feeAssessmentService.getInvoiceByStudentAndTerm(studentProfileId, termId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "READ_STUDENT_LEDGER", entityName = "StudentAccountLedger", entityId = "#studentProfileId")
    @GetMapping("/ledgers/student/{studentProfileId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'CASHIER', 'REGISTRAR') or @enrollmentSecurity.canAccessStudentAdvising(authentication, #studentProfileId)")
    public ResponseEntity<List<StudentAccountLedgerDto>> getStudentLedgerHistory(@PathVariable Long studentProfileId) {
        List<StudentAccountLedgerDto> result = feeAssessmentService.getStudentLedgerHistory(studentProfileId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "PROCESS_PAYMENT", entityName = "CashierReceipt")
    @PostMapping("/payments")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'CASHIER')")
    public ResponseEntity<CashierReceiptDto> processPayment(@Valid @RequestBody ProcessPaymentRequest request, Authentication authentication) {
        Long cashierUserId = securityUtils.resolveUserId(authentication);
        CashierReceiptDto result = cashieringService.processPayment(request, cashierUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Auditable(action = "READ_RECEIPT_BY_OR", entityName = "CashierReceipt", entityId = "#orNumber")
    @GetMapping("/receipts/{orNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'CASHIER', 'REGISTRAR')")
    public ResponseEntity<CashierReceiptDto> getReceiptByOrNumber(@PathVariable String orNumber) {
        CashierReceiptDto result = cashieringService.getReceiptByOrNumber(orNumber);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "READ_RECEIPTS_BY_STUDENT", entityName = "CashierReceipt", entityId = "#studentProfileId")
    @GetMapping("/receipts/student/{studentProfileId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'CASHIER', 'REGISTRAR') or @enrollmentSecurity.canAccessStudentAdvising(authentication, #studentProfileId)")
    public ResponseEntity<List<CashierReceiptDto>> getReceiptsByStudentProfile(@PathVariable Long studentProfileId) {
        List<CashierReceiptDto> result = cashieringService.getReceiptsByStudentProfile(studentProfileId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "READ_RECEIPTS_BY_STUDENT_SLICE", entityName = "CashierReceipt", entityId = "#studentProfileId")
    @GetMapping("/receipts/student/{studentProfileId}/slice")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'CASHIER', 'REGISTRAR') or @enrollmentSecurity.canAccessStudentAdvising(authentication, #studentProfileId)")
    public ResponseEntity<com.sdt.web_app.dto.common.SliceResponse<CashierReceiptDto>> getReceiptsByStudentProfileSlice(
            @PathVariable Long studentProfileId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", required = false) String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "DESC") String sortDir) {
        return ResponseEntity.ok(cashieringService.getReceiptsByStudentProfileSlice(studentProfileId, page, size, sortBy, sortDir));
    }

    @Auditable(action = "CREATE_OR_BOOKLET", entityName = "OrBooklet")
    @PostMapping("/or-booklets")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'CASHIER')")
    public ResponseEntity<OrBookletDto> createOrBooklet(@Valid @RequestBody CreateOrBookletRequest request) {
        OrBookletDto result = orBookletService.createBooklet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Auditable(action = "READ_ACTIVE_OR_BOOKLET", entityName = "OrBooklet")
    @GetMapping("/or-booklets/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'CASHIER')")
    public ResponseEntity<OrBookletDto> getActiveBookletForCashier(Authentication authentication) {
        Long cashierUserId = securityUtils.resolveUserId(authentication);
        OrBookletDto result = orBookletService.getActiveBookletForCashier(cashierUserId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "READ_CASHIER_OR_BOOKLETS", entityName = "OrBooklet")
    @GetMapping("/or-booklets")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'CASHIER')")
    public ResponseEntity<?> getCashierBooklets(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            Authentication authentication) {
        Long cashierUserId = securityUtils.resolveUserId(authentication);
        if (page != null && size != null) {
            return ResponseEntity.ok(orBookletService.getCashierBooklets(cashierUserId, org.springframework.data.domain.PageRequest.of(page, size)));
        }
        List<OrBookletDto> result = orBookletService.getCashierBooklets(cashierUserId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "VOID_OFFICIAL_RECEIPT", entityName = "VoidedOfficialReceipt")
    @PostMapping("/or-booklets/void")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'CASHIER')")
    public ResponseEntity<VoidedOfficialReceiptDto> voidOfficialReceipt(@Valid @RequestBody VoidOfficialReceiptRequest request, Authentication authentication) {
        Long cashierUserId = securityUtils.resolveUserId(authentication);
        VoidedOfficialReceiptDto result = orBookletService.voidOfficialReceipt(request, cashierUserId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "GENERATE_EOD_RCD_REPORT", entityName = "EodRcdReport")
    @GetMapping("/cashier/eod-rcd")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'CASHIER')")
    public ResponseEntity<EodRcdReportDto> generateEodRcdReport(@RequestParam(name = "date", required = false) String date, Authentication authentication) {
        Long cashierUserId = securityUtils.resolveUserId(authentication);
        EodRcdReportDto result = cashieringService.generateEodRcdReport(cashierUserId, date);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "GENERATE_UNIFAST_CLAIM", entityName = "UnifastFheClaim")
    @PostMapping("/unifast/claims")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'REGISTRAR')")
    public ResponseEntity<UnifastFheClaimDto> generateUnifastClaimBatch(@Valid @RequestBody CreateUnifastClaimRequest request, Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        UnifastFheClaimDto result = unifastBillingService.generateUnifastClaimBatch(request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @Auditable(action = "READ_UNIFAST_CLAIMS_BY_TERM", entityName = "UnifastFheClaim", entityId = "#termId")
    @GetMapping("/unifast/claims/term/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'REGISTRAR', 'CASHIER')")
    public ResponseEntity<List<UnifastFheClaimDto>> getClaimsByTerm(@PathVariable Long termId) {
        List<UnifastFheClaimDto> result = unifastBillingService.getClaimsByTerm(termId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "READ_UNIFAST_CLAIM_BATCH", entityName = "UnifastFheClaim", entityId = "#claimBatchId")
    @GetMapping("/unifast/claims/{claimBatchId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'REGISTRAR', 'CASHIER')")
    public ResponseEntity<UnifastFheClaimDto> getClaimBatchDetails(@PathVariable Long claimBatchId) {
        UnifastFheClaimDto result = unifastBillingService.getClaimBatch(claimBatchId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "DISALLOW_UNIFAST_CLAIM_ITEM", entityName = "UnifastFheClaimItem", entityId = "#itemId")
    @PutMapping("/unifast/claims/items/{itemId}/disallow")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'REGISTRAR', 'CASHIER')")
    public ResponseEntity<UnifastFheClaimItemDto> disallowClaimItem(@PathVariable Long itemId, @Valid @RequestBody DisallowClaimItemRequest request, Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        UnifastFheClaimItemDto result = unifastBillingService.disallowClaimItem(itemId, request, actorUserId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "EXPORT_UNIFAST_FORM2_CSV", entityName = "UnifastFheClaim", entityId = "#claimBatchId")
    @GetMapping(value = "/unifast/claims/{claimBatchId}/form2/export", produces = "text/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT', 'REGISTRAR', 'CASHIER')")
    public ResponseEntity<String> exportForm2Csv(@PathVariable Long claimBatchId) {
        String csvContent = unifastBillingService.exportForm2Csv(claimBatchId);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"UniFAST_Form_2_Batch_" + claimBatchId + ".csv\"")
                .body(csvContent);
    }

    @Auditable(action = "PROCESS_LINKBIZ_WEBHOOK", entityName = "CashierReceipt")
    @PostMapping("/gateways/linkbiz/webhook")
    public ResponseEntity<LinkBizWebhookResponse> processLinkBizWebhook(@Valid @RequestBody LinkBizWebhookRequest request) {
        LinkBizWebhookResponse response = cashieringService.processLinkBizPayment(request);
        return ResponseEntity.ok(response);
    }
}

