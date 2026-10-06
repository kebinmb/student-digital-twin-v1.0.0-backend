package com.sdt.web_app.service.webhook;

import tools.jackson.databind.ObjectMapper;
import com.sdt.web_app.entities.webhook.InstitutionalWebhook;
import com.sdt.web_app.entities.webhook.InstitutionalWebhookDelivery;
import com.sdt.web_app.repositories.webhook.InstitutionalWebhookDeliveryRepository;
import com.sdt.web_app.repositories.webhook.InstitutionalWebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstitutionalWebhookService {

    private final InstitutionalWebhookRepository webhookRepository;
    private final InstitutionalWebhookDeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Transactional(readOnly = true)
    public List<InstitutionalWebhook> getAllWebhooks() {
        return webhookRepository.findAll();
    }

    @Transactional
    public InstitutionalWebhook createWebhook(String name, String targetUrl, String secretKey, String subscribedEvents) {
        String secret = (secretKey != null && !secretKey.isBlank())
                ? secretKey
                : "whsec_" + java.util.UUID.randomUUID().toString().replace("-", "");

        InstitutionalWebhook webhook = InstitutionalWebhook.builder()
                .name(name)
                .targetUrl(targetUrl)
                .secretKey(secret)
                .subscribedEvents(subscribedEvents)
                .active(true)
                .build();
        return webhookRepository.save(webhook);
    }

    @Transactional
    public InstitutionalWebhook toggleWebhook(Long id, boolean active) {
        InstitutionalWebhook wh = webhookRepository.findById(id)
                .orElseThrow(() -> new com.sdt.web_app.exceptions.ResourceNotFoundException("Webhook not found: " + id));
        wh.setActive(active);
        return webhookRepository.save(wh);
    }

    @Transactional
    public void deleteWebhook(Long id) {
        webhookRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<InstitutionalWebhookDelivery> getDeliveriesForWebhook(Long webhookId) {
        return deliveryRepository.findByWebhookIdOrderByCreatedAtDesc(webhookId);
    }

    @Transactional
    public InstitutionalWebhookDelivery redeliver(Long deliveryId) {
        InstitutionalWebhookDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new com.sdt.web_app.exceptions.ResourceNotFoundException("Delivery record not found: " + deliveryId));
        executeDeliveryAttempt(delivery);
        return deliveryRepository.save(delivery);
    }

    @Transactional
    public InstitutionalWebhookDelivery testPing(Long webhookId) {
        InstitutionalWebhook wh = webhookRepository.findById(webhookId)
                .orElseThrow(() -> new com.sdt.web_app.exceptions.ResourceNotFoundException("Webhook not found: " + webhookId));

        java.util.Map<String, Object> testPayload = java.util.Map.of(
                "event", "PING_VERIFICATION",
                "webhookId", wh.getId(),
                "webhookName", wh.getName(),
                "targetUrl", wh.getTargetUrl(),
                "timestamp", Instant.now().toString(),
                "message", "Carlos Hilado Memorial State University - SDT Webhook Connectivity Test"
        );

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(testPayload);
        } catch (Exception e) {
            payloadJson = "{\"event\":\"PING_VERIFICATION\"}";
        }

        InstitutionalWebhookDelivery delivery = InstitutionalWebhookDelivery.builder()
                .webhook(wh)
                .eventType("PING_VERIFICATION")
                .payloadJson(payloadJson)
                .status(InstitutionalWebhookDelivery.DeliveryStatus.PENDING)
                .attemptCount(0)
                .maxAttempts(1)
                .nextRetryAt(Instant.now())
                .build();

        InstitutionalWebhookDelivery saved = deliveryRepository.save(delivery);
        executeDeliveryAttempt(saved);
        return deliveryRepository.save(saved);
    }

    @Async
    @Transactional
    public void dispatchEvent(String eventType, Object payload) {
        if (eventType == null || payload == null) return;

        List<InstitutionalWebhook> activeWebhooks = webhookRepository.findByActiveTrue();
        if (activeWebhooks.isEmpty()) return;

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("Failed to serialize webhook payload for event {}", eventType, e);
            return;
        }

        for (InstitutionalWebhook wh : activeWebhooks) {
            if (wh.isSubscribedTo(eventType)) {
                InstitutionalWebhookDelivery delivery = InstitutionalWebhookDelivery.builder()
                        .webhook(wh)
                        .eventType(eventType)
                        .payloadJson(payloadJson)
                        .status(InstitutionalWebhookDelivery.DeliveryStatus.PENDING)
                        .attemptCount(0)
                        .maxAttempts(5)
                        .nextRetryAt(Instant.now())
                        .build();

                InstitutionalWebhookDelivery saved = deliveryRepository.save(delivery);
                executeDeliveryAttempt(saved);
            }
        }
    }

    @Transactional
    public void executeDeliveryAttempt(InstitutionalWebhookDelivery delivery) {
        InstitutionalWebhook wh = delivery.getWebhook();
        if (wh == null || !wh.isActive()) {
            delivery.setStatus(InstitutionalWebhookDelivery.DeliveryStatus.FAILED);
            deliveryRepository.save(delivery);
            return;
        }

        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setLastAttemptAt(Instant.now());

        String signature = computeHmacSha256(delivery.getPayloadJson(), wh.getSecretKey());

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(wh.getTargetUrl()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("X-SDT-Event", delivery.getEventType())
                    .header("X-SDT-Signature", signature)
                    .header("User-Agent", "SDT-Institutional-Webhook/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(delivery.getPayloadJson(), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            delivery.setResponseHttpCode(statusCode);

            String body = response.body();
            if (body != null && body.length() > 1900) {
                body = body.substring(0, 1900) + "...";
            }
            delivery.setResponseBody(body);

            if (statusCode >= 200 && statusCode < 300) {
                delivery.setStatus(InstitutionalWebhookDelivery.DeliveryStatus.DELIVERED);
                delivery.setNextRetryAt(null);
                log.info("Webhook #{} delivered successfully (HTTP {}) for event {}", wh.getId(), statusCode, delivery.getEventType());
            } else {
                handleFailure(delivery);
            }
        } catch (Exception e) {
            log.warn("Webhook delivery attempt #{} failed for {}: {}", delivery.getAttemptCount(), wh.getTargetUrl(), e.getMessage());
            delivery.setResponseBody("Error: " + e.getMessage());
            delivery.setResponseHttpCode(500);
            handleFailure(delivery);
        }

        deliveryRepository.save(delivery);
    }

    private void handleFailure(InstitutionalWebhookDelivery delivery) {
        if (delivery.getAttemptCount() >= delivery.getMaxAttempts()) {
            delivery.setStatus(InstitutionalWebhookDelivery.DeliveryStatus.DEAD_LETTER);
            delivery.setNextRetryAt(null);
            log.error("Webhook #{} marked DEAD_LETTER after {} failed attempts.",
                    delivery.getWebhook().getId(), delivery.getAttemptCount());
        } else {
            delivery.setStatus(InstitutionalWebhookDelivery.DeliveryStatus.FAILED);
            // Exponential backoff: 2^(attempt) * 30 seconds
            long delaySeconds = (long) Math.pow(2, delivery.getAttemptCount()) * 30L;
            delivery.setNextRetryAt(Instant.now().plus(delaySeconds, ChronoUnit.SECONDS));
        }
    }

    @Scheduled(fixedDelay = 60000) // Every 1 minute
    @Transactional
    public void retryFailedDeliveries() {
        List<InstitutionalWebhookDelivery> pendingRetries = deliveryRepository.findByStatusAndNextRetryAtBefore(
                InstitutionalWebhookDelivery.DeliveryStatus.FAILED,
                Instant.now()
        );

        for (InstitutionalWebhookDelivery d : pendingRetries) {
            executeDeliveryAttempt(d);
        }
    }

    public static String computeHmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception e) {
            return "SIGNATURE-ERROR";
        }
    }
}
