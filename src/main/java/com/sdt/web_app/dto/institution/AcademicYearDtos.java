package com.sdt.web_app.dto.institution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class AcademicYearDtos {

    public record CreateAcademicYearRequest(
            @NotBlank(message = "Academic year code is required")
            @Size(max = 20, message = "Code must not exceed 20 characters")
            String code,

            @NotNull(message = "Start date is required")
            LocalDate startDate,

            @NotNull(message = "End date is required")
            LocalDate endDate,

            boolean isCurrent
    ) {
    }

    public record UpdateAcademicYearRequest(
            @NotNull(message = "Start date is required")
            LocalDate startDate,

            @NotNull(message = "End date is required")
            LocalDate endDate
    ) {
    }

    public record AcademicYearResponse(
            Long id,
            String code,
            LocalDate startDate,
            LocalDate endDate,
            boolean isCurrent
    ) {
    }
}
