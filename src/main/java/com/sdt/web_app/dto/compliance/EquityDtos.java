package com.sdt.web_app.dto.compliance;

import com.sdt.web_app.entities.compliance.StudentEquityProfile.DisabilityType;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.EquityVerificationStatus;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.HouseholdIncomeBracket;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class EquityDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StudentEquityProfileDto {
        private Long id;
        private Long studentProfileId;
        private String studentNumber;
        private String studentName;
        private String programCode;
        private String programName;

        // Statutory Indicators
        private Boolean is4psBeneficiary;
        private String household4psIdNumber;
        private Boolean isListahananNhts;
        private Boolean unifastTesAwardee;
        private String unifastTesAwardNumber;

        private Boolean isIndigenousPeople;
        private String ipEthnicGroup;
        private String ncipCertificateNumber;

        private Boolean isPersonWithDisability;
        private String pwdIdNumber;
        private DisabilityType disabilityType;

        private Boolean isSoloParentOrDependent;
        private String soloParentIdNumber;

        private Boolean isFirstGenerationCollege;
        private Boolean isGidaResident;
        private HouseholdIncomeBracket monthlyHouseholdIncomeBracket;

        // Governance & Audit
        private EquityVerificationStatus verificationStatus;
        private Long verifiedByUserId;
        private String verifiedByUsername;
        private LocalDateTime verifiedAt;
        private String verificationRemarks;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateStudentEquityProfileRequest {
        @NotNull
        private Boolean is4psBeneficiary;
        @Size(max = 60)
        private String household4psIdNumber;
        @NotNull
        private Boolean isListahananNhts;
        @NotNull
        private Boolean unifastTesAwardee;
        @Size(max = 60)
        private String unifastTesAwardNumber;

        @NotNull
        private Boolean isIndigenousPeople;
        @Size(max = 100)
        private String ipEthnicGroup;
        @Size(max = 100)
        private String ncipCertificateNumber;

        @NotNull
        private Boolean isPersonWithDisability;
        @Size(max = 60)
        private String pwdIdNumber;
        private DisabilityType disabilityType;

        @NotNull
        private Boolean isSoloParentOrDependent;
        @Size(max = 60)
        private String soloParentIdNumber;

        @NotNull
        private Boolean isFirstGenerationCollege;
        @NotNull
        private Boolean isGidaResident;
        @NotNull
        private HouseholdIncomeBracket monthlyHouseholdIncomeBracket;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VerifyEquityProfileRequest {
        @NotNull
        private EquityVerificationStatus verificationStatus;
        @Size(max = 255)
        private String verificationRemarks;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EquityStatisticsSummaryDto {
        private long totalProfilesCount;
        private long count4psBeneficiaries;
        private long countListahananNhts;
        private long countUnifastTesAwardees;
        private long countIndigenousPeoples;
        private long countPersonsWithDisabilities;
        private long countSoloParents;
        private long countFirstGenerationCollege;
        private long countGidaResidents;

        private long countSelfDeclared;
        private long countDocumented;
        private long countVerified;
        private long countRejected;
    }
}
