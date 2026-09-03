package com.sdt.web_app.dto.institution;

import com.sdt.web_app.entities.institution.DepartmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DepartmentDtos {

    public record CreateDepartmentRequest(
            @NotNull(message = "Campus ID is required")
            Long campusId,

            @NotBlank(message = "Department code is required")
            @Size(max = 20, message = "Code must not exceed 20 characters")
            String code,

            @NotBlank(message = "Department name is required")
            @Size(max = 100, message = "Name must not exceed 100 characters")
            String name,

            @NotNull(message = "Department type is required")
            DepartmentType type,

            Long parentDepartmentId,

            Long deanUserId
    ) {
    }

    public record UpdateDepartmentRequest(
            @NotBlank(message = "Department name is required")
            @Size(max = 100, message = "Name must not exceed 100 characters")
            String name,

            @NotNull(message = "Department type is required")
            DepartmentType type,

            Long parentDepartmentId,

            Long deanUserId
    ) {
    }

    public record DepartmentResponse(
            Long id,
            Long campusId,
            String campusCode,
            String campusName,
            String code,
            String name,
            DepartmentType type,
            Long parentDepartmentId,
            String parentDepartmentCode,
            Long deanUserId,
            boolean isActive
    ) {
    }
}
