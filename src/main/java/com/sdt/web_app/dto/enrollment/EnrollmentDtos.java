package com.sdt.web_app.dto.enrollment;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class EnrollmentDtos {

    public record AdvisingEligibilityResponse(
            Long studentId,
            String studentNumber,
            String studentName,
            String programCode,
            String programName,
            String curriculumCode,
            int yearLevel,
            String enrollmentStatus,
            boolean isGraduating,
            BigDecimal totalUnitsEarned,
            BigDecimal cumulativeGpa,
            BigDecimal maxAllowedUnits,
            BigDecimal currentEnrolledUnits,
            List<CourseEligibilityItemDto> courses
    ) {}

    public record CourseEligibilityItemDto(
            Long courseId,
            String code,
            String title,
            BigDecimal lectureUnits,
            BigDecimal labUnits,
            BigDecimal creditUnits,
            int yearLevel,
            String semester,
            String eligibilityStatus, // "ELIGIBLE", "LOCKED_PREREQUISITE", "ALREADY_PASSED", "CURRENTLY_ENROLLED"
            String failureReason,
            List<PrerequisiteDetailDto> prerequisites,
            List<AvailableSectionOptionDto> availableSections
    ) {}

    public record PrerequisiteDetailDto(
            Long prerequisiteCourseId,
            String prerequisiteCode,
            String prerequisiteTitle,
            BigDecimal minGradeRequired,
            boolean isSatisfied,
            BigDecimal studentGrade
    ) {}

    public record AvailableSectionOptionDto(
            Long sectionId,
            String sectionCode,
            int maxCapacity,
            int enrolledCount,
            String status,
            String scheduleSummary
    ) {}

    public record EnlistSectionRequest(
            @NotNull(message = "Term ID is required")
            Long termId,

            @NotNull(message = "Section ID is required")
            Long sectionId
    ) {}

    public record RemoveEnlistedSectionRequest(
            @NotNull(message = "Term ID is required")
            Long termId,

            @NotNull(message = "Section ID is required")
            Long sectionId
    ) {}

    public record StudentEnrollmentResponse(
            Long enrollmentId,
            Long studentId,
            String studentNumber,
            Long termId,
            String termName,
            Instant enrollmentDate,
            String status,
            BigDecimal totalCreditUnits,
            boolean isOverloadApproved,
            List<EnrollmentItemResponse> items
    ) {}

    public record EnrollmentItemResponse(
            Long itemId,
            Long sectionId,
            String sectionCode,
            String courseCode,
            String courseTitle,
            BigDecimal creditUnits,
            String scheduleSummary,
            String completionStatus,
            BigDecimal finalNumericalGrade
    ) {}

    public record ConfirmEnrollmentRequest(
            @NotNull(message = "Term ID is required")
            Long termId
    ) {}

    public record EnrollmentConfirmationDto(
            Long enrollmentId,
            String status,
            BigDecimal totalCreditUnits,
            String message
    ) {}

    public record UpdateEnrollmentStatusRequest(
            @NotNull(message = "Status is required")
            String status,
            Boolean isOverloadApproved
    ) {}

    public record StudentSearchResultDto(
            Long id,
            String studentIdNumber,
            String fullName,
            String programCode,
            int yearLevel,
            String academicStatus
    ) {}
}


