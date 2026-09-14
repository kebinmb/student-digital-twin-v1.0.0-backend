package com.sdt.web_app.dto.admission;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AdmissionDtos {

    public record AdmissionConfigDto(
            Long id,
            Long termId,
            String termName,
            boolean isActive,
            int dailySlotLimit,
            int totalOpenedSlots,
            int daysOpen,
            String startDate,
            String endDate
    ) {}

    public record UpdateAdmissionConfigRequest(
            @NotNull(message = "Term ID is required")
            Long termId,
            boolean isActive,
            @Min(value = 1, message = "Daily slot limit must be at least 1")
            int dailySlotLimit,
            @Min(value = 1, message = "Total opened slots must be at least 1")
            int totalOpenedSlots,
            String startDate,
            String endDate
    ) {}

    public record CreateExamSlotRequest(
            @NotNull(message = "Term ID is required")
            Long termId,

            @NotNull(message = "Exam date is required")
            LocalDate examDate,

            @NotBlank(message = "Start time is required")
            String startTime,

            @NotBlank(message = "End time is required")
            String endTime,

            @NotBlank(message = "Venue room is required")
            String venueRoom,

            @Min(value = 1, message = "Max capacity must be at least 1")
            int maxCapacity
    ) {}

    public record EvaluateExamRequest(
            @NotNull(message = "Exam score is required")
            @DecimalMin(value = "0.00")
            @DecimalMax(value = "100.00")
            BigDecimal examScore,

            String examRemarks,

            @NotBlank(message = "Status decision is required (EXAM_PASSED or EXAM_FAILED)")
            String status
    ) {}

    public record EvaluateInterviewRequest(
            @NotNull(message = "Interview score is required")
            @DecimalMin(value = "0.00")
            @DecimalMax(value = "100.00")
            BigDecimal interviewScore,

            String interviewRemarks,

            @NotBlank(message = "Status decision is required (INTERVIEW_ACCEPTED or REJECTED)")
            String status
    ) {}

    public record EntranceExamSlotResponse(
            Long id,
            Long termId,
            LocalDate examDate,
            String startTime,
            String endTime,
            String venueRoom,
            int maxCapacity,
            int reservedCount,
            int availableSeats,
            String status
    ) {}

    public record PublicProgramDto(
            Long id,
            String code,
            String name
    ) {}

    public record PublicTermDto(
            Long id,
            String academicYearCode,
            String termType,
            boolean isActive
    ) {}

    public record QueueTokenRequest(
            String clientIdentifier
    ) {}

    public record QueueTokenResponse(
            String queueToken,
            String status,
            int queuePosition,
            long estimatedWaitSeconds,
            boolean allowedToProceed,
            String expiresAt,
            Long ttlSeconds
    ) {
        public QueueTokenResponse(
                String queueToken,
                String status,
                int queuePosition,
                long estimatedWaitSeconds,
                boolean allowedToProceed
        ) {
            this(queueToken, status, queuePosition, estimatedWaitSeconds, allowedToProceed, null, null);
        }
    }

    public record EmailAvailabilityResponse(
            boolean available
    ) {}

    public record SubmitAdmissionRequest(
            String queueToken,

            @NotNull(message = "Target program ID is required")
            Long targetProgramId,

            @NotNull(message = "Term ID is required")
            Long termId,

            Long examSlotId,

            @NotBlank(message = "First name is required")
            @Size(max = 50)
            String firstName,

            @Size(max = 50)
            String middleName,

            @NotBlank(message = "Last name is required")
            @Size(max = 50)
            String lastName,

            @Size(max = 10)
            String suffix,

            @NotNull(message = "Birth date is required")
            LocalDate birthDate,

            String birthPlace,

            @NotBlank(message = "Gender is required")
            String gender,

            String genderIdentity,

            @NotBlank(message = "Civil status is required")
            String civilStatus,

            @NotBlank(message = "Citizenship is required")
            String citizenship,

            @NotBlank(message = "Mobile number is required")
            @Size(max = 20)
            String mobileNumber,

            @NotBlank(message = "Email is required")
            @Email(message = "Invalid email address format")
            @Size(max = 100)
            String email,

            @Size(max = 30)
            String lrnNumber,

            @NotBlank(message = "High school name is required")
            @Size(max = 150)
            String highSchoolName,

            String depedSchoolId,

            @NotBlank(message = "High school type is required")
            String highSchoolType,

            @Size(max = 100)
            String shsTrackAndStrand,

            @DecimalMin(value = "60.00", message = "GWA cannot be less than 60")
            @DecimalMax(value = "100.00", message = "GWA cannot exceed 100")
            BigDecimal highSchoolGwa,

            Integer shsYearGraduated,

            @NotBlank(message = "Street address is required")
            @Size(max = 255)
            String streetAddress,

            @NotBlank(message = "Barangay is required")
            @Size(max = 100)
            String barangay,

            @NotBlank(message = "City / Municipality is required")
            @Size(max = 100)
            String cityMunicipality,

            @NotBlank(message = "Province is required")
            @Size(max = 100)
            String province,

            @Size(max = 10)
            String zipCode,

            String permRegion,
            String permProvince,
            String permCityMunicipality,
            String permBarangay,
            String permZipCode,
            String permStreetAddress,

            @NotBlank(message = "Emergency contact name is required")
            @Size(max = 150)
            String emergencyContactName,

            @NotBlank(message = "Emergency contact relationship is required")
            @Size(max = 50)
            String emergencyContactRelationship,

            @NotBlank(message = "Emergency contact number is required")
            @Size(max = 20)
            String emergencyContactNumber,

            @Email
            @Size(max = 100)
            String emergencyContactEmail,

            Boolean is4psBeneficiary,
            String household4psIdNumber,
            Boolean isIndigenousPeople,
            String ipEthnicGroup,
            String ncipCertificateNumber,
            Boolean isPersonWithDisability,
            String disabilityType,
            String pwdIdNumber,
            Boolean isSoloParent,
            Boolean isRaisedBySoloParent,
            String soloParentIdNumber,
            Boolean isOrphan,
            Boolean isGidaResident,
            String gidaBarangayResidence,
            Boolean isFarmerFisherfolk,
            String rsbsaRegistrationNumber,
            Boolean isRebelReturneeFamily,
            String certificateOfSurrenderNumber,
            Boolean isBottom40IncomeBracket,
            String monthlyHouseholdIncomeBracket,
            Boolean isFirstGenerationCollege,
            Boolean isUnderprivilegedHomeless,
            String scholarshipGrantType
    ) {
        public SubmitAdmissionRequest(String queueToken, Long targetProgramId, Long termId, String email) {
            this(queueToken, targetProgramId, termId, null, "Juan", null, "Dela Cruz", null,
                 LocalDate.of(2005, 1, 1), null, "MALE", null, "SINGLE", "FILIPINO", "09171234567",
                 email, null, "High School", null, "PUBLIC", null, BigDecimal.valueOf(90), 2024,
                 "Street", "Barangay", "City", "Province", "1234", null, null, null, null, null, null,
                 "Emergency Contact", "Parent", "09171234567", null, false, null, false, null, null,
                 false, null, null, false, false, null, false, false, null, false, null, false, null,
                 false, "POOR_BELOW_10K", false, false, null);
        }
    }

    public record AdmissionApplicationResponse(
            Long id,
            String applicationNumber,
            Long targetProgramId,
            String targetProgramCode,
            String targetProgramName,
            Long termId,
            String termName,
            Long examSlotId,
            LocalDate examDate,
            String examStartTime,
            String examEndTime,
            String examVenue,
            String firstName,
            String middleName,
            String lastName,
            String suffix,
            String fullName,
            LocalDate birthDate,
            String birthPlace,
            String gender,
            String genderIdentity,
            String civilStatus,
            String citizenship,
            String mobileNumber,
            String email,
            String lrnNumber,
            String highSchoolName,
            String depedSchoolId,
            String highSchoolType,
            String shsTrackAndStrand,
            BigDecimal highSchoolGwa,
            Integer shsYearGraduated,
            String streetAddress,
            String barangay,
            String cityMunicipality,
            String province,
            String zipCode,
            String permRegion,
            String permProvince,
            String permCityMunicipality,
            String permBarangay,
            String permZipCode,
            String permStreetAddress,
            String emergencyContactName,
            String emergencyContactRelationship,
            String emergencyContactNumber,
            String emergencyContactEmail,
            boolean is4psBeneficiary,
            String household4psIdNumber,
            boolean isIndigenousPeople,
            String ipEthnicGroup,
            String ncipCertificateNumber,
            boolean isPersonWithDisability,
            String disabilityType,
            String pwdIdNumber,
            boolean isSoloParent,
            boolean isRaisedBySoloParent,
            String soloParentIdNumber,
            boolean isOrphan,
            boolean isGidaResident,
            String gidaBarangayResidence,
            boolean isFarmerFisherfolk,
            String rsbsaRegistrationNumber,
            boolean isRebelReturneeFamily,
            String certificateOfSurrenderNumber,
            boolean isBottom40IncomeBracket,
            String monthlyHouseholdIncomeBracket,
            boolean isFirstGenerationCollege,
            boolean isUnderprivilegedHomeless,
            String scholarshipGrantType,
            String queueToken,
            String applicationStatus,
            BigDecimal examScore,
            String examRemarks,
            BigDecimal interviewScore,
            String interviewRemarks,
            String evaluatedByName,
            String interviewedByName,
            String createdAt,
            Boolean isEnrolled,
            Long studentId,
            String enrolledAt
    ) {
        public String status() {
            return applicationStatus;
        }
    }

    public record UpdateAdmissionStatusRequest(
            @NotBlank(message = "Status is required")
            String applicationStatus,
            String remarks
    ) {}
}
