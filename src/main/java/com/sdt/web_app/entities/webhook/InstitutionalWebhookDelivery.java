package com.sdt.web_app.entities.webhook;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "institutional_webhook_deliveries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InstitutionalWebhookDelivery {

    public enum DeliveryStatus {
        PENDING,
        DELIVERED,
        FAILED,
        DEAD_LETTER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_id", nullable = false)
    private InstitutionalWebhook webhook;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private int maxAttempts = 5;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "response_http_code")
    private Integer responseHttpCode;

    @Column(name = "response_body", length = 2000)
    private String responseBody;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
