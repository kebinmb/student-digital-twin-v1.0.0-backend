package com.sdt.web_app.service.push;

import com.sdt.web_app.dto.push.PushDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.push.WebPushSubscription;
import com.sdt.web_app.exceptions.ResourceNotFoundException;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.push.WebPushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebPushSubscriptionService {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final WebPushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final WebPushPayloadEncryptionService encryptionService;
    private final VapidJwtService vapidJwtService;

    @Value("${sdt.push.vapid.public-key:BCyZ6f-12Qp5i4f2F0A3x7rZ9k_tE2wL8vN5mQ9x1Y3_rV0cW4dF8tH2jK6lM4nO8pQ2rS6tU0vW2xY4zA6bC8d}")
    private String vapidPublicKey;

    public String getVapidPublicKey() {
        if (vapidJwtService != null && vapidJwtService.getPublicKeyBase64Url() != null) {
            return vapidJwtService.getPublicKeyBase64Url();
        }
        return vapidPublicKey;
    }

    @Transactional
    public WebPushSubscription registerSubscription(Long userId, WebPushSubscriptionRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required for push subscription.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        return subscriptionRepository.findByUserIdAndEndpoint(userId, request.endpoint())
                .map(sub -> {
                    sub.setP256dhKey(request.keys().p256dh());
                    sub.setAuthKey(request.keys().auth());
                    sub.setUserAgent(request.userAgent());
                    sub.setActive(true);
                    log.info("Refreshed active push subscription #{} for user {}", sub.getId(), userId);
                    return subscriptionRepository.save(sub);
                })
                .orElseGet(() -> {
                    WebPushSubscription newSub = WebPushSubscription.builder()
                            .user(user)
                            .endpoint(request.endpoint())
                            .p256dhKey(request.keys().p256dh())
                            .authKey(request.keys().auth())
                            .userAgent(request.userAgent())
                            .active(true)
                            .build();
                    WebPushSubscription saved = subscriptionRepository.save(newSub);
                    log.info("Registered new push subscription #{} for user {}", saved.getId(), userId);
                    return saved;
                });
    }

    @Transactional
    public void unsubscribe(Long userId, String endpoint) {
        if (userId == null || endpoint == null) return;
        subscriptionRepository.findByUserIdAndEndpoint(userId, endpoint)
                .ifPresent(sub -> {
                    sub.setActive(false);
                    subscriptionRepository.save(sub);
                    log.info("Deactivated push subscription #{} for user {}", sub.getId(), userId);
                });
    }

    @Transactional
    public WebPushSubscription rotateSubscriptionKeys(Long userId, RotatePushKeysRequest request) {
        if (userId == null || request == null) {
            throw new IllegalArgumentException("User ID and rotation request are required.");
        }
        WebPushSubscription sub = subscriptionRepository.findByUserIdAndEndpoint(userId, request.endpoint())
                .orElseThrow(() -> new ResourceNotFoundException("Active push subscription not found for endpoint: " + request.endpoint()));

        sub.setP256dhKey(request.keys().p256dh());
        sub.setAuthKey(request.keys().auth());
        sub.setActive(true);
        WebPushSubscription saved = subscriptionRepository.save(sub);
        log.info("Successfully rotated cryptographic push keys for subscription #{} (user {})", saved.getId(), userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public WebPushSubscriptionStatusResponse getStatus(Long userId) {
        if (userId == null) {
            return new WebPushSubscriptionStatusResponse(false, 0);
        }
        List<WebPushSubscription> active = subscriptionRepository.findByUserIdAndActiveTrue(userId);
        return new WebPushSubscriptionStatusResponse(!active.isEmpty(), active.size());
    }

    @Transactional(readOnly = true)
    public List<WebPushSubscription> getActiveSubscriptionsForUser(Long userId) {
        if (userId == null) return List.of();
        return subscriptionRepository.findByUserIdAndActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public PushPreferencesResponse getPreferences(Long userId) {
        if (userId == null) {
            return new PushPreferencesResponse(true, true, true, true);
        }
        List<WebPushSubscription> active = subscriptionRepository.findByUserIdAndActiveTrue(userId);
        if (active.isEmpty()) {
            return new PushPreferencesResponse(true, true, true, true);
        }
        WebPushSubscription primary = active.get(0);
        return new PushPreferencesResponse(
                primary.isNotifyGrades(),
                primary.isNotifyClearance(),
                primary.isNotifyHonors(),
                primary.isNotifyAttendance()
        );
    }

    @Transactional
    public PushPreferencesResponse updatePreferences(Long userId, PushPreferencesRequest request) {
        if (userId == null || request == null) {
            return new PushPreferencesResponse(true, true, true, true);
        }
        List<WebPushSubscription> active = subscriptionRepository.findByUserIdAndActiveTrue(userId);
        for (WebPushSubscription sub : active) {
            sub.setNotifyGrades(request.notifyGrades());
            sub.setNotifyClearance(request.notifyClearance());
            sub.setNotifyHonors(request.notifyHonors());
            sub.setNotifyAttendance(request.notifyAttendance());
            subscriptionRepository.save(sub);
        }
        return new PushPreferencesResponse(
                request.notifyGrades(),
                request.notifyClearance(),
                request.notifyHonors(),
                request.notifyAttendance()
        );
    }

    @Async
    @Transactional
    public void dispatchPushToUser(Long userId, String category, String title, String body, String url) {
        if (userId == null) return;
        List<WebPushSubscription> subscriptions = subscriptionRepository.findByUserIdAndActiveTrue(userId);
        if (subscriptions.isEmpty()) return;

        Map<String, Object> payloadMap = Map.of(
                "title", title != null ? title : "SDT Real-Time Alert",
                "body", body != null ? body : "Academic record updated.",
                "icon", "/favicon.ico",
                "badge", "/favicon.ico",
                "data", Map.of("url", url != null ? url : "/dashboard/portal/student")
        );

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payloadMap);
        } catch (Exception e) {
            payloadJson = "{\"title\":\"" + title + "\",\"body\":\"" + body + "\"}";
        }

        for (WebPushSubscription sub : subscriptions) {
            if ("GRADES".equalsIgnoreCase(category) && !sub.isNotifyGrades()) continue;
            if ("CLEARANCE".equalsIgnoreCase(category) && !sub.isNotifyClearance()) continue;
            if ("HONORS".equalsIgnoreCase(category) && !sub.isNotifyHonors()) continue;
            if ("ATTENDANCE".equalsIgnoreCase(category) && !sub.isNotifyAttendance()) continue;

            try {
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(sub.getEndpoint()))
                        .timeout(Duration.ofSeconds(5))
                        .header("TTL", "86400")
                        .header("Urgency", "high");

                if (vapidJwtService != null) {
                    String authHeader = vapidJwtService.createAuthorizationHeader(sub.getEndpoint());
                    if (authHeader != null) {
                        reqBuilder.header("Authorization", authHeader);
                    }
                }

                if (encryptionService != null && sub.getP256dhKey() != null && sub.getAuthKey() != null) {
                    try {
                        var encrypted = encryptionService.encrypt(payloadJson, sub.getP256dhKey(), sub.getAuthKey());
                        reqBuilder.header("Content-Type", encrypted.contentType())
                                  .header("Content-Encoding", encrypted.contentEncoding())
                                  .POST(HttpRequest.BodyPublishers.ofByteArray(encrypted.body()));
                    } catch (Exception encErr) {
                        log.debug("Fallback to standard JSON payload for sub #{}: {}", sub.getId(), encErr.getMessage());
                        reqBuilder.header("Content-Type", "application/json")
                                  .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8));
                    }
                } else {
                    reqBuilder.header("Content-Type", "application/json")
                              .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8));
                }

                HttpRequest request = reqBuilder.build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status == 404 || status == 410) {
                    log.info("Push endpoint expired (HTTP {}). Deactivating sub #{}", status, sub.getId());
                    sub.setActive(false);
                    subscriptionRepository.save(sub);
                }
            } catch (Exception e) {
                log.debug("Push dispatch to endpoint #{} skipped/failed: {}", sub.getId(), e.getMessage());
            }
        }
    }
}
