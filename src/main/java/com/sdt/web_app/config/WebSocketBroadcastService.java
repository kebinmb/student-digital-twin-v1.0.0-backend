package com.sdt.web_app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Centralized WebSocket broadcast utility.
 * Inject this into any Spring service that needs to push updates to clients.
 * Do NOT inject SimpMessagingTemplate directly into domain services.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast to a topic — all subscribers receive the message.
     * Use for global topics like /topic/terms/active or user-specific topic paths.
     */
    public void broadcast(String topic, Object payload) {
        try {
            messagingTemplate.convertAndSend(topic, payload);
            log.debug("[WebSocket] Broadcast to {}: {}", topic, payload);
        } catch (Exception e) {
            log.error("[WebSocket] Failed to broadcast to {}: {}", topic, e.getMessage());
        }
    }

    /**
     * Send to a specific user — only that user's session receives it.
     * Use for private per-user data via user destinations.
     */
    public void sendToUser(String username, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(username, destination, payload);
            log.debug("[WebSocket] Sent to user {}: {}", username, payload);
        } catch (Exception e) {
            log.error("[WebSocket] Failed to send to user {}: {}", username, e.getMessage());
        }
    }
}
