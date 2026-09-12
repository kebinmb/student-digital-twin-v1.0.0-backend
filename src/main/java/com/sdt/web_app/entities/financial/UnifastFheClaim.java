package com.sdt.web_app.entities.financial;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.institution.Campus;
import com.sdt.web_app.entities.institution.Term;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "unifast_fhe_claims")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UnifastFheClaim {

    public enum ClaimStatus {
        DRAFT, SUBMITTED, APPROVED, DISBURSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_batch_number", nullable = false, unique = true, length = 50)
    private String claimBatchNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;

    @Column(name = "total_beneficiaries", nullable = false)
    @Builder.Default
    private Integer totalBeneficiaries = 0;

    @Column(name = "total_tuition_claimed", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalTuitionClaimed = BigDecimal.ZERO;

    @Column(name = "total_tosf_claimed", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalTosfClaimed = BigDecimal.ZERO;

    @Column(name = "total_claim_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalClaimAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ClaimStatus status = ClaimStatus.DRAFT;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    @OneToMany(mappedBy = "claimBatch", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UnifastFheClaimItem> claimItems = new ArrayList<>();

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
