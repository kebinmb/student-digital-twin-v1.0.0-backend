package com.sdt.web_app.dto.institution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProgramDtos {

    public record ProgramResponse(
            Long id,
            Long departmentId,
            String departmentCode,
            String code,
            String name,
            String major,
            String degreeLevel,
            int totalUnitsRequired,
            boolean isActive
    ) {
    }

    public record CreateProgramRequest(
            @NotNull(message = "Department ID is mandatory")
            Long departmentId,

            @NotBlank(message = "Program code is mandatory")
            String code,

            @NotBlank(message = "Program name is mandatory")
            String name,

            String major,
            String degreeLevel,
            int totalUnitsRequired
    ) {
    }

    public record UpdateProgramRequest(
            @NotBlank(message = "Program name is mandatory")
            String name,

            String major,
            String cmoReference,
            String governmentPermit,
            int totalUnitsRequired
    ) {
    }

    public record CreateProgramOutcomeRequest(
            @NotBlank(message = "Outcome code is mandatory")
            String code,

            @NotBlank(message = "Description is mandatory")
            String description
    ) {
    }

    public record ProgramOutcomeResponse(
            Long id,
            Long programId,
            String programCode,
            String code,
            String description
    ) {
    }
}
