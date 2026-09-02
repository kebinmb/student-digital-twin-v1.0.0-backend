package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "payment_term_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
public class PaymentTermTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    @Column(name = "downpayment_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal downpaymentPct;

    @Column(name = "prelim_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal prelimPct;

    @Column(name = "midterm_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal midtermPct;

    @Column(name = "semifinal_pct", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal semifinalPct = BigDecimal.ZERO;

    @Column(name = "final_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal finalPct;

    public void updatePercentages(BigDecimal downpayment, BigDecimal prelim, BigDecimal midterm, BigDecimal semifinal, BigDecimal finalPct) {
        if (downpayment == null || prelim == null || midterm == null || finalPct == null) {
            throw new IllegalArgumentException("Payment percentages cannot be null");
        }
        BigDecimal semi = (semifinal != null) ? semifinal : BigDecimal.ZERO;
        BigDecimal total = downpayment.add(prelim).add(midterm).add(semi).add(finalPct);

        if (total.compareTo(new BigDecimal("100.00")) != 0) {
            throw new IllegalArgumentException("Sum of payment schedule percentages must strictly equal 100.00% (Received: " + total + "%)");
        }

        this.downpaymentPct = downpayment;
        this.prelimPct = prelim;
        this.midtermPct = midterm;
        this.semifinalPct = semi;
        this.finalPct = finalPct;
    }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Payment template name cannot be blank");
        }
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentTermTemplate that)) return false;
        return name != null && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}