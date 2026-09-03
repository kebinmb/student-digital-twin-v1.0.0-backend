package com.sdt.web_app.service.institution;

import com.sdt.web_app.entities.institution.*;
import com.sdt.web_app.repositories.institution.FeeCatalogRepository;
import com.sdt.web_app.repositories.institution.FeeCategoryRepository;
import com.sdt.web_app.repositories.institution.PaymentTermTemplateRepository;
import com.sdt.web_app.repositories.institution.ScholarshipDiscountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class FinancialStructureService {

    private final FeeCategoryRepository feeCategoryRepository;
    private final FeeCatalogRepository feeCatalogRepository;
    private final PaymentTermTemplateRepository paymentTermTemplateRepository;
    private final ScholarshipDiscountRepository scholarshipDiscountRepository;

    // -------------------------------------------------------------------------
    // FeeCategory Operations
    // -------------------------------------------------------------------------

    public FeeCategory createFeeCategory(FeeCategoryCode code, String name) {
        if (feeCategoryRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Fee category with code already exists: " + code);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Fee category name cannot be blank");
        }

        FeeCategory category = FeeCategory.builder()
                .code(code)
                .name(name.trim())
                .build();

        return feeCategoryRepository.save(category);
    }

    public FeeCategory updateFeeCategory(Long id, String name) {
        FeeCategory category = feeCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fee category not found with ID: " + id));
        category.updateName(name);
        return category;
    }

    public void deleteFeeCategory(Long id) {
        FeeCategory category = feeCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fee category not found with ID: " + id));

        if (feeCatalogRepository.existsByCategoryId(id)) {
            throw new IllegalStateException("Cannot delete fee category containing active catalog fee items");
        }

        feeCategoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<FeeCategory> getAllFeeCategories() {
        return feeCategoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public FeeCategory getFeeCategoryById(Long id) {
        return feeCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fee category not found with ID: " + id));
    }

    // -------------------------------------------------------------------------
    // FeeCatalog Operations
    // -------------------------------------------------------------------------

    public FeeCatalog createFeeCatalog(
            Long categoryId,
            String code,
            String name,
            BigDecimal defaultAmount,
            boolean isPerUnit,
            boolean isChedSanctioned,
            boolean isFheBillable
    ) {
        FeeCategory category = feeCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Fee category not found with ID: " + categoryId));

        if (feeCatalogRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Fee catalog item with code already exists: " + code);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Fee name cannot be blank");
        }
        if (defaultAmount == null || defaultAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Default amount must be non-negative");
        }

        FeeCatalog fee = FeeCatalog.builder()
                .category(category)
                .code(code.trim().toUpperCase())
                .name(name.trim())
                .defaultAmount(defaultAmount)
                .isPerUnit(isPerUnit)
                .isChedSanctioned(isChedSanctioned)
                .isFheBillable(isFheBillable)
                .build();

        return feeCatalogRepository.save(fee);
    }

    public FeeCatalog updateFeeCatalogPricing(Long id, String name, BigDecimal amount, boolean isPerUnit) {
        FeeCatalog fee = feeCatalogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fee catalog item not found with ID: " + id));
        fee.updatePricing(name, amount, isPerUnit);
        return fee;
    }

    public FeeCatalog reclassifyFeeCatalog(Long id, Long newCategoryId) {
        FeeCatalog fee = feeCatalogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fee catalog item not found with ID: " + id));
        FeeCategory newCategory = feeCategoryRepository.findById(newCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("Target fee category not found with ID: " + newCategoryId));
        fee.reclassifyCategory(newCategory);
        return fee;
    }

    public void deleteFeeCatalog(Long id) {
        FeeCatalog fee = feeCatalogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fee catalog item not found with ID: " + id));
        feeCatalogRepository.delete(fee);
    }

    @Transactional(readOnly = true)
    public List<FeeCatalog> getFeeCatalogByCategory(Long categoryId) {
        return feeCatalogRepository.findByCategoryId(categoryId);
    }

    @Transactional(readOnly = true)
    public FeeCatalog getFeeCatalogById(Long id) {
        return feeCatalogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fee catalog item not found with ID: " + id));
    }

    // -------------------------------------------------------------------------
    // PaymentTermTemplate Operations
    // -------------------------------------------------------------------------

    public PaymentTermTemplate createPaymentTermTemplate(
            String name,
            BigDecimal downpaymentPct,
            BigDecimal prelimPct,
            BigDecimal midtermPct,
            BigDecimal semifinalPct,
            BigDecimal finalPct
    ) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Template name cannot be blank");
        }
        if (paymentTermTemplateRepository.existsByName(name)) {
            throw new IllegalArgumentException("Payment term template already exists with name: " + name);
        }

        BigDecimal semi = (semifinalPct != null) ? semifinalPct : BigDecimal.ZERO;
        BigDecimal total = downpaymentPct.add(prelimPct).add(midtermPct).add(semi).add(finalPct);
        if (total.compareTo(new BigDecimal("100.00")) != 0) {
            throw new IllegalArgumentException("Sum of payment schedule percentages must strictly equal 100.00% (Received: " + total + "%)");
        }

        PaymentTermTemplate template = PaymentTermTemplate.builder()
                .name(name.trim())
                .downpaymentPct(downpaymentPct)
                .prelimPct(prelimPct)
                .midtermPct(midtermPct)
                .semifinalPct(semi)
                .finalPct(finalPct)
                .build();

        return paymentTermTemplateRepository.save(template);
    }

    public PaymentTermTemplate updatePaymentTermTemplate(
            Long id,
            String name,
            BigDecimal downpaymentPct,
            BigDecimal prelimPct,
            BigDecimal midtermPct,
            BigDecimal semifinalPct,
            BigDecimal finalPct
    ) {
        PaymentTermTemplate template = paymentTermTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment term template not found with ID: " + id));

        template.updateName(name);
        template.updatePercentages(downpaymentPct, prelimPct, midtermPct, semifinalPct, finalPct);
        return template;
    }

    public void deletePaymentTermTemplate(Long id) {
        PaymentTermTemplate template = paymentTermTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment term template not found with ID: " + id));
        paymentTermTemplateRepository.delete(template);
    }

    @Transactional(readOnly = true)
    public List<PaymentTermTemplate> getAllPaymentTermTemplates() {
        return paymentTermTemplateRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PaymentTermTemplate getPaymentTermTemplateById(Long id) {
        return paymentTermTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment term template not found with ID: " + id));
    }

    // -------------------------------------------------------------------------
    // ScholarshipDiscount Operations
    // -------------------------------------------------------------------------

    public ScholarshipDiscount createScholarshipDiscount(
            String code,
            String name,
            ScholarshipType type,
            ScholarshipCategory category,
            BigDecimal discountPercentage,
            BigDecimal fixedAmount,
            String fundingSource,
            boolean appliesToTuition,
            boolean appliesToMisc
    ) {
        if (scholarshipDiscountRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Scholarship discount already exists with code: " + code);
        }
        if (discountPercentage != null && (discountPercentage.compareTo(BigDecimal.ZERO) < 0 || discountPercentage.compareTo(new BigDecimal("100.00")) > 0)) {
            throw new IllegalArgumentException("Discount percentage must be between 0.00 and 100.00");
        }
        if (fixedAmount != null && fixedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fixed discount amount must be non-negative");
        }

        ScholarshipDiscount discount = ScholarshipDiscount.builder()
                .code(code.trim().toUpperCase())
                .name(name.trim())
                .type(type)
                .category(category != null ? category : ScholarshipCategory.INSTITUTIONAL)
                .discountPercentage(discountPercentage != null ? discountPercentage : BigDecimal.ZERO)
                .fixedAmount(fixedAmount != null ? fixedAmount : BigDecimal.ZERO)
                .fundingSource(fundingSource)
                .appliesToTuition(appliesToTuition)
                .appliesToMisc(appliesToMisc)
                .build();

        return scholarshipDiscountRepository.save(discount);
    }

    public ScholarshipDiscount updateScholarshipDiscount(
            Long id,
            String name,
            ScholarshipCategory category,
            ScholarshipType type,
            BigDecimal discountPercentage,
            BigDecimal fixedAmount,
            String fundingSource,
            boolean appliesToTuition,
            boolean appliesToMisc
    ) {
        ScholarshipDiscount discount = scholarshipDiscountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Scholarship discount not found with ID: " + id));

        discount.updateDetails(name, category, type, fundingSource, appliesToTuition, appliesToMisc);
        discount.updateDiscountRule(discountPercentage, fixedAmount);
        return discount;
    }

    public void deleteScholarshipDiscount(Long id) {
        ScholarshipDiscount discount = scholarshipDiscountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Scholarship discount not found with ID: " + id));
        scholarshipDiscountRepository.delete(discount);
    }

    @Transactional(readOnly = true)
    public List<ScholarshipDiscount> getAllScholarshipDiscounts() {
        return scholarshipDiscountRepository.findAll();
    }

    @Transactional(readOnly = true)
    public ScholarshipDiscount getScholarshipDiscountById(Long id) {
        return scholarshipDiscountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Scholarship discount not found with ID: " + id));
    }
}
