package com.sdt.web_app.controller.authentication;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.authentication.AuthDtos;
import com.sdt.web_app.service.authentication.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Auditable(action = "FORGOT_PASSWORD", entityName = "User")
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        passwordResetService.initiatePasswordReset(request.email(), frontendUrl);
        return ResponseEntity.ok("If the email is registered, a password reset link has been dispatched.");
    }

    @Auditable(action = "RESET_PASSWORD", entityName = "User", entityId = "#result?.userId")
    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetService.PasswordResetResult> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        PasswordResetService.PasswordResetResult result = passwordResetService.completePasswordReset(request.token(), request.newPassword());
        return ResponseEntity.ok(result);
    }
}
