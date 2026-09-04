package com.sdt.web_app.entities.scheduling;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.institution.Term;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "faculty_workloads")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"term", "faculty", "approvedBy"})
public class FacultyWorkload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_user_id", nullable = false)
    private User faculty;

    @Column(name = "regular_units", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal regularUnits = BigDecimal.ZERO;

    @Column(name = "overload_units", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal overloadUnits = BigDecimal.ZERO;

    @Column(name = "total_contact_hours", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal totalContactHours = BigDecimal.ZERO;

    @Column(name = "is_overload_approved", nullable = false)
    @Builder.Default
    private boolean isOverloadApproved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @Column(name = "number_of_preparations", nullable = false)
    @Builder.Default
    private int numberOfPreparations = 0;

    @Column(name = "custom_max_load_units", precision = 4, scale = 2)
    private BigDecimal customMaxLoadUnits;

    @Column(name = "override_reason")
    private String overrideReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "overridden_by_user_id")
    private User overriddenBy;

    public void updateWorkload(BigDecimal regularUnits, BigDecimal overloadUnits, BigDecimal totalContactHours) {
        this.regularUnits = regularUnits;
        this.overloadUnits = overloadUnits;
        this.totalContactHours = totalContactHours;
    }

    public void updatePreparations(int preps) {
        this.numberOfPreparations = Math.max(0, preps);
    }

    public void overrideLoadLimit(BigDecimal customMaxUnits, String reason, User adminUser) {
        this.customMaxLoadUnits = customMaxUnits;
        this.overrideReason = reason;
        this.overriddenBy = adminUser;
    }

    public void clearLoadLimitOverride() {
        this.customMaxLoadUnits = null;
        this.overrideReason = null;
        this.overriddenBy = null;
    }

    public BigDecimal getEffectiveMaxLoad() {
        if (this.customMaxLoadUnits != null) {
            return this.customMaxLoadUnits;
        }
        return this.numberOfPreparations <= 2 ? new BigDecimal("21.00") : new BigDecimal("18.00");
    }

    public void approveOverload(User approver) {
        this.isOverloadApproved = true;
        this.approvedBy = approver;
    }

    public void revokeOverload() {
        this.isOverloadApproved = false;
        this.approvedBy = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FacultyWorkload that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
