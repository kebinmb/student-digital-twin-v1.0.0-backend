package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "grading_scales")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
public class GradingScale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10, updatable = false)
    private String code;

    @Column(name = "numeric_grade", precision = 3, scale = 2)
    private BigDecimal numericGrade;

    @Column(name = "percentage_min", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageMin;

    @Column(name = "percentage_max", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageMax;

    @Column(name = "transmuted_grade", length = 10)
    private String transmutedGrade;

    @Column(nullable = false, length = 50)
    private String remarks;

    @Column(name = "is_passing", nullable = false)
    @Builder.Default
    private boolean isPassing = true;

    @Column(name = "is_non_numeric", nullable = false)
    @Builder.Default
    private boolean isNonNumeric = false;

    public void updateBracket(BigDecimal min, BigDecimal max, String remarks, boolean isPassing) {
        if (min == null || max == null) {
            throw new IllegalArgumentException("Percentage min and max cannot be null");
        }
        if (min.compareTo(BigDecimal.ZERO) < 0 || max.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage values must be within 0.00% and 100.00%");
        }
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Percentage min cannot be greater than percentage max");
        }
        if (remarks == null || remarks.isBlank()) {
            throw new IllegalArgumentException("Remarks cannot be blank");
        }
        this.percentageMin = min;
        this.percentageMax = max;
        this.remarks = remarks;
        this.isPassing = isPassing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GradingScale that)) return false;
        return code != null && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
