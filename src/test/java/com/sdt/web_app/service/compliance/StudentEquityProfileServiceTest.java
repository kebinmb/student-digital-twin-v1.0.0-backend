package com.sdt.web_app.service.compliance;

import com.sdt.web_app.dto.compliance.EquityDtos.*;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.compliance.StudentEquityProfile;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.DisabilityType;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.EquityVerificationStatus;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.HouseholdIncomeBracket;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.compliance.StudentEquityProfileRepository;
import com.sdt.web_app.service.security.StudentProfileL2CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentEquityProfileServiceTest {

    @Mock
    private StudentEquityProfileRepository equityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileL2CacheService studentProfileL2CacheService;

    @InjectMocks
    private StudentEquityProfileService equityProfileService;

    private User studentUser;
    private User registrarUser;
    private StudentProfile studentProfile;
    private StudentEquityProfile equityProfile;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(18L)
                .username("student.a")
                .email("student.a@chmsu.edu.ph")
                .roles(Set.of(Roles.STUDENT))
                .build();

        registrarUser = User.builder()
                .id(11L)
                .username("registrar_head")
                .email("registrar@chmsu.edu.ph")
                .roles(Set.of(Roles.REGISTRAR))
                .build();

        studentProfile = StudentProfile.builder()
                .id(10L)
                .studentNumber("2026-0001")
                .user(studentUser)
                .build();

        equityProfile = StudentEquityProfile.builder()
                .id(100L)
                .studentProfile(studentProfile)
                .is4psBeneficiary(true)
                .household4psIdNumber("4PS-2026-001")
                .isIndigenousPeople(false)
                .isPersonWithDisability(true)
                .pwdIdNumber("PWD-12345")
                .disabilityType(DisabilityType.VISUAL)
                .isSoloParent(false)
                .isRaisedBySoloParent(true)
                .soloParentIdNumber("SP-8888")
                .isOrphan(false)
                .isGidaResident(true)
                .gidaBarangayResidence("Barangay Katilingban")
                .isFarmerFisherfolk(true)
                .rsbsaRegistrationNumber("RSBSA-099")
                .isRebelReturneeFamily(false)
                .isBottom40IncomeBracket(true)
                .monthlyHouseholdIncomeBracket(HouseholdIncomeBracket.POOR_BELOW_10K)
                .isFirstGenerationCollege(true)
                .verificationStatus(EquityVerificationStatus.SELF_DECLARED)
                .build();
    }

    @Test
    @DisplayName("Should retrieve existing student equity profile by student profile ID")
    void getEquityProfileByStudentProfileId_Success() {
        when(studentProfileL2CacheService.findByUserId(10L)).thenReturn(studentProfile);
        when(equityRepository.findByStudentProfileId(10L)).thenReturn(Optional.of(equityProfile));

        StudentEquityProfileDto dto = equityProfileService.getEquityProfileByStudentProfileId(10L);

        assertThat(dto).isNotNull();
        assertThat(dto.getIs4psBeneficiary()).isTrue();
        assertThat(dto.getIsPersonWithDisability()).isTrue();
        assertThat(dto.getIsRaisedBySoloParent()).isTrue();
        assertThat(dto.getIsFarmerFisherfolk()).isTrue();
        assertThat(dto.getGidaBarangayResidence()).isEqualTo("Barangay Katilingban");
        assertThat(dto.getVerificationStatus()).isEqualTo(EquityVerificationStatus.SELF_DECLARED);
    }

    @Test
    @DisplayName("Upserting profile with valid certificates transitions status to PENDING_VERIFICATION")
    void upsertEquityProfileForUser_SetsPendingVerification() {
        when(studentProfileL2CacheService.findByUserId(18L)).thenReturn(studentProfile);
        when(equityRepository.findByStudentProfileId(10L)).thenReturn(Optional.of(equityProfile));
        when(equityRepository.save(any(StudentEquityProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateStudentEquityProfileRequest req = UpdateStudentEquityProfileRequest.builder()
                .isPersonWithDisability(true)
                .pwdIdNumber("PWD-999")
                .disabilityType(DisabilityType.HEARING)
                .isSoloParent(true)
                .isRaisedBySoloParent(false)
                .soloParentIdNumber("SP-001")
                .is4psBeneficiary(true)
                .household4psIdNumber("4PS-999")
                .isListahananNhts(true)
                .unifastTesAwardee(true)
                .unifastTesAwardNumber("TES-999")
                .isIndigenousPeople(true)
                .ipEthnicGroup("Ati")
                .ncipCertificateNumber("NCIP-123")
                .isOrphan(false)
                .isGidaResident(true)
                .gidaBarangayResidence("Barangay Mountain")
                .isFarmerFisherfolk(true)
                .rsbsaRegistrationNumber("RSBSA-123")
                .isRebelReturneeFamily(false)
                .certificateOfSurrenderNumber(null)
                .isBottom40IncomeBracket(true)
                .monthlyHouseholdIncomeBracket(HouseholdIncomeBracket.POOR_BELOW_10K)
                .isFirstGenerationCollege(true)
                .build();

        StudentEquityProfileDto result = equityProfileService.upsertEquityProfileForUser(18L, req);

        assertThat(result).isNotNull();
        assertThat(result.getVerificationStatus()).isEqualTo(EquityVerificationStatus.PENDING_VERIFICATION);
        assertThat(result.getIsSoloParent()).isTrue();
        assertThat(result.getIsRaisedBySoloParent()).isFalse();
        assertThat(result.getIpEthnicGroup()).isEqualTo("Ati");
        assertThat(result.getRsbsaRegistrationNumber()).isEqualTo("RSBSA-123");
    }

    @Test
    @DisplayName("Registrar or admin verification transitions status to VERIFIED and populates audit metadata")
    void verifyEquityProfile_PopulatesAuditMetadata() {
        when(equityRepository.findById(100L)).thenReturn(Optional.of(equityProfile));
        when(userRepository.findById(11L)).thenReturn(Optional.of(registrarUser));
        when(equityRepository.save(any(StudentEquityProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerifyEquityProfileRequest req = VerifyEquityProfileRequest.builder()
                .verificationStatus(EquityVerificationStatus.VERIFIED)
                .verificationRemarks("Verified against NCIP, PDAO, and DSWD databases.")
                .build();

        StudentEquityProfileDto result = equityProfileService.verifyEquityProfile(100L, 11L, req);

        assertThat(result).isNotNull();
        assertThat(result.getVerificationStatus()).isEqualTo(EquityVerificationStatus.VERIFIED);
        assertThat(result.getVerifiedByUserId()).isEqualTo(11L);
        assertThat(result.getVerifiedByUsername()).isEqualTo("registrar_head");
        assertThat(result.getVerificationRemarks()).contains("NCIP, PDAO, and DSWD");
    }

    @Test
    @DisplayName("Equity statistics summary returns disaggregated affirmative action indicators")
    void getEquityStatisticsSummary_ReturnsDisaggregatedCounts() {
        when(equityRepository.count()).thenReturn(100L);
        when(equityRepository.countByIsPersonWithDisabilityTrue()).thenReturn(12L);
        when(equityRepository.countByIsSoloParentTrue()).thenReturn(8L);
        when(equityRepository.countByIsRaisedBySoloParentTrue()).thenReturn(15L);
        when(equityRepository.countByIs4psBeneficiaryTrue()).thenReturn(30L);
        when(equityRepository.countByIsListahananNhtsTrue()).thenReturn(25L);
        when(equityRepository.countByUnifastTesAwardeeTrue()).thenReturn(20L);
        when(equityRepository.countByIsIndigenousPeopleTrue()).thenReturn(10L);
        when(equityRepository.countByIsOrphanTrue()).thenReturn(5L);
        when(equityRepository.countByIsGidaResidentTrue()).thenReturn(18L);
        when(equityRepository.countByIsFarmerFisherfolkTrue()).thenReturn(22L);
        when(equityRepository.countByIsRebelReturneeFamilyTrue()).thenReturn(2L);
        when(equityRepository.countByIsBottom40IncomeBracketTrue()).thenReturn(45L);
        when(equityRepository.countByIsFirstGenerationCollegeTrue()).thenReturn(35L);

        when(equityRepository.countByVerificationStatus(EquityVerificationStatus.SELF_DECLARED)).thenReturn(40L);
        when(equityRepository.countByVerificationStatus(EquityVerificationStatus.PENDING_VERIFICATION)).thenReturn(20L);
        when(equityRepository.countByVerificationStatus(EquityVerificationStatus.VERIFIED)).thenReturn(35L);
        when(equityRepository.countByVerificationStatus(EquityVerificationStatus.REJECTED)).thenReturn(5L);

        EquityStatisticsSummaryDto summary = equityProfileService.getEquityStatisticsSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalProfilesCount()).isEqualTo(100L);
        assertThat(summary.getCountPersonsWithDisabilities()).isEqualTo(12L);
        assertThat(summary.getCountSoloParents()).isEqualTo(8L);
        assertThat(summary.getCountRaisedBySoloParents()).isEqualTo(15L);
        assertThat(summary.getCount4psBeneficiaries()).isEqualTo(30L);
        assertThat(summary.getCountOrphans()).isEqualTo(5L);
        assertThat(summary.getCountFarmerFisherfolk()).isEqualTo(22L);
        assertThat(summary.getCountBottom40IncomeBracket()).isEqualTo(45L);
        assertThat(summary.getCountVerified()).isEqualTo(35L);
    }
}
