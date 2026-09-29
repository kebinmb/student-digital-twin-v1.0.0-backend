package com.sdt.web_app.dto.faculty;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class FacultyDtos {

    public record CreateFacultyAccountRequest(
            @jakarta.validation.constraints.NotBlank(message = "Username is required")
            String username,

            @jakarta.validation.constraints.NotBlank(message = "Email is required")
            @jakarta.validation.constraints.Email(message = "Invalid email format")
            String email,

            String password,

            @jakarta.validation.constraints.NotBlank(message = "Faculty ID number is required")
            String facultyIdNumber,

            String firstName,

            String middleName,

            String lastName,

            String suffix,

            @NotNull(message = "Highest degree is required")
            String highestDegree,

            @NotNull(message = "Academic rank is required")
            String academicRank,

            String prcLicenseNo,

            @NotNull(message = "Employment status is required")
            String employmentStatus,

            boolean isTenured,

            Long collegeId,

            Long programId
    ) {
        public CreateFacultyAccountRequest(
                String username,
                String email,
                String password,
                String facultyIdNumber,
                String highestDegree,
                String academicRank,
                String prcLicenseNo,
                String employmentStatus,
                boolean isTenured) {
            this(username, email, password, facultyIdNumber, null, null, null, null, highestDegree, academicRank, prcLicenseNo, employmentStatus, isTenured, null, null);
        }

        public CreateFacultyAccountRequest(
                String username,
                String email,
                String password,
                String facultyIdNumber,
                String highestDegree,
                String academicRank,
                String prcLicenseNo,
                String employmentStatus,
                boolean isTenured,
                Long collegeId,
                Long programId) {
            this(username, email, password, facultyIdNumber, null, null, null, null, highestDegree, academicRank, prcLicenseNo, employmentStatus, isTenured, collegeId, programId);
        }
    }

    public record FacultyProfileResponse(
            Long id,
            Long userId,
            String username,
            String email,
            String facultyIdNumber,
            String firstName,
            String middleName,
            String lastName,
            String suffix,
            String fullName,
            String highestDegree,
            String academicRank,
            String prcLicenseNo,
            String employmentStatus,
            boolean isTenured,
            Long collegeId,
            String collegeCode,
            String collegeName,
            Long programId,
            String programCode,
            String programName
    ) {
        public FacultyProfileResponse(
                Long id,
                Long userId,
                String username,
                String email,
                String facultyIdNumber,
                String highestDegree,
                String academicRank,
                String prcLicenseNo,
                String employmentStatus,
                boolean isTenured) {
            this(id, userId, username, email, facultyIdNumber, null, null, null, null, username, highestDegree, academicRank, prcLicenseNo, employmentStatus, isTenured, null, null, null, null, null, null);
        }

        public FacultyProfileResponse(
                Long id,
                Long userId,
                String username,
                String email,
                String facultyIdNumber,
                String highestDegree,
                String academicRank,
                String prcLicenseNo,
                String employmentStatus,
                boolean isTenured,
                Long collegeId,
                String collegeCode,
                String collegeName,
                Long programId,
                String programCode,
                String programName) {
            this(id, userId, username, email, facultyIdNumber, null, null, null, null, username, highestDegree, academicRank, prcLicenseNo, employmentStatus, isTenured, collegeId, collegeCode, collegeName, programId, programCode, programName);
        }
    }

    public record UpdateFacultyProfileRequest(
            String firstName,

            String middleName,

            String lastName,

            String suffix,

            @NotNull(message = "Highest degree is required")
            String highestDegree,

            @NotNull(message = "Academic rank is required")
            String academicRank,

            String prcLicenseNo,

            @NotNull(message = "Employment status is required")
            String employmentStatus,

            boolean isTenured,

            Long collegeId,

            Long programId
    ) {
        public UpdateFacultyProfileRequest(
                String highestDegree,
                String academicRank,
                String prcLicenseNo,
                String employmentStatus,
                boolean isTenured) {
            this(null, null, null, null, highestDegree, academicRank, prcLicenseNo, employmentStatus, isTenured, null, null);
        }

        public UpdateFacultyProfileRequest(
                String highestDegree,
                String academicRank,
                String prcLicenseNo,
                String employmentStatus,
                boolean isTenured,
                Long collegeId,
                Long programId) {
            this(null, null, null, null, highestDegree, academicRank, prcLicenseNo, employmentStatus, isTenured, collegeId, programId);
        }
    }

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
