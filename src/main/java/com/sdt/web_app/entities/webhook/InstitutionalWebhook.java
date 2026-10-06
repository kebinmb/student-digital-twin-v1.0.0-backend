package com.sdt.web_app.entities.webhook;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "institutional_webhooks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstitutionalWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "target_url", nullable = false, length = 500)
    private String targetUrl;

    @Column(name = "secret_key", nullable = false)
    private String secretKey;

    @Column(name = "subscribed_events", nullable = false, length = 500)
    private String subscribedEvents;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isSubscribedTo(String eventType) {
        if (subscribedEvents == null || eventType == null) return false;
        String[] events = subscribedEvents.split(",");
        for (String ev : events) {
            if (ev.trim().equalsIgnoreCase(eventType.trim()) || ev.trim().equals("*")) {
                return true;
            }
        }
        return false;
    }
}
