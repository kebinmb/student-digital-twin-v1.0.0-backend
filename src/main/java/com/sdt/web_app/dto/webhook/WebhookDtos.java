package com.sdt.web_app.dto.webhook;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public class WebhookDtos {

    public record CreateWebhookRequest(
            @NotBlank String name,
            @NotBlank String targetUrl,
            String secretKey,
            @NotBlank String subscribedEvents
    ) {}

    public record WebhookResponse(
            Long id,
            String name,
            String targetUrl,
            String subscribedEvents,
            boolean active,
            Instant createdAt
    ) {}

    public record WebhookDeliveryResponse(
            Long id,
            Long webhookId,
            String eventType,
            String payloadJson,
            String status,
            int attemptCount,
            int maxAttempts,
            Instant lastAttemptAt,
            Instant nextRetryAt,
            Integer responseHttpCode,
            String responseBody,
            Instant createdAt
    ) {}
}
