package com.sdt.web_app.dto.institution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CiloPiloMappingDtos {

    public record CreateCiloPiloMappingRequest(
            @NotNull(message = "Course Outcome ID is required")
            Long courseOutcomeId,

            @NotNull(message = "Program Outcome ID is required")
            Long programOutcomeId,

            @NotBlank(message = "Mapping type is required")
            @Pattern(regexp = "^(I|E|D)$", message = "Mapping type must be 'I' (Introduced), 'E' (Emphasized), or 'D' (Demonstrated)")
            String mappingType
    ) {
    }

    public record UpdateCiloPiloMappingRequest(
            @NotBlank(message = "Mapping type is required")
            @Pattern(regexp = "^(I|E|D)$", message = "Mapping type must be 'I' (Introduced), 'E' (Emphasized), or 'D' (Demonstrated)")
            String mappingType
    ) {
    }

    public record CiloPiloMappingResponse(
            Long id,
            Long courseOutcomeId,
            String courseOutcomeCode,
            Long programOutcomeId,
            String programOutcomeCode,
            String mappingType
    ) {
    }
}
