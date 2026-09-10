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

            Boolean enabled,

            Long collegeId,

            Long programId
    ) {
        public CreateUserRequest(String username, String email, String password, Set<String> roles, Boolean enabled) {
            this(username, email, password, roles, enabled, null, null);
        }
    }

    public record UpdateUserRequest(
            @Email(message = "Invalid email format")
            String email,

            String password,

            Set<String> roles,

            Boolean enabled,

            Long collegeId,

            Long programId,

            Boolean clearCollege,

            Boolean clearProgram
    ) {
        public UpdateUserRequest(String email, String password, Set<String> roles, Boolean enabled) {
            this(email, password, roles, enabled, null, null, false, false);
        }

        public UpdateUserRequest(String email, String password, Set<String> roles, Boolean enabled, Long collegeId, Long programId) {
            this(email, password, roles, enabled, collegeId, programId, false, false);
        }
    }

    public record UserDetailResponse(
            Long id,
            String username,
            String email,
            Set<String> roles,
            boolean enabled,
            Instant createdAt,
            Long collegeId,
            String collegeCode,
            String collegeName,
            Long programId,
            String programCode,
            String programName
    ) {
        public UserDetailResponse(Long id, String username, String email, Set<String> roles, boolean enabled, Instant createdAt) {
            this(id, username, email, roles, enabled, createdAt, null, null, null, null, null, null);
        }
    }
}
