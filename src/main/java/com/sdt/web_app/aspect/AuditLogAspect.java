package com.sdt.web_app.aspect;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.entities.audit.AuditLog;
import com.sdt.web_app.entities.authentication.PasswordResetToken;
import com.sdt.web_app.entities.authentication.RefreshToken;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.repositories.authentication.PasswordResetTokenRepository;
import com.sdt.web_app.repositories.authentication.RefreshTokenRepository;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.service.audit.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;
import com.sdt.web_app.service.security.SecurityUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.*;

/**
 * Robust AOP Aspect to intercept methods annotated with {@link Auditable}
 * and asynchronously record audit logs in compliance with RA 10173 (Data Privacy Act).
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAM_DISCOVERER = new DefaultParameterNameDiscoverer();

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditable)")
    public Object auditMethodExecution(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long startTime = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        // 1. Capture SecurityContext username and authentication BEFORE execution
        Authentication preAuth = extractAuthentication(joinPoint);
        String preAuthUsername = extractUsername(preAuth);

        // 2. Extract RAW metadata BEFORE execution (before token is invalidated/deleted)
        String rawRefreshToken = extractRawRefreshToken(joinPoint, signature);
        String rawAttemptedIdentifier = extractRawAttemptedIdentifier(joinPoint, signature);
        UserIdentity preResolvedTokenIdentity = preResolveResetTokenUser(joinPoint, signature);

        Object result = null;
        Throwable exception = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            exception = t;
            throw t;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            recordAuditLog(joinPoint, auditable, result, exception, executionTime,
                    preAuth, preAuthUsername, rawRefreshToken, rawAttemptedIdentifier, preResolvedTokenIdentity);
        }
    }

    private void recordAuditLog(ProceedingJoinPoint joinPoint,
                                Auditable auditable,
                                Object result,
                                Throwable exception,
                                long executionTimeMs,
                                Authentication preAuth,
                                String preAuthUsername,
                                String rawRefreshToken,
                                String rawAttemptedIdentifier,
                                UserIdentity preResolvedTokenIdentity) {
        try {
            HttpServletRequest request = getHttpServletRequest();
            String ipAddress = resolveClientIpAddress(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : "N/A";

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();

            // 1. Construct Fully Sanitized Payload Details (Passwords & Tokens Redacted)
            Map<String, Object> detailsMap = new HashMap<>();
            detailsMap.put("method", signature.getDeclaringTypeName() + "." + signature.getName());

            if (auditable.includeArgs()) {
                String[] paramNames = signature.getParameterNames();
                Object[] args = joinPoint.getArgs();
                Map<String, Object> sanitizedArgs = new LinkedHashMap<>();
                if (paramNames != null && args != null) {
                    for (int i = 0; i < paramNames.length && i < args.length; i++) {
                        sanitizedArgs.put(paramNames[i], sanitizeValue(args[i]));
                    }
                }
                detailsMap.put("arguments", sanitizedArgs);
            }

            // 2. Resolve User Identity (Security Context -> Pre-resolved Reset Token -> Execution Result -> Refresh Token -> Attempted DTO)
            UserIdentity identity = resolveUserIdentity(joinPoint, preAuth, preAuthUsername, rawRefreshToken, rawAttemptedIdentifier, preResolvedTokenIdentity, result);

            // 3. Resolve Dynamic Entity ID
            String entityId = resolveEntityId(auditable.entityId(), joinPoint, signature, result);
            if (entityId == null && identity.userId() != null && ("User".equalsIgnoreCase(auditable.entityName()) || "PasswordResetToken".equalsIgnoreCase(auditable.entityName()))) {
                entityId = String.valueOf(identity.userId());
            }
            if (entityId != null) {
                detailsMap.put("entityId", entityId);
            }

            String detailsJson;
            try {
                detailsJson = objectMapper.writeValueAsString(detailsMap);
            } catch (Exception ex) {
                log.warn("Failed to serialize audit log details map", ex);
                detailsJson = "{\"error\":\"Serialization failure\"}";
            }

            String targetEntityName = auditable.entityName().isEmpty()
                    ? signature.getDeclaringType().getSimpleName()
                    : auditable.entityName();

            AuditLog auditLog = AuditLog.builder()
                    .userId(identity.userId())
                    .username(identity.username())
                    .action(auditable.action())
                    .entityName(targetEntityName)
                    .entityId(entityId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .details(detailsJson)
                    .status(exception == null ? "SUCCESS" : "FAILED")
                    .errorMessage(exception != null ? exception.getMessage() : null)
                    .executionTimeMs(executionTimeMs)
                    .build();

            // 4. Asynchronous persistence via proxy-backed service
            auditLogService.saveAuditLogAsync(auditLog);

        } catch (Exception e) {
            log.error("Failed to record audit log for action: {}", auditable.action(), e);
        }
    }

    public record UserIdentity(Long userId, String username) {}

    private Authentication extractAuthentication(ProceedingJoinPoint joinPoint) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth;
        }
        if (joinPoint != null && joinPoint.getArgs() != null) {
            for (Object arg : joinPoint.getArgs()) {
                if (arg instanceof Authentication a && a.isAuthenticated() && !"anonymousUser".equals(a.getPrincipal())) {
                    return a;
                }
            }
        }
        return null;
    }

    private String extractUsername(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        if (auth.getPrincipal() instanceof Jwt jwt) {
            String pref = jwt.getClaimAsString("preferred_username");
            if (pref != null && !pref.isBlank()) {
                return pref;
            }
        }
        return auth.getName();
    }

    private UserIdentity preResolveResetTokenUser(ProceedingJoinPoint joinPoint, MethodSignature signature) {
        String rawToken = extractRawTokenParam(joinPoint, signature);
        if (rawToken != null && !rawToken.isBlank()) {
            try {
                Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(rawToken);
                if (tokenOpt.isPresent()) {
                    User user = tokenOpt.get().getUser();
                    if (user != null) {
                        return new UserIdentity(user.getId(), user.getUsername());
                    }
                }
            } catch (Exception ex) {
                log.debug("Unable to pre-resolve user from reset token: {}", ex.getMessage());
            }
        }
        return null;
    }

    private UserIdentity resolveUserIdentity(ProceedingJoinPoint joinPoint,
                                              Authentication preAuth,
                                              String preAuthUsername,
                                              String rawRefreshToken,
                                              String rawAttemptedIdentifier,
                                              UserIdentity preResolvedTokenIdentity,
                                              Object result) {
        Authentication auth = preAuth != null ? preAuth : extractAuthentication(joinPoint);
        Long userId = null;
        String username = preAuthUsername != null ? preAuthUsername : "ANONYMOUS";

        if (auth != null) {
            try {
                if (securityUtils != null) {
                    Long resolvedId = securityUtils.resolveUserId(auth);
                    if (resolvedId != null) {
                        userId = resolvedId;
                    }
                }
            } catch (Exception ex) {
                log.debug("SecurityUtils failed to resolve user ID: {}", ex.getMessage());
            }

            if (auth.getPrincipal() instanceof Jwt jwt) {
                String pref = jwt.getClaimAsString("preferred_username");
                if (pref != null && !pref.isBlank()) {
                    username = pref;
                }
            } else if (auth.getName() != null && !auth.getName().isBlank() && !"anonymousUser".equals(auth.getName())) {
                username = auth.getName();
            }
        }

        // If we found a valid userId, look up the canonical user
        if (userId != null) {
            try {
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    return new UserIdentity(userId, userOpt.get().getUsername());
                }
            } catch (Exception ex) {
                log.debug("Unable to fetch user by id {}: {}", userId, ex.getMessage());
            }
            if (username != null && !"ANONYMOUS".equals(username)) {
                return new UserIdentity(userId, username);
            }
        }

        // Strategy A: Use Pre-Resolved Reset Token Identity (for RESET_PASSWORD before token deletion)
        if ("ANONYMOUS".equals(username) && preResolvedTokenIdentity != null && preResolvedTokenIdentity.userId() != null) {
            return preResolvedTokenIdentity;
        }

        // Strategy B: Inspect Execution Result (for methods returning User, PasswordResetResult, or ResponseEntity<Result>)
        if ("ANONYMOUS".equals(username) && result != null) {
            UserIdentity resultIdentity = extractIdentityFromResult(result);
            if (resultIdentity != null && resultIdentity.username() != null) {
                username = resultIdentity.username();
                userId = resultIdentity.userId();
                if (userId != null) {
                    return new UserIdentity(userId, username);
                }
            }
        }

        // Strategy C: Unauthenticated LOGOUT / REFRESH via Raw Refresh Token Eager EntityGraph Lookup
        if ("ANONYMOUS".equals(username) && rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            try {
                Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(rawRefreshToken);
                if (tokenOpt.isPresent()) {
                    User user = tokenOpt.get().getUser();
                    if (user != null) {
                        return new UserIdentity(user.getId(), user.getUsername());
                    }
                }
            } catch (Exception ex) {
                log.debug("Unable to resolve user from raw refresh token: {}", ex.getMessage());
            }
        }

        // Strategy D: Fallback for LOGIN / REGISTER / FORGOT_PASSWORD via Raw Attempted Identifier
        if ("ANONYMOUS".equals(username) && rawAttemptedIdentifier != null && !rawAttemptedIdentifier.isBlank()) {
            username = rawAttemptedIdentifier;
        }

        // Strategy E: Database lookup by numeric ID, username, or email
        if (!"ANONYMOUS".equals(username)) {
            try {
                try {
                    Long parsedId = Long.parseLong(username);
                    Optional<User> userOpt = userRepository.findById(parsedId);
                    if (userOpt.isPresent()) {
                        return new UserIdentity(userOpt.get().getId(), userOpt.get().getUsername());
                    }
                } catch (NumberFormatException ignored) {}

                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isEmpty()) {
                    userOpt = userRepository.findByEmail(username);
                }
                if (userOpt.isPresent()) {
                    userId = userOpt.get().getId();
                    username = userOpt.get().getUsername();
                }
            } catch (Exception ex) {
                log.debug("Unable to resolve userId from database for username: {}", username, ex);
            }
        }

        return new UserIdentity(userId, username);
    }

    @SuppressWarnings("unchecked")
    private UserIdentity extractIdentityFromResult(Object result) {
        Object payload = result;
        if (result instanceof ResponseEntity<?> responseEntity) {
            payload = responseEntity.getBody();
        }
        if (payload == null) return null;

        try {
            Map<String, Object> resultMap = objectMapper.convertValue(payload, Map.class);
            if (resultMap != null) {
                Long userId = null;
                String username = null;

                if (resultMap.get("userId") != null) {
                    userId = Long.valueOf(resultMap.get("userId").toString());
                } else if (resultMap.get("id") != null) {
                    userId = Long.valueOf(resultMap.get("id").toString());
                }

                if (resultMap.get("username") != null) {
                    username = String.valueOf(resultMap.get("username"));
                } else if (resultMap.get("email") != null) {
                    username = String.valueOf(resultMap.get("email"));
                }

                if (username != null || userId != null) {
                    return new UserIdentity(userId, username);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractRawTokenParam(ProceedingJoinPoint joinPoint, MethodSignature signature) {
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                String name = paramNames[i].toLowerCase();
                if (("token".equals(name) || name.contains("resettoken")) && args[i] instanceof String val) {
                    return val;
                }
            }
        }
        // Check inside DTO args
        if (args != null) {
            for (Object arg : args) {
                if (arg == null || isNonSerializableWebObject(arg)) continue;
                try {
                    Map<String, Object> map = objectMapper.convertValue(arg, Map.class);
                    if (map != null && map.get("token") != null) {
                        return map.get("token").toString();
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private String extractRawRefreshToken(ProceedingJoinPoint joinPoint, MethodSignature signature) {
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (paramNames == null || args == null) return null;

        for (int i = 0; i < paramNames.length && i < args.length; i++) {
            String name = paramNames[i].toLowerCase();
            if ((name.contains("refreshtoken") || "token".equals(name)) && args[i] instanceof String val) {
                return val;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractRawAttemptedIdentifier(ProceedingJoinPoint joinPoint, MethodSignature signature) {
        Object[] args = joinPoint.getArgs();
        if (args == null) return null;

        for (Object arg : args) {
            if (arg == null || isNonSerializableWebObject(arg)) continue;
            try {
                Map<String, Object> rawMap = objectMapper.convertValue(arg, Map.class);
                if (rawMap != null) {
                    for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                        String key = entry.getKey().toLowerCase();
                        if ((key.contains("username") || key.contains("email") || key.contains("login"))
                                && entry.getValue() != null) {
                            return String.valueOf(entry.getValue());
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String resolveEntityId(String spelExpression, ProceedingJoinPoint joinPoint, MethodSignature signature, Object result) {
        if (spelExpression != null && !spelExpression.isBlank()) {
            try {
                Method method = signature.getMethod();
                Object[] args = joinPoint.getArgs();
                String[] paramNames = PARAM_DISCOVERER.getParameterNames(method);

                EvaluationContext context = new StandardEvaluationContext();
                if (paramNames != null && args != null) {
                    for (int i = 0; i < paramNames.length && i < args.length; i++) {
                        context.setVariable(paramNames[i], args[i]);
                    }
                }
                Object payload = result;
                if (result instanceof ResponseEntity<?> responseEntity) {
                    payload = responseEntity.getBody();
                }
                context.setVariable("result", payload);

                Object val = SPEL_PARSER.parseExpression(spelExpression).getValue(context);
                if (val != null) {
                    return val.toString();
                }
            } catch (Exception e) {
                log.debug("Could not evaluate SpEL expression '{}': {}", spelExpression, e.getMessage());
            }
        }

        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();
        if (paramNames != null && args != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                String name = paramNames[i].toLowerCase();
                if (("id".equals(name) || "entityid".equals(name) || name.endsWith("id")) && args[i] != null) {
                    return args[i].toString();
                }
            }
        }

        // Automatic fallback: inspect result payload for common ID properties
        Object payload = result instanceof ResponseEntity<?> re ? re.getBody() : result;
        if (payload != null && !isNonSerializableWebObject(payload)) {
            try {
                Map<String, Object> map = objectMapper.convertValue(payload, Map.class);
                if (map != null) {
                    if (map.get("sessionId") != null) return String.valueOf(map.get("sessionId"));
                    if (map.get("recordId") != null) return String.valueOf(map.get("recordId"));
                    if (map.get("id") != null) return String.valueOf(map.get("id"));
                    if (map.get("entityId") != null) return String.valueOf(map.get("entityId"));
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private Object sanitizeValue(Object val) {
        if (val == null) return null;

        if (isNonSerializableWebObject(val)) {
            return "[" + val.getClass().getSimpleName() + "]";
        }

        if (val instanceof String || val instanceof Number || val instanceof Boolean || val.getClass().isEnum()) {
            return val;
        }

        try {
            Object converted = objectMapper.convertValue(val, Object.class);
            return sanitizeRecursive(converted);
        } catch (Exception e) {
            return "[" + val.getClass().getSimpleName() + "]";
        }
    }

    private Object sanitizeRecursive(Object obj) {
        if (obj == null) return null;

        if (obj instanceof Map<?, ?> map) {
            Map<String, Object> sanitizedMap = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (isSensitiveKey(key)) {
                    sanitizedMap.put(key, "[REDACTED]");
                } else {
                    sanitizedMap.put(key, sanitizeRecursive(entry.getValue()));
                }
            }
            return sanitizedMap;
        }

        if (obj instanceof List<?> list) {
            List<Object> sanitizedList = new ArrayList<>();
            for (Object item : list) {
                sanitizedList.add(sanitizeRecursive(item));
            }
            return sanitizedList;
        }

        return obj;
    }

    private boolean isSensitiveKey(String key) {
        if (key == null) return false;
        String lower = key.toLowerCase();
        return lower.contains("password") ||
                lower.contains("secret") ||
                lower.contains("rawtoken") ||
                lower.contains("refreshtoken") ||
                lower.contains("creditcard") ||
                lower.contains("cvv") ||
                lower.contains("authorization");
    }

    private boolean isNonSerializableWebObject(Object obj) {
        return obj instanceof HttpServletRequest ||
                obj instanceof HttpServletResponse ||
                obj instanceof HttpSession ||
                obj instanceof BindingResult ||
                obj instanceof MultipartFile ||
                obj instanceof InputStream ||
                obj instanceof OutputStream ||
                obj instanceof Principal ||
                obj instanceof Authentication;
    }

    private HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String resolveClientIpAddress(HttpServletRequest request) {
        if (request == null) return "UNKNOWN";
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}