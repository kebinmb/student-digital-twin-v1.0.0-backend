package com.sdt.web_app.entities.institution;

import com.sdt.web_app.entities.authentication.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "certificate_revocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateRevocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "certificate_id", nullable = false, unique = true, length = 120)
    private String certificateId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by_user_id")
    private User revokedBy;

    @Column(name = "revocation_reason", nullable = false, length = 500)
    private String revocationReason;

    @CreationTimestamp
    @Column(name = "revoked_at", nullable = false, updatable = false)
    private Instant revokedAt;

    @Column(name = "reinstated_at")
    private Instant reinstatedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
