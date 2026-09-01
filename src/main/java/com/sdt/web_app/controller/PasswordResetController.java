package com.sdt.web_app.controller;

import com.sdt.web_app.dto.authentication.AuthDtos;
import com.sdt.web_app.service.authentication.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
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

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        passwordResetService.initiatePasswordReset(request.email(), frontendUrl);
        return ResponseEntity.ok("If the email is registered, a password reset link has been dispatched.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        try {
            passwordResetService.completePasswordReset(request.token(), request.newPassword());
            return ResponseEntity.ok("Password has been updated successfully.");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
