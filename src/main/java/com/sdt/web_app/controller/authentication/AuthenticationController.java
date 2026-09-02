package com.sdt.web_app.controller.authentication;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.authentication.AuthDtos;
import com.sdt.web_app.service.authentication.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/auth")
public class AuthenticationController {
    private final AuthService authService;

    public AuthenticationController(AuthService authService) {
        this.authService = authService;
    }

    @Auditable(action = "REGISTER", entityName = "User")
    @PostMapping("register")
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
        setRefreshTokenCookie(response, result.refreshToken(), 7 * 24 * 3600);

        return ResponseEntity.ok(new AuthDtos.AuthResponse(result.accessToken(), "Bearer", result.expiresInSeconds()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDtos.AuthResponse> refresh(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken,
            HttpServletResponse response) {

        AuthDtos.AuthResult result = authService.rotateRefreshToken(refreshToken);
        setRefreshTokenCookie(response, result.refreshToken(), 7 * 24 * 3600);

        return ResponseEntity.ok(new AuthDtos.AuthResponse(result.accessToken(), "Bearer", result.expiresInSeconds()));
    }

    @Auditable(action = "LOGOUT", entityName = "User")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken,
            HttpServletResponse response) {

        authService.logout(refreshToken);
        setRefreshTokenCookie(response, "", 0);

        return ResponseEntity.noContent().build();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String tokenValue, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from("REFRESH_TOKEN", tokenValue)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/public/auth")
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString() + "; Partitioned");
    }
}
