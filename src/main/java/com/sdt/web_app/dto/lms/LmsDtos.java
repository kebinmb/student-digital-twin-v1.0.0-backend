package com.sdt.web_app.dto.lms;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

public class LmsDtos {

    public record LtiDeploymentRequest(
            @NotBlank String platformName,
            @NotBlank String clientId,
            @NotBlank String deploymentId,
            @NotBlank String oidcAuthUrl,
            @NotBlank String accessTokenUrl,
            @NotBlank String jwksUrl,
            Boolean active
    ) {}

    public record LtiDeploymentResponse(
            Long id,
            String platformName,
            String clientId,
            String deploymentId,
            String oidcAuthUrl,
            String accessTokenUrl,
            String jwksUrl,
            boolean active,
            Instant createdAt
    ) {}

    public record LtiLaunchRequest(
            @NotBlank String clientId,
            @NotBlank String deploymentId,
            @NotBlank String subClaim,
            @NotBlank String idToken
    ) {}

    public record LtiLaunchResponse(
            String redirectUrl,
            String token,
            String username,
            String role
    ) {}

    public record LmsRosterSyncResponse(
            Long sectionId,
            String sectionCode,
            int syncedStudentsCount,
            String status,
            String message
    ) {}

    public record StudentSelfServiceSummaryDto(
            Long studentId,
            String studentNumber,
            String studentName,
            String programCode,
            Integer yearLevel,
            String cumulativeGpa,
            String totalUnitsEarned,
            String financialClearance,
            String departmentalClearance,
            List<EnrolledCourseSummaryDto> currentCourses
    ) {}

    public record EnrolledCourseSummaryDto(
            Long sectionId,
            String sectionCode,
            String courseCode,
            String courseTitle,
            String creditUnits,
            String scheduleText,
            String gradeStatus,
            String currentGrade
    ) {}
}
