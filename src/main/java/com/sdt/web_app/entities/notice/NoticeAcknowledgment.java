package com.sdt.web_app.entities.notice;

import com.sdt.web_app.entities.authentication.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notice_user_acknowledgments", uniqueConstraints = {
    @UniqueConstraint(name = "uq_notice_user_ack", columnNames = {"notice_id", "user_id"})
}, indexes = {
    @Index(name = "idx_ack_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class NoticeAcknowledgment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notice_id", nullable = false)
    private CampusNotice notice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "acknowledged_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant acknowledgedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (acknowledgedAt == null) acknowledgedAt = Instant.now();
    }
}
