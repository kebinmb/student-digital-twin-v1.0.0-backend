package com.sdt.web_app.service.security;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.repositories.authentication.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Resolves the application User ID from the current Spring Security Authentication.
     * Supports OAuth 2.1 JWT tokens (where subject is userId), custom claims, and username authentication.
     */
    public Long resolveUserId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            if (subject != null) {
                try {
                    return Long.parseLong(subject);
                } catch (NumberFormatException ignored) {
                    // subject was not numeric, fall back to username lookup
                }
            }
            String username = jwt.getClaimAsString("preferred_username");
            if (username != null) {
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    return userOpt.get().getId();
                }
            }
        }

        String name = authentication.getName();
        if (name != null && !name.isBlank()) {
            try {
                return Long.parseLong(name);
            } catch (NumberFormatException ignored) {
                // name was not numeric
            }
            return userRepository.findByUsername(name).map(User::getId).orElse(null);
        }

        return null;
    }
}
