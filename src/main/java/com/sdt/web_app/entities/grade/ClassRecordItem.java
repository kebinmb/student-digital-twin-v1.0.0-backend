package com.sdt.web_app.entities.grade;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "class_record_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"category", "scores"})
public class ClassRecordItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private SectionGradingCategory category;

    @Column(name = "item_title", nullable = false, length = 100)
    private String itemTitle;

    @Column(name = "max_points", nullable = false, precision = 6, scale = 2)
    private BigDecimal maxPoints;

    @Column(name = "sequence_order", nullable = false)
    @Builder.Default
    private int sequenceOrder = 1;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StudentAssessmentScore> scores = new ArrayList<>();

    public void updateDetails(String itemTitle, BigDecimal maxPoints, int sequenceOrder) {
        if (itemTitle == null || itemTitle.isBlank()) {
            throw new IllegalArgumentException("Item title cannot be blank");
        }
        if (maxPoints == null || maxPoints.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max points must be greater than zero");
        }
        this.itemTitle = itemTitle.trim();
        this.maxPoints = maxPoints;
        this.sequenceOrder = sequenceOrder;
    }
}
