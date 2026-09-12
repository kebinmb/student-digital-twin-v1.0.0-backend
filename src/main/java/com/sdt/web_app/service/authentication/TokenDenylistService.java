package com.sdt.web_app.service.authentication;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenDenylistService {

    private final Map<String, Instant> denylist = new ConcurrentHashMap<>();

    public void revokeToken(String tokenIdOrSignature, Instant expiresAt) {
        if (tokenIdOrSignature != null && !tokenIdOrSignature.isBlank() && expiresAt != null) {
            if (expiresAt.isAfter(Instant.now())) {
                denylist.put(tokenIdOrSignature, expiresAt);
            }
        }
        cleanupExpiredTokens();
    }

    public boolean isRevoked(String tokenIdOrSignature) {
        if (tokenIdOrSignature == null || tokenIdOrSignature.isBlank()) {
            return false;
        }
        Instant expiresAt = denylist.get(tokenIdOrSignature);
        if (expiresAt == null) {
            return false;
        }
        if (Instant.now().isAfter(expiresAt)) {
            denylist.remove(tokenIdOrSignature);
            return false;
        }
        return true;
    }

    public void clear() {
        denylist.clear();
    }

    private void cleanupExpiredTokens() {
        Instant now = Instant.now();
        denylist.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
}
