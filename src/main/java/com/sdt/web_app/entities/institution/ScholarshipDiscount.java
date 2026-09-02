package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "scholarship_discounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
public class ScholarshipDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30, updatable = false)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScholarshipType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ScholarshipCategory category = ScholarshipCategory.INSTITUTIONAL;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(name = "fixed_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal fixedAmount = BigDecimal.ZERO;

    @Column(name = "applies_to_tuition", nullable = false)
    @Builder.Default
    private boolean appliesToTuition = true;

    @Column(name = "applies_to_misc", nullable = false)
    @Builder.Default
    private boolean appliesToMisc = true;

    @Column(name = "funding_source", length = 100)
    private String fundingSource;

    public void updateDiscountRule(BigDecimal percentage, BigDecimal fixedAmount) {
        if (percentage != null && (percentage.compareTo(BigDecimal.ZERO) < 0 || percentage.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("Discount percentage must be between 0.00 and 100.00");
        }
        if (fixedAmount != null && fixedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Fixed amount must be non-negative");
        }
        this.discountPercentage = percentage != null ? percentage : BigDecimal.ZERO;
        this.fixedAmount = fixedAmount != null ? fixedAmount : BigDecimal.ZERO;
    }

    public void updateCoverage(boolean appliesToTuition, boolean appliesToMisc, String fundingSource) {
        this.appliesToTuition = appliesToTuition;
        this.appliesToMisc = appliesToMisc;
        this.fundingSource = fundingSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScholarshipDiscount that)) return false;
        return code != null && Objects.equals(code, that.getCode());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}