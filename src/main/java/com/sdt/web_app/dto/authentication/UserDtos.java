package com.sdt.web_app.dto.authentication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.Set;

public class UserDtos {

    public record CreateUserRequest(
            @NotBlank(message = "Username is required")
            String username,

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email format")
            String email,

            @NotBlank(message = "Password is required")
            String password,

            @NotEmpty(message = "At least one role must be assigned")
            Set<String> roles,

            Boolean enabled
    ) {}

    public record UpdateUserRequest(
            @Email(message = "Invalid email format")
            String email,

            String password,

            Set<String> roles,

            Boolean enabled
    ) {}

    public record UserDetailResponse(
            Long id,
            String username,
            String email,
            Set<String> roles,
            boolean enabled,
            Instant createdAt
    ) {}
}
