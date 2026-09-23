package com.sdt.web_app.entities.financial;

import com.sdt.web_app.entities.authentication.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "voided_official_receipts")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class VoidedOfficialReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "or_number", nullable = false, unique = true, length = 30)
    private String orNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booklet_id", nullable = false)
    private OrBooklet booklet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voided_by_cashier_id", nullable = false)
    private User voidedByCashier;

    @Column(name = "void_reason", nullable = false, columnDefinition = "TEXT")
    private String voidReason;

    @Column(name = "voided_at", updatable = false)
    @Builder.Default
    private Instant voidedAt = Instant.now();
}
