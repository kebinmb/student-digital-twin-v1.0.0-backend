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

        // 1. Person with Disability
        private Boolean isPersonWithDisability;
        private String pwdIdNumber;
        private DisabilityType disabilityType;

        // 2. Solo Parent Status
        private Boolean isSoloParent;
        private Boolean isRaisedBySoloParent;
        private String soloParentIdNumber;

        // 3. 4Ps Beneficiary & UniFAST
        private Boolean is4psBeneficiary;
        private String household4psIdNumber;
        private Boolean isListahananNhts;
        private Boolean unifastTesAwardee;
        private String unifastTesAwardNumber;

        // 4. Indigenous Peoples
        private Boolean isIndigenousPeople;
        private String ipEthnicGroup;
        private String ncipCertificateNumber;

        // 5. Orphan Status
        private Boolean isOrphan;

        // 6. GIDA Resident
        private Boolean isGidaResident;
        private String gidaBarangayResidence;

        // 7. Subsistence Farmer or Fisherfolk Family
        private Boolean isFarmerFisherfolk;
        private String rsbsaRegistrationNumber;

        // 8. Rebel Returnees / E-CLIP
        private Boolean isRebelReturneeFamily;
        private String certificateOfSurrenderNumber;

        // 9. Bottom 40% Household Income Bracket
        private Boolean isBottom40IncomeBracket;
        private HouseholdIncomeBracket monthlyHouseholdIncomeBracket;

        // 10. First Generation College Student
        private Boolean isFirstGenerationCollege;

        // 11. Verification & Audit Metadata
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
        // 1. Person with Disability
        @NotNull
        private Boolean isPersonWithDisability;
        @Size(max = 60)
        private String pwdIdNumber;
        private DisabilityType disabilityType;

        // 2. Solo Parent Status
        @NotNull
        private Boolean isSoloParent;
        @NotNull
        private Boolean isRaisedBySoloParent;
        @Size(max = 60)
        private String soloParentIdNumber;

        // 3. 4Ps Beneficiary & UniFAST
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

        // 4. Indigenous Peoples
        @NotNull
        private Boolean isIndigenousPeople;
        @Size(max = 100)
        private String ipEthnicGroup;
        @Size(max = 100)
        private String ncipCertificateNumber;

        // 5. Orphan Status
        @NotNull
        private Boolean isOrphan;

        // 6. GIDA Resident
        @NotNull
        private Boolean isGidaResident;
        @Size(max = 150)
        private String gidaBarangayResidence;

        // 7. Subsistence Farmer or Fisherfolk Family
        @NotNull
        private Boolean isFarmerFisherfolk;
        @Size(max = 60)
        private String rsbsaRegistrationNumber;

        // 8. Rebel Returnees / E-CLIP
        @NotNull
        private Boolean isRebelReturneeFamily;
        @Size(max = 60)
        private String certificateOfSurrenderNumber;

        // 9. Bottom 40% Household Income Bracket
        @NotNull
        private Boolean isBottom40IncomeBracket;
        @NotNull
        private HouseholdIncomeBracket monthlyHouseholdIncomeBracket;

        // 10. First Generation College Student
        @NotNull
        private Boolean isFirstGenerationCollege;
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
        private long countPersonsWithDisabilities;
        private long countSoloParents;
        private long countRaisedBySoloParents;
        private long count4psBeneficiaries;
        private long countListahananNhts;
        private long countUnifastTesAwardees;
        private long countIndigenousPeoples;
        private long countOrphans;
        private long countGidaResidents;
        private long countFarmerFisherfolk;
        private long countRebelReturneeFamilies;
        private long countBottom40IncomeBracket;
        private long countFirstGenerationCollege;

        private long countSelfDeclared;
        private long countPendingVerification;
        private long countVerified;
        private long countRejected;
    }
}
