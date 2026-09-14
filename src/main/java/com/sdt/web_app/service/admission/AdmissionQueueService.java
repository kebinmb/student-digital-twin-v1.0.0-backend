package com.sdt.web_app.service.admission;

import com.sdt.web_app.dto.admission.AdmissionDtos.QueueTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class AdmissionQueueService {

    private static final int MAX_ACTIVE_TRANSACTIONS = 50;
    private static final long TOKEN_TTL_SECONDS = 600; // 10 minutes active window

    public enum QueueStatus {
        ACTIVE, QUEUED, EXPIRED, CONSUMED
    }

    public static class TokenEntry {
        public final String token;
        public final String clientIdentifier;
        public QueueStatus status;
        public final Instant createdAt;
        public Instant lastAccessedAt;
        public Instant expiresAt;

        public TokenEntry(String token, String clientIdentifier, QueueStatus status) {
            this.token = token;
            this.clientIdentifier = clientIdentifier;
            this.status = status;
            this.createdAt = Instant.now();
            this.lastAccessedAt = Instant.now();
            if (status == QueueStatus.ACTIVE) {
                this.expiresAt = Instant.now().plusSeconds(TOKEN_TTL_SECONDS);
            }
        }
    }

    private final Map<String, TokenEntry> tokenStore = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> waitingQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> activeTokens = ConcurrentHashMap.newKeySet();

    public QueueTokenResponse issueToken(String clientIdentifier) {
        cleanExpiredTokens();
        String token = "ADM-Q-" + UUID.randomUUID().toString().substring(0, 18).toUpperCase();
        String clientId = (clientIdentifier != null && !clientIdentifier.isBlank()) ? clientIdentifier.trim() : "anonymous";

        synchronized (this) {
            if (activeTokens.size() < MAX_ACTIVE_TRANSACTIONS) {
                activeTokens.add(token);
                TokenEntry entry = new TokenEntry(token, clientId, QueueStatus.ACTIVE);
                tokenStore.put(token, entry);
                log.info("Issued ACTIVE queue token {} for client {}", token, clientId);
                return new QueueTokenResponse(token, QueueStatus.ACTIVE.name(), 0, 0, true, entry.expiresAt.toString(), TOKEN_TTL_SECONDS);
            } else {
                waitingQueue.add(token);
                TokenEntry entry = new TokenEntry(token, clientId, QueueStatus.QUEUED);
                tokenStore.put(token, entry);
                int pos = calculateQueuePosition(token);
                long estWait = pos * 15L;
                log.info("Issued QUEUED token {} at position {} for client {}", token, pos, clientId);
                return new QueueTokenResponse(token, QueueStatus.QUEUED.name(), pos, estWait, false, null, null);
            }
        }
    }

    public QueueTokenResponse checkTokenStatus(String token) {
        if (token == null || token.isBlank()) {
            return new QueueTokenResponse(null, QueueStatus.EXPIRED.name(), -1, 0, false, null, 0L);
        }

        TokenEntry entry = tokenStore.get(token);
        if (entry == null) {
            return new QueueTokenResponse(token, QueueStatus.EXPIRED.name(), -1, 0, false, null, 0L);
        }

        synchronized (this) {
            entry.lastAccessedAt = Instant.now();
            
            // Check if queue can be promoted
            promoteQueueIfPossible();

            if (entry.status == QueueStatus.ACTIVE) {
                if (entry.expiresAt != null && entry.expiresAt.isBefore(Instant.now())) {
                    entry.status = QueueStatus.EXPIRED;
                    activeTokens.remove(token);
                    tokenStore.remove(token);
                    promoteQueueIfPossible();
                    return new QueueTokenResponse(token, QueueStatus.EXPIRED.name(), -1, 0, false, null, 0L);
                }
                long remainingTtl = entry.expiresAt != null
                        ? Math.max(0, java.time.Duration.between(Instant.now(), entry.expiresAt).getSeconds())
                        : TOKEN_TTL_SECONDS;
                String expStr = entry.expiresAt != null ? entry.expiresAt.toString() : null;
                return new QueueTokenResponse(token, QueueStatus.ACTIVE.name(), 0, 0, true, expStr, remainingTtl);
            } else if (entry.status == QueueStatus.QUEUED) {
                int pos = calculateQueuePosition(token);
                long estWait = pos * 15L;
                return new QueueTokenResponse(token, QueueStatus.QUEUED.name(), pos, estWait, false, null, null);
            } else {
                return new QueueTokenResponse(token, entry.status.name(), -1, 0, false, null, 0L);
            }
        }
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return true;
        }

        TokenEntry entry = tokenStore.get(token);
        if (entry == null) {
            return false;
        }

        synchronized (this) {
            if (entry.status == QueueStatus.ACTIVE) {
                if (entry.expiresAt != null && entry.expiresAt.isBefore(Instant.now())) {
                    entry.status = QueueStatus.EXPIRED;
                    activeTokens.remove(token);
                    tokenStore.remove(token);
                    promoteQueueIfPossible();
                    return false;
                }
                return true;
            }
            return false;
        }
    }

    public void consumeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        synchronized (this) {
            TokenEntry entry = tokenStore.remove(token);
            if (entry != null) {
                entry.status = QueueStatus.CONSUMED;
                activeTokens.remove(token);
                promoteQueueIfPossible();
                log.info("Successfully consumed queue token {}", token);
            }
        }
    }

    public boolean validateAndConsumeToken(String token) {
        if (token == null || token.isBlank()) {
            // If token is omitted, we allow graceful fallback for testing/direct submissions
            return true;
        }

        synchronized (this) {
            if (validateToken(token)) {
                consumeToken(token);
                return true;
            }
            return false;
        }
    }

    private int calculateQueuePosition(String targetToken) {
        int pos = 1;
        for (String t : waitingQueue) {
            if (t.equals(targetToken)) {
                return pos;
            }
            pos++;
        }
        return -1;
    }

    private void promoteQueueIfPossible() {
        while (activeTokens.size() < MAX_ACTIVE_TRANSACTIONS && !waitingQueue.isEmpty()) {
            String nextToken = waitingQueue.poll();
            if (nextToken != null) {
                TokenEntry entry = tokenStore.get(nextToken);
                if (entry != null && entry.status == QueueStatus.QUEUED) {
                    entry.status = QueueStatus.ACTIVE;
                    entry.expiresAt = Instant.now().plusSeconds(TOKEN_TTL_SECONDS);
                    activeTokens.add(nextToken);
                    log.info("Promoted token {} from QUEUED to ACTIVE", nextToken);
                }
            }
        }
    }

    @Scheduled(fixedRate = 30000)
    public void cleanExpiredTokens() {
        synchronized (this) {
            Instant now = Instant.now();
            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, TokenEntry> e : tokenStore.entrySet()) {
                TokenEntry entry = e.getValue();
                if (entry.status == QueueStatus.ACTIVE && entry.expiresAt != null) {
                    if (entry.expiresAt.isBefore(now)) {
                        toRemove.add(e.getKey());
                    }
                } else if (entry.lastAccessedAt.plusSeconds(TOKEN_TTL_SECONDS).isBefore(now)) {
                    toRemove.add(e.getKey());
                }
            }
            for (String t : toRemove) {
                TokenEntry entry = tokenStore.remove(t);
                if (entry != null) {
                    entry.status = QueueStatus.EXPIRED;
                    activeTokens.remove(t);
                    waitingQueue.remove(t);
                }
            }
            promoteQueueIfPossible();
        }
    }
}
