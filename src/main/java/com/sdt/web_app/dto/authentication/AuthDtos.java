package com.sdt.web_app.dto.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(
            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
            String username,
            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 100, message = "Password must be at least 8 characters.")
            String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "Username or email is required.")
            String usernameOrEmail,
            @NotBlank(message = "Password is required")
            String password) {
    }

    public record AuthResponse(
            String accessToken,
            String tokenType,
            long expiresInSeconds
    ) {
    }

    public record AuthResult(
            String accessToken,
            String refreshToken,
            long expiresInSeconds
    ) {
    }

    public record ForgotPasswordRequest(
            @NotBlank(message = "Email cannot be blank")
            @Email(message = "Invalid email format")
            String email
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "Token is required")
            String token,
            @NotBlank(message = "Password cannot be blank")
            @Size(min = 8, message = "Password must be atleast 8 characters")
            String newPassword
    ) {
    }
}
