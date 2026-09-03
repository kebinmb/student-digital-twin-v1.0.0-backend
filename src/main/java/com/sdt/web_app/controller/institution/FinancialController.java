package com.sdt.web_app.controller.institution;

import com.sdt.web_app.dto.institution.FinancialDtos.*;
import com.sdt.web_app.entities.institution.FeeCatalog;
import com.sdt.web_app.entities.institution.FeeCategory;
import com.sdt.web_app.entities.institution.PaymentTermTemplate;
import com.sdt.web_app.entities.institution.ScholarshipDiscount;
import com.sdt.web_app.service.institution.FinancialStructureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FinancialController {

    private final FinancialStructureService financialService;

    // -------------------------------------------------------------------------
    // Fee Categories (/api/v1/fee-categories)
    // -------------------------------------------------------------------------
    @GetMapping("/fee-categories")
    public ResponseEntity<List<FeeCategoryResponse>> getAllFeeCategories() {
        List<FeeCategoryResponse> list = financialService.getAllFeeCategories().stream()
                .map(this::mapToCategoryResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/fee-categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public ResponseEntity<FeeCategoryResponse> createFeeCategory(@Valid @RequestBody CreateFeeCategoryRequest request) {
        FeeCategory category = financialService.createFeeCategory(request.code(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToCategoryResponse(category));
    }

    @PutMapping("/fee-categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public ResponseEntity<FeeCategoryResponse> updateFeeCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFeeCategoryRequest request
    ) {
        FeeCategory category = financialService.updateFeeCategory(id, request.name());
        return ResponseEntity.ok(mapToCategoryResponse(category));
    }

    @DeleteMapping("/fee-categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> deleteFeeCategory(@PathVariable Long id) {
        financialService.deleteFeeCategory(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Fee Catalog (/api/v1/fee-catalog)
    // -------------------------------------------------------------------------
    @GetMapping("/fee-catalog")
    public ResponseEntity<List<FeeCatalogResponse>> getAllFeeCatalog() {
        List<FeeCatalogResponse> list = financialService.getAllFeeCategories().stream()
                .flatMap(cat -> financialService.getFeeCatalogByCategory(cat.getId()).stream())
                .map(this::mapToCatalogResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/fee-catalog/category/{categoryId}")
    public ResponseEntity<List<FeeCatalogResponse>> getFeeCatalogByCategory(@PathVariable Long categoryId) {
        List<FeeCatalogResponse> list = financialService.getFeeCatalogByCategory(categoryId).stream()
                .map(this::mapToCatalogResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/fee-catalog")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public ResponseEntity<FeeCatalogResponse> createFeeCatalog(@Valid @RequestBody CreateFeeCatalogRequest request) {
        FeeCatalog fee = financialService.createFeeCatalog(
                request.categoryId(),
                request.code(),
                request.name(),
                request.defaultAmount(),
                request.isPerUnit(),
                request.isChedSanctioned(),
                request.isFheBillable()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToCatalogResponse(fee));
    }

    @PutMapping("/fee-catalog/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public ResponseEntity<FeeCatalogResponse> updateFeeCatalog(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFeeCatalogRequest request
    ) {
        FeeCatalog fee = financialService.updateFeeCatalogPricing(id, request.name(), request.defaultAmount(), request.isPerUnit());
        return ResponseEntity.ok(mapToCatalogResponse(fee));
    }

    @DeleteMapping("/fee-catalog/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> deleteFeeCatalog(@PathVariable Long id) {
        financialService.deleteFeeCatalog(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Payment Term Templates (/api/v1/payment-term-templates)
    // -------------------------------------------------------------------------
    @GetMapping("/payment-term-templates")
    public ResponseEntity<List<PaymentTermTemplateResponse>> getAllPaymentTermTemplates() {
        List<PaymentTermTemplateResponse> list = financialService.getAllPaymentTermTemplates().stream()
                .map(this::mapToTemplateResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/payment-term-templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public ResponseEntity<PaymentTermTemplateResponse> createPaymentTermTemplate(
            @Valid @RequestBody CreatePaymentTermTemplateRequest request
    ) {
        PaymentTermTemplate template = financialService.createPaymentTermTemplate(
                request.name(),
                request.downpaymentPercentage(),
                request.prelimPercentage(),
                request.midtermPercentage(),
                request.semiFinalPercentage(),
                request.finalPercentage()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToTemplateResponse(template));
    }

    @DeleteMapping("/payment-term-templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> deletePaymentTermTemplate(@PathVariable Long id) {
        financialService.deletePaymentTermTemplate(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Scholarship Discounts (/api/v1/scholarship-discounts)
    // -------------------------------------------------------------------------
    @GetMapping("/scholarship-discounts")
    public ResponseEntity<List<ScholarshipDiscountResponse>> getAllScholarshipDiscounts() {
        List<ScholarshipDiscountResponse> list = financialService.getAllScholarshipDiscounts().stream()
                .map(this::mapToScholarshipResponse)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/scholarship-discounts")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public ResponseEntity<ScholarshipDiscountResponse> createScholarshipDiscount(
            @Valid @RequestBody CreateScholarshipDiscountRequest request
    ) {
        ScholarshipDiscount discount = financialService.createScholarshipDiscount(
                request.code(),
                request.name(),
                request.type(),
                request.category(),
                request.discountPercentage(),
                request.fixedAmount(),
                request.fundingSource(),
                request.appliesToTuition(),
                request.appliesToMisc()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToScholarshipResponse(discount));
    }

    @DeleteMapping("/scholarship-discounts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> deleteScholarshipDiscount(@PathVariable Long id) {
        financialService.deleteScholarshipDiscount(id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Response Mappers
    // -------------------------------------------------------------------------
    private FeeCategoryResponse mapToCategoryResponse(FeeCategory c) {
        return new FeeCategoryResponse(c.getId(), c.getCode(), c.getName());
    }

    private FeeCatalogResponse mapToCatalogResponse(FeeCatalog f) {
        return new FeeCatalogResponse(
                f.getId(),
                f.getCategory().getId(),
                f.getCategory().getCode().name(),
                f.getCategory().getName(),
                f.getCode(),
                f.getName(),
                f.getDefaultAmount(),
                f.isPerUnit(),
                f.isChedSanctioned(),
                f.isFheBillable()
        );
    }

    private PaymentTermTemplateResponse mapToTemplateResponse(PaymentTermTemplate p) {
        return new PaymentTermTemplateResponse(
                p.getId(),
                p.getName(),
                p.getDownpaymentPct(),
                p.getPrelimPct(),
                p.getMidtermPct(),
                p.getSemifinalPct(),
                p.getFinalPct()
        );
    }

    private ScholarshipDiscountResponse mapToScholarshipResponse(ScholarshipDiscount s) {
        return new ScholarshipDiscountResponse(
                s.getId(),
                s.getCode(),
                s.getName(),
                s.getType(),
                s.getCategory(),
                s.getDiscountPercentage(),
                s.getFixedAmount(),
                s.getFundingSource(),
                s.isAppliesToTuition(),
                s.isAppliesToMisc(),
                true
        );
    }
}
