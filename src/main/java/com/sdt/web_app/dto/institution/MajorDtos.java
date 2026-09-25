package com.sdt.web_app.dto.institution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class MajorDtos {
    private MajorDtos() {}

    public record CreateMajorRequest(
        @NotNull(message = "Program ID is required")
        Long programId,

        @NotBlank(message = "Major code is required")
        @Size(max = 50, message = "Major code must not exceed 50 characters")
        String code,

        @NotBlank(message = "Major name is required")
        @Size(max = 255, message = "Major name must not exceed 255 characters")
        String name,

        String description
    ) {}

    public record UpdateMajorRequest(
        @NotBlank(message = "Major name is required")
        @Size(max = 255, message = "Major name must not exceed 255 characters")
        String name,

        String description,
        Boolean isActive
    ) {}

    public record MajorSummaryResponse(
        Long id,
        Long programId,
        String programCode,
        String code,
        String name,
        Boolean isActive
    ) {}

    public record MajorDetailResponse(
        Long id,
        Long programId,
        String programCode,
        String code,
        String name,
        String description,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt
    ) {}
}
