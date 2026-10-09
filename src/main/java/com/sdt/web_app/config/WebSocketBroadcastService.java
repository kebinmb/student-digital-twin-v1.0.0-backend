package com.sdt.web_app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
     * Defers transmission until after current transaction commits to prevent phantom/stale updates.
     */
    public void broadcast(String topic, Object payload) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doBroadcast(topic, payload);
                }
            });
        } else {
            doBroadcast(topic, payload);
        }
    }

    private void doBroadcast(String topic, Object payload) {
        try {
            messagingTemplate.convertAndSend(topic, payload);
            log.debug("[WebSocket] Broadcast to {}: {}", topic, payload);
        } catch (Exception e) {
            log.error("[WebSocket] Failed to broadcast to {}: {}", topic, e.getMessage());
        }
    }

    /**
     * Send to a specific user — only that user's session receives it.
     * Defers transmission until after current transaction commits.
     */
    public void sendToUser(String username, String destination, Object payload) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSendToUser(username, destination, payload);
                }
            });
        } else {
            doSendToUser(username, destination, payload);
        }
    }

    private void doSendToUser(String username, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(username, destination, payload);
            log.debug("[WebSocket] Sent to user {}: {}", username, payload);
        } catch (Exception e) {
            log.error("[WebSocket] Failed to send to user {}: {}", username, e.getMessage());
        }
    }
}
