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
                    // subject was not numeric
                }
            }
            String username = jwt.getClaimAsString("preferred_username");
            if (username != null) {
                org.springframework.web.context.request.RequestAttributes attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    Object cachedId = attributes.getAttribute("SDT_CACHED_USER_ID_" + username, org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
                    if (cachedId instanceof Long id) {
                        return id;
                    }
                }
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    Long id = userOpt.get().getId();
                    if (attributes != null) {
                        attributes.setAttribute("SDT_CACHED_USER_ID_" + username, id, org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
                    }
                    return id;
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
            org.springframework.web.context.request.RequestAttributes attributes = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                Object cachedId = attributes.getAttribute("SDT_CACHED_USER_ID_" + name, org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
                if (cachedId instanceof Long id) {
                    return id;
                }
            }
            Long resolvedId = userRepository.findByUsername(name).map(User::getId).orElse(null);
            if (attributes != null && resolvedId != null) {
                attributes.setAttribute("SDT_CACHED_USER_ID_" + name, resolvedId, org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
            }
            return resolvedId;
        }

        return null;
    }
}
