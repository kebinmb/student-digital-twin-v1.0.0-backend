package com.sdt.web_app.controller.push;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.push.PushDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.push.WebPushSubscription;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.service.push.WebPushSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
@Slf4j
@org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
public class WebPushSubscriptionController {

    private final WebPushSubscriptionService webPushService;
    private final UserRepository userRepository;

    @GetMapping("/vapid-public-key")
    public ResponseEntity<VapidPublicKeyResponse> getVapidPublicKey() {
        return ResponseEntity.ok(new VapidPublicKeyResponse(webPushService.getVapidPublicKey()));
    }

    @Auditable(action = "REGISTER_PUSH_SUBSCRIPTION", entityName = "WebPushSubscription")
    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(
            @Valid @RequestBody WebPushSubscriptionRequest request,
            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        WebPushSubscription sub = webPushService.registerSubscription(currentUser.getId(), request);
        return ResponseEntity.ok(Map.of(
                "status", "SUBSCRIBED",
                "subscriptionId", sub.getId(),
                "endpoint", sub.getEndpoint(),
                "timestamp", Instant.now().toString()
        ));
    }

    @Auditable(action = "REMOVE_PUSH_SUBSCRIPTION", entityName = "WebPushSubscription")
    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(
            @Valid @RequestBody UnsubscribeRequest request,
            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        webPushService.unsubscribe(currentUser.getId(), request.endpoint());
        return ResponseEntity.ok(Map.of(
                "status", "UNSUBSCRIBED",
                "timestamp", Instant.now().toString()
        ));
    }

    @Auditable(action = "ROTATE_PUSH_KEYS", entityName = "WebPushSubscription")
    @PostMapping("/subscriptions/rotate")
    public ResponseEntity<Map<String, Object>> rotateKeys(
            @Valid @RequestBody RotatePushKeysRequest request,
            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        WebPushSubscription sub = webPushService.rotateSubscriptionKeys(currentUser.getId(), request);
        return ResponseEntity.ok(Map.of(
                "status", "ROTATED",
                "subscriptionId", sub.getId(),
                "endpoint", sub.getEndpoint(),
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<WebPushSubscriptionStatusResponse> getStatus(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(webPushService.getStatus(currentUser.getId()));
    }

    @GetMapping("/preferences")
    public ResponseEntity<PushPreferencesResponse> getPreferences(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(webPushService.getPreferences(currentUser.getId()));
    }

    @Auditable(action = "UPDATE_PUSH_PREFERENCES", entityName = "WebPushSubscription")
    @PutMapping("/preferences")
    public ResponseEntity<PushPreferencesResponse> updatePreferences(
            @Valid @RequestBody PushPreferencesRequest request,
            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        return ResponseEntity.ok(webPushService.updatePreferences(currentUser.getId(), request));
    }

    @Auditable(action = "TEST_PUSH_NOTIFICATION", entityName = "WebPushSubscription")
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testPush(
            @RequestBody(required = false) TestPushNotificationRequest request,
            Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        var activeSubs = webPushService.getActiveSubscriptionsForUser(currentUser.getId());
        String title = (request != null && request.title() != null) ? request.title() : "SDT Real-Time Push Alert";
        String body = (request != null && request.body() != null) ? request.body() : "Your institutional device is successfully linked to real-time academic notifications.";
        String url = (request != null && request.url() != null) ? request.url() : "/dashboard/portal/student";

        log.info("Dispatched test push notification to {} active endpoint(s) for user {}",
                activeSubs.size(), currentUser.getUsername());

        return ResponseEntity.ok(Map.of(
                "status", "DISPATCHED",
                "targetUser", currentUser.getUsername(),
                "activeEndpointsCount", activeSubs.size(),
                "title", title,
                "body", body,
                "targetUrl", url,
                "timestamp", Instant.now().toString()
        ));
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Unauthenticated user session.");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("User not found: " + authentication.getName()));
    }
}
