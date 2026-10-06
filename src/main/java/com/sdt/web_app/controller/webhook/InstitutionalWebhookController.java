package com.sdt.web_app.controller.webhook;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.webhook.WebhookDtos.*;
import com.sdt.web_app.entities.webhook.InstitutionalWebhook;
import com.sdt.web_app.entities.webhook.InstitutionalWebhookDelivery;
import com.sdt.web_app.service.webhook.InstitutionalWebhookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/webhooks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InstitutionalWebhookController {

    private final InstitutionalWebhookService webhookService;

    @Auditable(action = "READ_ALL_WEBHOOKS", entityName = "InstitutionalWebhook")
    @GetMapping
    public ResponseEntity<List<WebhookResponse>> getAllWebhooks() {
        List<WebhookResponse> responses = webhookService.getAllWebhooks().stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @Auditable(action = "CREATE_WEBHOOK", entityName = "InstitutionalWebhook")
    @PostMapping
    public ResponseEntity<WebhookResponse> createWebhook(@Valid @RequestBody CreateWebhookRequest request) {
        InstitutionalWebhook created = webhookService.createWebhook(
                request.name(),
                request.targetUrl(),
                request.secretKey(),
                request.subscribedEvents()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(created));
    }

    @Auditable(action = "TOGGLE_WEBHOOK", entityName = "InstitutionalWebhook", entityId = "#id")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<WebhookResponse> toggleWebhook(@PathVariable Long id, @RequestParam boolean active) {
        InstitutionalWebhook updated = webhookService.toggleWebhook(id, active);
        return ResponseEntity.ok(mapToResponse(updated));
    }

    @Auditable(action = "DELETE_WEBHOOK", entityName = "InstitutionalWebhook", entityId = "#id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWebhook(@PathVariable Long id) {
        webhookService.deleteWebhook(id);
        return ResponseEntity.noContent().build();
    }

    @Auditable(action = "READ_WEBHOOK_DELIVERIES", entityName = "InstitutionalWebhookDelivery", entityId = "#id")
    @GetMapping("/{id}/deliveries")
    public ResponseEntity<List<WebhookDeliveryResponse>> getDeliveries(@PathVariable Long id) {
        List<WebhookDeliveryResponse> deliveries = webhookService.getDeliveriesForWebhook(id).stream()
                .map(this::mapToDeliveryResponse)
                .toList();
        return ResponseEntity.ok(deliveries);
    }

    @Auditable(action = "RETRY_WEBHOOK_DELIVERY", entityName = "InstitutionalWebhookDelivery", entityId = "#deliveryId")
    @PostMapping("/deliveries/{deliveryId}/retry")
    public ResponseEntity<WebhookDeliveryResponse> retryDelivery(@PathVariable Long deliveryId) {
        InstitutionalWebhookDelivery retried = webhookService.redeliver(deliveryId);
        return ResponseEntity.ok(mapToDeliveryResponse(retried));
    }

    @Auditable(action = "TEST_WEBHOOK_PING", entityName = "InstitutionalWebhook", entityId = "#id")
    @PostMapping("/{id}/test")
    public ResponseEntity<WebhookDeliveryResponse> testPingWebhook(@PathVariable Long id) {
        InstitutionalWebhookDelivery delivery = webhookService.testPing(id);
        return ResponseEntity.ok(mapToDeliveryResponse(delivery));
    }

    private WebhookResponse mapToResponse(InstitutionalWebhook wh) {
        return new WebhookResponse(
                wh.getId(),
                wh.getName(),
                wh.getTargetUrl(),
                wh.getSubscribedEvents(),
                wh.isActive(),
                wh.getCreatedAt()
        );
    }

    private WebhookDeliveryResponse mapToDeliveryResponse(InstitutionalWebhookDelivery d) {
        return new WebhookDeliveryResponse(
                d.getId(),
                d.getWebhook() != null ? d.getWebhook().getId() : null,
                d.getEventType(),
                d.getPayloadJson(),
                d.getStatus() != null ? d.getStatus().name() : "PENDING",
                d.getAttemptCount(),
                d.getMaxAttempts(),
                d.getLastAttemptAt(),
                d.getNextRetryAt(),
                d.getResponseHttpCode(),
                d.getResponseBody(),
                d.getCreatedAt()
        );
    }
}
