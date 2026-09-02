package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "fee_catalog")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = "category")
public class FeeCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private FeeCategory category;

    @Column(unique = true, nullable = false, length = 30, updatable = false)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "default_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultAmount;

    @Column(name = "is_per_unit", nullable = false)
    @Builder.Default
    private boolean isPerUnit = false;

    @Column(name = "is_ched_sanctioned", nullable = false)
    @Builder.Default
    private boolean isChedSanctioned = true;

    @Column(name = "is_fhe_billable", nullable = false)
    @Builder.Default
    private boolean isFheBillable = true;

    public void updatePricing(String name, BigDecimal amount, boolean isPerUnit) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Fee name cannot be blank");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be non-negative");
        }
        this.name = name;
        this.defaultAmount = amount;
        this.isPerUnit = isPerUnit;
    }

    public void reclassifyCategory(FeeCategory newCategory) {
        if (newCategory == null) {
            throw new IllegalArgumentException("Fee category cannot be null");
        }
        this.category = newCategory;
    }

    public void setFheBillable(boolean billable) {
        this.isFheBillable = billable;
    }

    public void setChedSanctioned(boolean sanctioned) {
        this.isChedSanctioned = sanctioned;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FeeCatalog that)) return false;
        return code != null && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}