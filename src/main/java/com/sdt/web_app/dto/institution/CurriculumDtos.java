package com.sdt.web_app.dto.institution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CurriculumDtos {

    public record CreateCurriculumRequest(
            @NotNull(message = "Program ID is required")
            Long programId,

            Long majorId,

            @NotBlank(message = "Curriculum code is required")
            @Size(max = 30, message = "Curriculum code must not exceed 30 characters")
            String code,

            @NotBlank(message = "Curriculum name is required")
            @Size(max = 150, message = "Curriculum name must not exceed 150 characters")
            String name,

            @NotBlank(message = "Effective academic year is required")
            @Size(max = 20, message = "Effective academic year must not exceed 20 characters")
            String effectiveAcademicYear
    ) {
        public CreateCurriculumRequest(Long programId, String code, String name, String effectiveAcademicYear) {
            this(programId, null, code, name, effectiveAcademicYear);
        }
    }

    public record UpdateCurriculumRequest(
            @NotBlank(message = "Curriculum name is required")
            @Size(max = 150, message = "Curriculum name must not exceed 150 characters")
            String name,

            @NotBlank(message = "Effective academic year is required")
            @Size(max = 20, message = "Effective academic year must not exceed 20 characters")
            String effectiveAcademicYear
    ) {
    }

    public record CurriculumResponse(
            Long id,
            Long programId,
            String programCode,
            String programName,
            Long majorId,
            String majorName,
            String code,
            String name,
            String effectiveAcademicYear,
            String status,
            int versionNumber,
            boolean isActive
    ) {
        public CurriculumResponse(
                Long id,
                Long programId,
                String programCode,
                String programName,
                String code,
                String name,
                String effectiveAcademicYear,
                String status,
                int versionNumber,
                boolean isActive
        ) {
            this(id, programId, programCode, programName, null, null, code, name, effectiveAcademicYear, status, versionNumber, isActive);
        }
    }
}
