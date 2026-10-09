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
            String financialClearance,
            String departmentalClearance,
            boolean isClearedForEnrollment,
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

    public record CreateStudentRequest(
            @jakarta.validation.constraints.NotBlank(message = "Student number is required")
            String studentNumber,

            @jakarta.validation.constraints.NotBlank(message = "Username is required")
            String username,

            @jakarta.validation.constraints.NotBlank(message = "Email is required")
            @jakarta.validation.constraints.Email(message = "Invalid email format")
            String email,

            String password,

            String firstName,

            String middleName,

            String lastName,

            String suffix,

            @NotNull(message = "Program ID is required")
            Long programId,

            @NotNull(message = "Curriculum ID is required")
            Long curriculumId,

            @NotNull(message = "Student classification is required")
            String classification,

            Integer yearLevel,

            Long admissionApplicationId
    ) {
        public CreateStudentRequest(
                String studentNumber,
                String username,
                String email,
                String password,
                Long programId,
                Long curriculumId,
                String classification,
                Integer yearLevel
        ) {
            this(studentNumber, username, email, password, null, null, null, null, programId, curriculumId, classification, yearLevel, null);
        }

        public CreateStudentRequest(
                String studentNumber,
                String username,
                String email,
                String password,
                Long programId,
                Long curriculumId,
                String classification,
                Integer yearLevel,
                Long admissionApplicationId
        ) {
            this(studentNumber, username, email, password, null, null, null, null, programId, curriculumId, classification, yearLevel, admissionApplicationId);
        }
    }

    public record StudentProfileResponse(
            Long id,
            String studentNumber,
            Long userId,
            String username,
            String email,
            String firstName,
            String middleName,
            String lastName,
            String suffix,
            String fullName,
            Long collegeId,
            String collegeName,
            Long programId,
            String programCode,
            String programName,
            Long curriculumId,
            String curriculumCode,
            String classification,
            int yearLevel,
            String enrollmentStatus,
            boolean isGraduating,
            BigDecimal totalUnitsEarned,
            BigDecimal cumulativeGpa,
            String financialClearance,
            String departmentalClearance
    ) {
        public StudentProfileResponse(
                Long id,
                String studentNumber,
                Long userId,
                String username,
                String email,
                String firstName,
                String middleName,
                String lastName,
                String suffix,
                String fullName,
                Long programId,
                String programCode,
                String programName,
                Long curriculumId,
                String curriculumCode,
                String classification,
                int yearLevel,
                String enrollmentStatus,
                boolean isGraduating,
                BigDecimal totalUnitsEarned,
                BigDecimal cumulativeGpa,
                String financialClearance,
                String departmentalClearance
        ) {
            this(id, studentNumber, userId, username, email, firstName, middleName, lastName, suffix, fullName, null, null, programId, programCode, programName, curriculumId, curriculumCode, classification, yearLevel, enrollmentStatus, isGraduating, totalUnitsEarned, cumulativeGpa, financialClearance, departmentalClearance);
        }

        public StudentProfileResponse(
                Long id,
                String studentNumber,
                Long userId,
                String username,
                String email,
                Long programId,
                String programCode,
                String programName,
                Long curriculumId,
                String curriculumCode,
                String classification,
                int yearLevel,
                String enrollmentStatus,
                boolean isGraduating,
                BigDecimal totalUnitsEarned,
                BigDecimal cumulativeGpa,
                String financialClearance,
                String departmentalClearance
        ) {
            this(id, studentNumber, userId, username, email, null, null, null, null, username, null, null, programId, programCode, programName, curriculumId, curriculumCode, classification, yearLevel, enrollmentStatus, isGraduating, totalUnitsEarned, cumulativeGpa, financialClearance, departmentalClearance);
        }
    }

    public record UpdateClearanceRequest(
            String financialClearance,
            String departmentalClearance
    ) {}

    public record CreditCourseItemRequest(
            @jakarta.validation.constraints.NotBlank(message = "External institution is required")
            String externalInstitution,

            @jakarta.validation.constraints.NotBlank(message = "External course code is required")
            String externalCourseCode,

            @jakarta.validation.constraints.NotBlank(message = "External course title is required")
            String externalCourseTitle,

            @NotNull(message = "Internal course ID is required")
            Long internalCourseId,

            @NotNull(message = "External numerical grade is required")
            @jakarta.validation.constraints.DecimalMin(value = "1.00", message = "External numerical grade must be at least 1.00.")
            @jakarta.validation.constraints.DecimalMax(value = "3.00", message = "CHED CMO 25 Violation: External numerical grade must be 3.00 or better to qualify for crediting.")
            BigDecimal externalNumericalGrade,

            @NotNull(message = "Credits granted is required")
            BigDecimal creditsGranted,

            String remarks
    ) {}

    public record CreditTransfereeCoursesRequest(
            @NotNull(message = "Course crediting items are required")
            @jakarta.validation.constraints.NotEmpty(message = "At least one course item must be specified for crediting")
            List<CreditCourseItemRequest> items
    ) {}

    public record CourseEquivalencyDto(
            Long id,
            Long studentId,
            String externalInstitution,
            String externalCourseCode,
            String externalCourseTitle,
            Long internalCourseId,
            String internalCourseCode,
            String internalCourseTitle,
            BigDecimal externalNumericalGrade,
            BigDecimal creditsGranted,
            String status,
            String approvedByUsername,
            String remarks
    ) {}

    public record TransfereeCreditingSummaryResponse(
            Long studentId,
            String studentNumber,
            int creditedCoursesCount,
            BigDecimal totalUnitsCredited,
            List<CourseEquivalencyDto> creditedCourses
    ) {}
}


