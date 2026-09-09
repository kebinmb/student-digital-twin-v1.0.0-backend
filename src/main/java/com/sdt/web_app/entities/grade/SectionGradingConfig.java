package com.sdt.web_app.entities.grade;

import com.sdt.web_app.entities.scheduling.ClassSection;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "section_grading_configs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"section", "categories"})
public class SectionGradingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false, unique = true)
    private ClassSection section;

    @Column(name = "midterm_weight", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal midtermWeight = new BigDecimal("50.00");

    @Column(name = "final_weight", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal finalWeight = new BigDecimal("50.00");

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private boolean isLocked = false;

    @OneToMany(mappedBy = "config", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC, id ASC")
    @Builder.Default
    private List<SectionGradingCategory> categories = new ArrayList<>();

    public void updateWeights(BigDecimal midtermWeight, BigDecimal finalWeight) {
        if (midtermWeight == null || finalWeight == null) {
            throw new IllegalArgumentException("Midterm and final weights cannot be null");
        }
        if (midtermWeight.add(finalWeight).compareTo(new BigDecimal("100.00")) != 0) {
            throw new IllegalArgumentException("Midterm and final weights must sum to 100.00%");
        }
        this.midtermWeight = midtermWeight;
        this.finalWeight = finalWeight;
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
    }
}
