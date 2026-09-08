package com.sdt.web_app.dto.faculty;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class FacultyDtos {

    public record FacultyProfileResponse(
            Long id,
            Long userId,
            String username,
            String email,
            String facultyIdNumber,
            String highestDegree,
            String academicRank,
            String prcLicenseNo,
            String employmentStatus,
            boolean isTenured
    ) {}

    public record UpdateFacultyProfileRequest(
            @NotNull(message = "Highest degree is required")
            String highestDegree,

            @NotNull(message = "Academic rank is required")
            String academicRank,

            String prcLicenseNo,

            @NotNull(message = "Employment status is required")
            String employmentStatus,

            boolean isTenured
    ) {}

    public record ChedE5WorkloadSummaryDto(
            Long facultyUserId,
            String facultyIdNumber,
            String facultyName,
            String email,
            String highestDegree,
            String academicRank,
            String prcLicenseNo,
            String employmentStatus,
            boolean isTenured,
            BigDecimal regularUnits,
            BigDecimal overloadUnits,
            BigDecimal totalContactHours,
            int numberOfPreparations,
            List<String> assignedSectionCodes
    ) {}

    public record ChedE5ReportResponse(
            Long termId,
            String termName,
            int totalFacultyCount,
            BigDecimal totalRegularUnits,
            BigDecimal totalOverloadUnits,
            BigDecimal totalContactHours,
            List<ChedE5WorkloadSummaryDto> facultyWorkloads
    ) {}
}
