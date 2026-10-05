package com.sdt.web_app.controller.authentication;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.authentication.AuthDtos;
import com.sdt.web_app.service.authentication.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/auth")
public class AuthenticationController {

    private final AuthService authService;

    @Value("${app.security.cookie.name:REFRESH_TOKEN}")
    private String cookieName;

    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${app.security.cookie.path:/}")
    private String cookiePath;

    @Value("${app.security.cookie.domain:}")
    private String cookieDomain;

    @Value("${app.security.cookie.max-age-seconds:604800}")
    private long cookieMaxAge;

    @Value("${app.security.refresh-token.expose-in-body:true}")
    private boolean exposeRefreshTokenInBody;

    public AuthenticationController(AuthService authService) {
        this.authService = authService;
    }

    @Auditable(action = "REGISTER", entityName = "User")
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Auditable(action = "LOGIN", entityName = "User")
    @PostMapping("/login")
    public ResponseEntity<AuthDtos.AuthResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest request,
            HttpServletResponse response) {

        AuthDtos.AuthResult result = authService.authenticate(request);
        setRefreshTokenCookie(response, result.refreshToken(), cookieMaxAge);

        String bodyRefreshToken = exposeRefreshTokenInBody ? result.refreshToken() : null;
        return ResponseEntity.ok(new AuthDtos.AuthResponse(
                result.accessToken(),
                "Bearer",
                result.expiresInSeconds(),
                bodyRefreshToken));
    }

    @Auditable(action = "REFRESH_TOKEN", entityName = "User")
    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.AuthResponse> refresh(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String cookieRefreshToken,
            @RequestHeader(name = "X-Refresh-Token", required = false) String headerRefreshToken,
            @RequestBody(required = false) AuthDtos.RefreshTokenRequest requestBody,
            HttpServletResponse response) {

        String tokenToRotate = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (headerRefreshToken != null && !headerRefreshToken.isBlank()
                    ? headerRefreshToken
                    : (requestBody != null && requestBody.refreshToken() != null && !requestBody.refreshToken().isBlank()
                        ? requestBody.refreshToken()
                        : null));

        AuthDtos.AuthResult result = authService.rotateRefreshToken(tokenToRotate);
        setRefreshTokenCookie(response, result.refreshToken(), cookieMaxAge);

        String bodyRefreshToken = exposeRefreshTokenInBody ? result.refreshToken() : null;
        return ResponseEntity.ok(new AuthDtos.AuthResponse(
                result.accessToken(),
                "Bearer",
                result.expiresInSeconds(),
                bodyRefreshToken));
    }

    @Auditable(action = "LOGOUT", entityName = "User")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String cookieRefreshToken,
            @RequestHeader(name = "X-Refresh-Token", required = false) String headerRefreshToken,
            @RequestBody(required = false) AuthDtos.RefreshTokenRequest requestBody,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            HttpServletResponse response) {

        String tokenToRevoke = (cookieRefreshToken != null && !cookieRefreshToken.isBlank())
                ? cookieRefreshToken
                : (headerRefreshToken != null && !headerRefreshToken.isBlank()
                    ? headerRefreshToken
                    : (requestBody != null && requestBody.refreshToken() != null && !requestBody.refreshToken().isBlank()
                        ? requestBody.refreshToken()
                        : null));

        authService.logout(tokenToRevoke, authHeader);
        setRefreshTokenCookie(response, "", 0L);

        return ResponseEntity.noContent().build();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String tokenValue, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, tokenValue)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(cookiePath != null && !cookiePath.isBlank() ? cookiePath : "/")
                .maxAge(maxAgeSeconds);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }

        ResponseCookie cookie = builder.build();
        String cookieHeader = cookie.toString();
        if (cookieSecure) {
            cookieHeader += "; Partitioned";
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookieHeader);
    }
}