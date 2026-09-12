package com.sdt.web_app.entities.financial;

import com.sdt.web_app.entities.institution.AcademicYear;
import com.sdt.web_app.entities.institution.Campus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fee_templates")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class FeeTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id")
    private Campus campus;

    @Column(name = "tuition_per_unit", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal tuitionPerUnit = BigDecimal.ZERO;

    @Column(name = "lab_fee_per_unit", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal labFeePerUnit = BigDecimal.ZERO;

    @Column(name = "miscellaneous_flat_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal miscellaneousFlatFee = BigDecimal.ZERO;

    @Column(name = "athletic_flat_fee", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal athleticFlatFee = BigDecimal.ZERO;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
