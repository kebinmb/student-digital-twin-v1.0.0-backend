package com.sdt.web_app.dto.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class ComplianceDtos {

    // Clearance DTOs
    public record ClearanceSignoffDto(
            Long id,
            Long clearanceRequestId,
            String departmentType,
            String signoffStatus,
            String remarks,
            Long signedByUserId,
            String signedByUsername,
            String signedAt
    ) {}

    public record ClearanceRequestDto(
            Long id,
            Long studentProfileId,
            String studentNumber,
            String studentName,
            Long termId,
            String termName,
            String purpose,
            String overallStatus,
            String createdAt,
            List<ClearanceSignoffDto> signoffs
    ) {}

    public record ClearanceStudentSuggestionDto(
            Long studentProfileId,
            String studentNumber,
            String studentName,
            String programCode,
            String clearanceStatus,
            String purpose
    ) {}

    public record InitiateClearanceRequest(
            Long studentProfileId,
            String studentNumber,

            @NotNull(message = "Term ID is required")
            Long termId,

            @NotBlank(message = "Purpose is required")
            String purpose
    ) {
        public InitiateClearanceRequest(Long studentProfileId, Long termId, String purpose) {
            this(studentProfileId, null, termId, purpose);
        }
    }

    public record ProcessSignoffRequest(
            @NotBlank(message = "Sign-off status is required")
            String signoffStatus,

            String remarks
    ) {}

    // Degree Audit & Graduation DTOs
    public record CourseAuditItemDto(
            Long courseId,
            String courseCode,
            String courseTitle,
            BigDecimal creditUnits,
            boolean completed,
            BigDecimal gradeEarned,
            String status
    ) {}

    public record DegreeAuditResultDto(
            Long studentProfileId,
            String studentNumber,
            String studentName,
            String programCode,
            String curriculumCode,
            BigDecimal totalCurriculumUnits,
            BigDecimal totalUnitsEarned,
            BigDecimal cumulativeGpa,
            boolean residencyRequirementMet,
            boolean qualifiedForGraduation,
            String honorsEligible,
            List<CourseAuditItemDto> auditedCourses
    ) {}

    public record ApplyForGraduationRequest(
            @NotNull(message = "Student profile ID is required")
            Long studentProfileId,

            @NotNull(message = "Term ID is required")
            Long termId
    ) {}

    public record GraduationApplicationDto(
            Long id,
            Long studentProfileId,
            String studentNumber,
            String studentName,
            Long curriculumId,
            String curriculumCode,
            Long termId,
            String termName,
            String applicationDate,
            String degreeAuditStatus,
            BigDecimal totalUnitsCompleted,
            BigDecimal cumulativeGpa,
            String honorsStatus,
            String specialOrderNumber,
            String specialOrderIssuedAt
    ) {}

    public record IssueSpecialOrderRequest(
            @NotBlank(message = "Special order number is required")
            String specialOrderNumber
    ) {}

    // CHED Regulatory Reporting DTOs
    public record ChedFormE1InstitutionalDto(
            Long campusId,
            String campusName,
            String chedInstitutionalCode,
            Integer totalPrograms,
            Integer totalEnrolledStudents,
            Integer totalFaculty
    ) {}

    public record ChedFormE3EnrolmentDto(
            Long termId,
            String termName,
            String programCode,
            String programName,
            Integer maleCount,
            Integer femaleCount,
            Integer totalEnrolled,
            BigDecimal totalUnitsTaken
    ) {}

    public record ChedFormE4GraduateDto(
            Long termId,
            String termName,
            String programCode,
            Integer totalGraduates,
            Integer summaCumLaudeCount,
            Integer magnaCumLaudeCount,
            Integer cumLaudeCount
    ) {}

    public record ChedFormE5FacultyDto(
            Long facultyId,
            String facultyName,
            String highestDegree,
            String employmentStatus,
            Integer teachingLoadContactHours,
            Integer assignedSectionsCount
    ) {}
}
