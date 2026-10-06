package com.sdt.web_app.entities.push;

import com.sdt.web_app.entities.authentication.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "web_push_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebPushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "endpoint", nullable = false, length = 1024)
    private String endpoint;

    @Column(name = "p256dh_key", nullable = false, length = 255)
    private String p256dhKey;

    @Column(name = "auth_key", nullable = false, length = 255)
    private String authKey;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "notify_grades", nullable = false)
    @Builder.Default
    private boolean notifyGrades = true;

    @Column(name = "notify_clearance", nullable = false)
    @Builder.Default
    private boolean notifyClearance = true;

    @Column(name = "notify_honors", nullable = false)
    @Builder.Default
    private boolean notifyHonors = true;

    @Column(name = "notify_attendance", nullable = false)
    @Builder.Default
    private boolean notifyAttendance = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
