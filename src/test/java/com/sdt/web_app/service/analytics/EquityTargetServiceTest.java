package com.sdt.web_app.service.analytics;

import com.sdt.web_app.entities.compliance.StudentEquityProfile;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.DisabilityType;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.HouseholdIncomeBracket;
import com.sdt.web_app.entities.enrollment.StudentProfile.EnrollmentStatus;
import com.sdt.web_app.entities.enrollment.StudentProfile.ClearanceStatus;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.compliance.StudentEquityProfileRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquityTargetServiceTest {

    @Mock
    private StudentProfileRepository profileRepository;

    @Mock
    private StudentEquityProfileRepository equityProfileRepository;

    @InjectMocks
    private EquityTargetService equityTargetService;

    private StudentProfile regularStudent;

    @BeforeEach
    void setUp() {
        regularStudent = StudentProfile.builder()
                .id(10L)
                .studentNumber("2026-0001")
                .financialClearance(ClearanceStatus.CLEARED)
                .enrollmentStatus(EnrollmentStatus.REGULAR)
                .build();
    }

    @Test
    @DisplayName("Should calculate baseline socioeconomic risk score when no equity profile exists")
    void calculateSocioeconomicRiskScore_NoEquityProfile_ReturnsBaseline() {
        when(profileRepository.findById(10L)).thenReturn(Optional.of(regularStudent));
        when(equityProfileRepository.findByStudentProfileId(10L)).thenReturn(Optional.empty());

        BigDecimal score = equityTargetService.calculateSocioeconomicRiskScore(10L);

        // Baseline score is 10.00
        assertThat(score).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    @DisplayName("Should aggregate statutory equity indicator signals into socioeconomic risk score")
    void calculateSocioeconomicRiskScore_WithEquityProfile_AggregatesSignals() {
        StudentEquityProfile equity = StudentEquityProfile.builder()
                .id(100L)
                .studentProfile(regularStudent)
                .is4psBeneficiary(true) // +15.0
                .isPersonWithDisability(true) // +15.0
                .disabilityType(DisabilityType.VISUAL)
                .isSoloParent(true) // +10.0
                .isOrphan(true) // +15.0
                .isGidaResident(true) // +10.0
                .isFarmerFisherfolk(true) // +10.0
                .isBottom40IncomeBracket(true) // +20.0
                .monthlyHouseholdIncomeBracket(HouseholdIncomeBracket.POOR_BELOW_10K)
                .isFirstGenerationCollege(true) // +10.0
                .build();

        when(profileRepository.findById(10L)).thenReturn(Optional.of(regularStudent));
        when(equityProfileRepository.findByStudentProfileId(10L)).thenReturn(Optional.of(equity));

        BigDecimal score = equityTargetService.calculateSocioeconomicRiskScore(10L);

        // Base 10 + 20 (bottom 40%) + 15 (4ps) + 15 (pwd) + 10 (solo) + 15 (orphan) + 10 (gida) + 10 (farmer) + 10 (first gen)
        // Expected total = 115 capped at 100.00
        assertThat(score).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Should add moderate risk for low income bracket and first generation student")
    void calculateSocioeconomicRiskScore_ModerateEquityProfile() {
        StudentEquityProfile equity = StudentEquityProfile.builder()
                .id(101L)
                .studentProfile(regularStudent)
                .is4psBeneficiary(false)
                .isPersonWithDisability(false)
                .isSoloParent(false)
                .isRaisedBySoloParent(true) // +10.0
                .isOrphan(false)
                .isGidaResident(false)
                .isFarmerFisherfolk(false)
                .isBottom40IncomeBracket(false)
                .monthlyHouseholdIncomeBracket(HouseholdIncomeBracket.LOW_INCOME_10K_TO_20K) // +10.0
                .isFirstGenerationCollege(true) // +10.0
                .build();

        when(profileRepository.findById(10L)).thenReturn(Optional.of(regularStudent));
        when(equityProfileRepository.findByStudentProfileId(10L)).thenReturn(Optional.of(equity));

        BigDecimal score = equityTargetService.calculateSocioeconomicRiskScore(10L);

        // Base 10 + 10 (low income) + 10 (raised by solo) + 10 (first-gen) = 40.00
        assertThat(score).isEqualByComparingTo(new BigDecimal("40.00"));
    }
}
