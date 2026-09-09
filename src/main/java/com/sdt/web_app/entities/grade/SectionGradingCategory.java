package com.sdt.web_app.entities.grade;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "section_grading_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"config", "items"})
public class SectionGradingCategory {

    public enum TermPeriod { MIDTERM, FINAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "config_id", nullable = false)
    private SectionGradingConfig config;

    @Column(name = "category_name", nullable = false, length = 50)
    private String categoryName;

    @Column(name = "weight_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal weightPercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "term_period", nullable = false, length = 10)
    private TermPeriod termPeriod;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 1;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC, id ASC")
    @Builder.Default
    private List<ClassRecordItem> items = new ArrayList<>();

    public void updateDetails(String categoryName, BigDecimal weightPercentage, TermPeriod termPeriod, int displayOrder) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be blank");
        }
        if (weightPercentage == null || weightPercentage.compareTo(BigDecimal.ZERO) < 0 || weightPercentage.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException("Weight percentage must be between 0.00 and 100.00");
        }
        this.categoryName = categoryName.trim();
        this.weightPercentage = weightPercentage;
        this.termPeriod = termPeriod != null ? termPeriod : TermPeriod.MIDTERM;
        this.displayOrder = displayOrder;
    }
}
