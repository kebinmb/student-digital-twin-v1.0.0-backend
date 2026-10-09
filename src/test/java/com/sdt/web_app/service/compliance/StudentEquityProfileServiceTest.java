package com.sdt.web_app.service.compliance;

import com.sdt.web_app.dto.compliance.EquityDtos.*;
import com.sdt.web_app.entities.admission.AdmissionApplication;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.compliance.StudentEquityProfile;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.DisabilityType;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.EquityVerificationStatus;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.HouseholdIncomeBracket;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.repositories.admission.AdmissionApplicationRepository;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.compliance.StudentEquityProfileRepository;
import com.sdt.web_app.service.analytics.EquityTargetService;
import com.sdt.web_app.service.security.StudentProfileL2CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    @Mock
    private AdmissionApplicationRepository admissionApplicationRepository;

    @Mock
    private EquityTargetService equityTargetService;

    @Mock
    private com.sdt.web_app.config.WebSocketBroadcastService broadcastService;

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
    @DisplayName("Should return default in-memory equity profile when none exists, without executing repository save")
    void getEquityProfileByStudentProfileId_ReturnsDefaultWithoutSaving() {
        when(studentProfileL2CacheService.findByUserId(10L)).thenReturn(studentProfile);
        when(equityRepository.findByStudentProfileId(10L)).thenReturn(Optional.empty());

        StudentEquityProfileDto dto = equityProfileService.getEquityProfileByStudentProfileId(10L);

        assertThat(dto).isNotNull();
        assertThat(dto.getStudentProfileId()).isEqualTo(10L);
        assertThat(dto.getStudentNumber()).isEqualTo("2026-0001");
        assertThat(dto.getVerificationStatus()).isEqualTo(EquityVerificationStatus.SELF_DECLARED);
        assertThat(dto.getIsPersonWithDisability()).isFalse();
        assertThat(dto.getIsSoloParent()).isFalse();
        assertThat(dto.getIs4psBeneficiary()).isFalse();
        assertThat(dto.getMonthlyHouseholdIncomeBracket()).isEqualTo(HouseholdIncomeBracket.POOR_BELOW_10K);

        // Crucial guard: verify that NO database save was called during read operation
        verify(equityRepository, never()).save(any(StudentEquityProfile.class));
    }

    @Test
    @DisplayName("Should return default profile for user ID without saving to database")
    void getEquityProfileForUser_ReturnsDefaultWithoutSaving() {
        when(studentProfileL2CacheService.findByUserId(18L)).thenReturn(studentProfile);
        when(studentProfileL2CacheService.findByUserId(10L)).thenReturn(studentProfile);
        when(equityRepository.findByStudentProfileId(10L)).thenReturn(Optional.empty());

        StudentEquityProfileDto dto = equityProfileService.getEquityProfileForUser(18L);

        assertThat(dto).isNotNull();
        assertThat(dto.getStudentProfileId()).isEqualTo(10L);
        assertThat(dto.getVerificationStatus()).isEqualTo(EquityVerificationStatus.SELF_DECLARED);
        verify(equityRepository, never()).save(any(StudentEquityProfile.class));
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

    @Test
    @DisplayName("Should handle dangling or unresolvable verifiedBy proxy gracefully without throwing EntityNotFoundException")
    void mapToDto_HandlesDanglingVerifiedByProxyGracefully() {
        User danglingProxy = mock(User.class);
        when(danglingProxy.getId()).thenThrow(new jakarta.persistence.EntityNotFoundException("No row with the given identifier exists for entity User with id '11'"));

        equityProfile.setVerifiedBy(danglingProxy);
        when(studentProfileL2CacheService.findByUserId(10L)).thenReturn(studentProfile);
        when(equityRepository.findByStudentProfileId(10L)).thenReturn(Optional.of(equityProfile));

        StudentEquityProfileDto dto = equityProfileService.getEquityProfileByStudentProfileId(10L);

        assertThat(dto).isNotNull();
        assertThat(dto.getVerifiedByUserId()).isNull();
        assertThat(dto.getVerifiedByUsername()).isNull();
    }

    @Test
    @DisplayName("Should search post-exam admission applicants for statutory equity audit")
    void searchPostExamApplicantsForEquityAudit_Success() {
        Program program = Program.builder().id(1L).code("BSCS").name("Bachelor of Science in Computer Science").build();
        Term term = Term.builder().id(10L).build();

        AdmissionApplication app = AdmissionApplication.builder()
                .id(50L)
                .applicationNumber("ADM-2026-59384")
                .firstName("Maria")
                .lastName("Santos")
                .email("maria.santos@gmail.com")
                .targetProgram(program)
                .term(term)
                .applicationStatus(AdmissionApplication.ApplicationStatus.EXAM_PASSED)
                .examScore(BigDecimal.valueOf(88.50))
                .examRemarks("Passed with honors")
                .is4psBeneficiary(true)
                .household4psIdNumber("4PS-ILOILO-9912")
                .isPersonWithDisability(true)
                .pwdIdNumber("PWD-8812")
                .disabilityType("VISUAL")
                .isBottom40IncomeBracket(true)
                .monthlyHouseholdIncomeBracket("POOR_BELOW_10K")
                .build();

        PageRequest pageable = PageRequest.of(0, 10);
        when(admissionApplicationRepository.searchPostExamApplicationsForEquityAudit(
                eq("ADM-2026-59384"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(app), pageable, 1));
        when(equityTargetService.calculateApplicantSocioeconomicRiskScore(app)).thenReturn(BigDecimal.valueOf(80.00));

        Page<ApplicantEquityAuditDto> result = equityProfileService.searchPostExamApplicantsForEquityAudit(
                "ADM-2026-59384", null, null, null, null, null, null, null, null, null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        ApplicantEquityAuditDto dto = result.getContent().get(0);
        assertThat(dto.getApplicationNumber()).isEqualTo("ADM-2026-59384");
        assertThat(dto.getApplicantName()).isEqualTo("Maria Santos");
        assertThat(dto.getExamScore()).isEqualTo(BigDecimal.valueOf(88.50));
        assertThat(dto.getIs4psBeneficiary()).isTrue();
        assertThat(dto.getIsPersonWithDisability()).isTrue();
        assertThat(dto.getSocioeconomicRiskScore()).isEqualTo(BigDecimal.valueOf(80.00));
        assertThat(dto.getTargetProgramCode()).isEqualTo("BSCS");
    }

    @Test
    @DisplayName("Should retrieve single applicant equity dossier by application number")
    void getApplicantEquityDossier_Success() {
        Program program = Program.builder().id(1L).code("BSIT").name("Bachelor of Science in Information Technology").build();
        Term term = Term.builder().id(10L).build();

        AdmissionApplication app = AdmissionApplication.builder()
                .id(51L)
                .applicationNumber("ADM-2026-59384")
                .firstName("Juan")
                .lastName("Dela Cruz")
                .email("juan.dc@gmail.com")
                .targetProgram(program)
                .term(term)
                .applicationStatus(AdmissionApplication.ApplicationStatus.ELIGIBLE_FOR_ENROLLMENT)
                .examScore(BigDecimal.valueOf(92.00))
                .isFarmerFisherfolk(true)
                .rsbsaRegistrationNumber("RSBSA-998811")
                .build();

        when(admissionApplicationRepository.findByApplicationNumber("ADM-2026-59384")).thenReturn(Optional.of(app));
        when(equityTargetService.calculateApplicantSocioeconomicRiskScore(app)).thenReturn(BigDecimal.valueOf(45.00));

        ApplicantEquityAuditDto dossier = equityProfileService.getApplicantEquityDossier("ADM-2026-59384");

        assertThat(dossier).isNotNull();
        assertThat(dossier.getApplicationNumber()).isEqualTo("ADM-2026-59384");
        assertThat(dossier.getApplicantName()).isEqualTo("Juan Dela Cruz");
        assertThat(dossier.getExamScore()).isEqualTo(BigDecimal.valueOf(92.00));
        assertThat(dossier.getIsFarmerFisherfolk()).isTrue();
        assertThat(dossier.getRsbsaRegistrationNumber()).isEqualTo("RSBSA-998811");
        assertThat(dossier.getSocioeconomicRiskScore()).isEqualTo(BigDecimal.valueOf(45.00));
    }

    @Test
    @DisplayName("Should aggregate post-exam applicant equity statistics")
    void getPostExamApplicantEquityStatistics_Success() {
        AdmissionApplicationRepository.PostExamApplicantEquityStatisticsProjection projection = mock(AdmissionApplicationRepository.PostExamApplicantEquityStatisticsProjection.class);
        when(projection.getTotalPostExamCount()).thenReturn(50L);
        when(projection.getExamPassedCount()).thenReturn(40L);
        when(projection.getExamFailedCount()).thenReturn(10L);
        when(projection.getFourPsCount()).thenReturn(15L);
        when(projection.getIpCount()).thenReturn(5L);
        when(projection.getPwdCount()).thenReturn(3L);
        when(projection.getSoloParentCount()).thenReturn(8L);
        when(projection.getOrphanCount()).thenReturn(2L);
        when(projection.getGidaCount()).thenReturn(12L);
        when(projection.getFarmerFisherfolkCount()).thenReturn(14L);
        when(projection.getBottom40Count()).thenReturn(25L);
        when(projection.getFirstGenCount()).thenReturn(20L);

        when(admissionApplicationRepository.getPostExamApplicantEquityStatistics()).thenReturn(projection);

        ApplicantEquityStatsDto stats = equityProfileService.getPostExamApplicantEquityStatistics();

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalPostExamCount()).isEqualTo(50L);
        assertThat(stats.getExamPassedCount()).isEqualTo(40L);
        assertThat(stats.getCount4psBeneficiaries()).isEqualTo(15L);
        assertThat(stats.getCountIndigenousPeoples()).isEqualTo(5L);
        assertThat(stats.getCountPersonsWithDisabilities()).isEqualTo(3L);
        assertThat(stats.getCountBottom40IncomeBracket()).isEqualTo(25L);
    }

    @Test
    @DisplayName("Should return zero defaults without NPE when projection is null")
    void getPostExamApplicantEquityStatistics_NullProjection_ReturnsZeroDefaults() {
        when(admissionApplicationRepository.getPostExamApplicantEquityStatistics()).thenReturn(null);

        ApplicantEquityStatsDto stats = equityProfileService.getPostExamApplicantEquityStatistics();

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalPostExamCount()).isEqualTo(0L);
        assertThat(stats.getExamPassedCount()).isEqualTo(0L);
        assertThat(stats.getCount4psBeneficiaries()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should return zero defaults without NPE when projection fields return null")
    void getPostExamApplicantEquityStatistics_NullFields_ReturnsZeroDefaults() {
        AdmissionApplicationRepository.PostExamApplicantEquityStatisticsProjection projection =
                mock(AdmissionApplicationRepository.PostExamApplicantEquityStatisticsProjection.class);
        when(projection.getTotalPostExamCount()).thenReturn(null);
        when(projection.getExamPassedCount()).thenReturn(null);
        when(projection.getExamFailedCount()).thenReturn(null);
        when(projection.getFourPsCount()).thenReturn(null);
        when(admissionApplicationRepository.getPostExamApplicantEquityStatistics()).thenReturn(projection);

        ApplicantEquityStatsDto stats = equityProfileService.getPostExamApplicantEquityStatistics();

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalPostExamCount()).isEqualTo(0L);
        assertThat(stats.getExamPassedCount()).isEqualTo(0L);
        assertThat(stats.getCount4psBeneficiaries()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should guard against division by zero in computePercentage")
    void computePercentage_ZeroDivisionGuard() {
        double resultZeroDenominator = equityProfileService.computePercentage(15, 0);
        assertThat(resultZeroDenominator).isEqualTo(0.0);

        double resultNormal = equityProfileService.computePercentage(25, 100);
        assertThat(resultNormal).isEqualTo(25.0);

        double resultRounding = equityProfileService.computePercentage(1, 3);
        assertThat(resultRounding).isEqualTo(33.33);
    }
}
