package com.sdt.web_app.dto.institution;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CampusDtos {

    public record CreateCampusRequest(
            @NotBlank(message = "Campus code is required")
            @Size(max = 20, message = "Code must not exceed 20 characters")
            String code,

            @NotBlank(message = "Campus name is required")
            @Size(max = 100, message = "Name must not exceed 100 characters")
            String name,

            @Size(max = 20, message = "CHED institutional code must not exceed 20 characters")
            String chedInstitutionalCode,

            String address,

            @Size(max = 50, message = "Region must not exceed 50 characters")
            String region,

            @Size(max = 30, message = "Contact number must not exceed 30 characters")
            String contactNumber,

            @Email(message = "Email must be valid")
            @Size(max = 100, message = "Email must not exceed 100 characters")
            String email,

            boolean isMain
    ) {
    }

    public record UpdateCampusRequest(
            @NotBlank(message = "Campus name is required")
            @Size(max = 100, message = "Name must not exceed 100 characters")
            String name,

            @Size(max = 20, message = "CHED institutional code must not exceed 20 characters")
            String chedInstitutionalCode,

            String address,

            @Size(max = 30, message = "Contact number must not exceed 30 characters")
            String contactNumber,

            @Email(message = "Email must be valid")
            @Size(max = 100, message = "Email must not exceed 100 characters")
            String email
    ) {
    }

    public record CampusResponse(
            Long id,
            String code,
            String chedInstitutionalCode,
            String name,
            String address,
            String region,
            String contactNumber,
            String email,
            boolean isMain,
            boolean isActive
    ) {
    }
}
