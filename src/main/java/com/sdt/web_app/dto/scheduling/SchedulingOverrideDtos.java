package com.sdt.web_app.dto.scheduling;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

public class SchedulingOverrideDtos {

    public record OverrideValidationResponse(
            boolean isMismatch,
            String overrideType, // NONE, CROSS_PROGRAM, CROSS_COLLEGE
            Long facultyUserId,
            String facultyName,
            String facultyEmail,
            Long homeProgramId,
            String homeProgramName,
            Long homeCollegeId,
            String homeCollegeName,
            Long targetProgramId,
            String targetProgramName,
            Long targetCollegeId,
            String targetCollegeName,
            Long assignedChairpersonId,
            String assignedChairpersonName,
            Long assignedDeanId,
            String assignedDeanName,
            boolean hasMissingAuthorities,
            List<String> missingAuthorityMessages,
            List<String> requiredApprovalSteps
    ) {}

    public record CreateOverrideRequest(
            @NotNull(message = "Section ID is required")
            Long sectionId,
            Long scheduleId,
            @NotNull(message = "Faculty User ID is required")
            Long facultyUserId,
            @NotBlank(message = "Justification is required")
            String justification
    ) {}

    public record ApprovalDecisionRequest(
            @NotBlank(message = "Remarks are required")
            String remarks
    ) {}

    public record SchedulingOverrideResponse(
            Long id,
            Long sectionId,
            String sectionCode,
            Long courseId,
            String courseCode,
            String courseTitle,
            Long termId,
            String termName,
            Long scheduleId,
            String scheduleSummary,
            Long facultyUserId,
            String facultyName,
            String facultyEmail,
            Long requestedByUserId,
            String requestedByName,
            String overrideType,
            Long homeProgramId,
            String homeProgramName,
            Long homeCollegeId,
            String homeCollegeName,
            Long targetProgramId,
            String targetProgramName,
            Long targetCollegeId,
            String targetCollegeName,
            String status,
            String justification,
            String resolutionNotes,
            Instant createdAt,
            Instant completedAt,
            List<ApprovalStepResponse> approvals,
            boolean isLocked
    ) {}

    public record ApprovalStepResponse(
            Long id,
            String approvalType,
            int stepOrder,
            String requiredRole,
            Long assignedApproverUserId,
            String assignedApproverName,
            Long decisionByUserId,
            String decisionByName,
            String status,
            Instant decisionAt,
            String remarks
    ) {}

    public record PendingOverrideCountResponse(
            long pendingCount
    ) {}
}
