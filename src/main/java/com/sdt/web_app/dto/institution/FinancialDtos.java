package com.sdt.web_app.dto.institution;

import com.sdt.web_app.entities.institution.FeeCategoryCode;
import com.sdt.web_app.entities.institution.ScholarshipCategory;
import com.sdt.web_app.entities.institution.ScholarshipType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public final class FinancialDtos {

    // FeeCategory
    public record FeeCategoryResponse(
            Long id,
            FeeCategoryCode code,
            String name
    ) {}

    public record CreateFeeCategoryRequest(
            @NotNull(message = "Fee category code is mandatory")
            FeeCategoryCode code,

            @NotBlank(message = "Fee category name is mandatory")
            String name
    ) {}

    public record UpdateFeeCategoryRequest(
            @NotBlank(message = "Fee category name is mandatory")
            String name
    ) {}

    // FeeCatalog
    public record FeeCatalogResponse(
            Long id,
            Long categoryId,
            String categoryCode,
            String categoryName,
            String code,
            String name,
            BigDecimal defaultAmount,
            boolean isPerUnit,
            boolean isChedSanctioned,
            boolean isFheBillable
    ) {}

    public record CreateFeeCatalogRequest(
            @NotNull(message = "Category ID is mandatory")
            Long categoryId,

            @NotBlank(message = "Fee code is mandatory")
            String code,

            @NotBlank(message = "Fee name is mandatory")
            String name,

            @NotNull(message = "Default amount is mandatory")
            BigDecimal defaultAmount,

            boolean isPerUnit,
            boolean isChedSanctioned,
            boolean isFheBillable
    ) {}

    public record UpdateFeeCatalogRequest(
            @NotBlank(message = "Fee name is mandatory")
            String name,

            @NotNull(message = "Amount is mandatory")
            BigDecimal defaultAmount,

            boolean isPerUnit
    ) {}

    // PaymentTermTemplate
    public record PaymentTermTemplateResponse(
            Long id,
            String name,
            BigDecimal downpaymentPercentage,
            BigDecimal prelimPercentage,
            BigDecimal midtermPercentage,
            BigDecimal semiFinalPercentage,
            BigDecimal finalPercentage
    ) {}

    public record CreatePaymentTermTemplateRequest(
            @NotBlank(message = "Template name is mandatory")
            String name,

            @NotNull(message = "Downpayment percentage is mandatory")
            BigDecimal downpaymentPercentage,

            @NotNull(message = "Prelim percentage is mandatory")
            BigDecimal prelimPercentage,

            @NotNull(message = "Midterm percentage is mandatory")
            BigDecimal midtermPercentage,

            BigDecimal semiFinalPercentage,

            @NotNull(message = "Final percentage is mandatory")
            BigDecimal finalPercentage
    ) {}

    // ScholarshipDiscount
    public record ScholarshipDiscountResponse(
            Long id,
            String code,
            String name,
            ScholarshipType type,
            ScholarshipCategory category,
            BigDecimal discountPercentage,
            BigDecimal fixedAmount,
            String fundingSource,
            boolean appliesToTuition,
            boolean appliesToMisc,
            boolean isActive
    ) {}

    public record CreateScholarshipDiscountRequest(
            @NotBlank(message = "Code is mandatory")
            String code,

            @NotBlank(message = "Name is mandatory")
            String name,

            @NotNull(message = "Scholarship type is mandatory")
            ScholarshipType type,

            ScholarshipCategory category,
            BigDecimal discountPercentage,
            BigDecimal fixedAmount,
            String fundingSource,
            boolean appliesToTuition,
            boolean appliesToMisc
    ) {}

    public record UpdateScholarshipDiscountRequest(
            @NotBlank(message = "Name is mandatory")
            String name,

            ScholarshipCategory category,
            ScholarshipType type,
            BigDecimal discountPercentage,
            BigDecimal fixedAmount,
            String fundingSource,
            boolean appliesToTuition,
            boolean appliesToMisc
    ) {}

    private FinancialDtos() {}
}
