package com.sdt.web_app.service.analytics;

import com.sdt.web_app.dto.analytics.AnalyticsDtos.*;
import com.sdt.web_app.entities.analytics.StudentRiskScore;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.analytics.AttendanceRecordRepository;
import com.sdt.web_app.repositories.analytics.StudentRiskScoreRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.sdt.web_app.service.security.StudentProfileL2CacheService;

@ExtendWith(MockitoExtension.class)
class DigitalTwinRiskServiceTest {

    @Mock private StudentProfileRepository profileRepository;
    @Mock private AttendanceRecordRepository attendanceRecordRepository;
    @Mock private StudentRiskScoreRepository riskScoreRepository;
    @Mock private EquityTargetService equityTargetService;
    @Mock private StudentProfileL2CacheService studentProfileL2CacheService;

    @InjectMocks
    private DigitalTwinRiskService riskService;

    private StudentProfile mockStudent;

    @BeforeEach
    void setUp() {
        mockStudent = StudentProfile.builder()
                .id(10L)
                .studentNumber("2024-9999")
                .cumulativeGpa(new BigDecimal("3.75"))
                .yearLevel(2)
                .build();
    }

    @Test
    @DisplayName("Evaluate risk profile generates HIGH/CRITICAL risk for low GPA & poor attendance")
    void evaluateStudentRiskProfile_HighRisk() {
        when(studentProfileL2CacheService.findById(10L)).thenReturn(mockStudent);
        when(attendanceRecordRepository.countTotalByStudentId(10L)).thenReturn(10L);
        when(attendanceRecordRepository.countPresentByStudentId(10L)).thenReturn(5L); // 50% attendance
        when(equityTargetService.calculateSocioeconomicRiskScore(10L)).thenReturn(new BigDecimal("40.00"));

        StudentRiskScore savedRisk = StudentRiskScore.builder()
                .id(1L)
                .student(mockStudent)
                .academicRiskScore(new BigDecimal("90.00"))
                .attendanceRiskScore(new BigDecimal("85.00"))
                .socioeconomicRiskScore(new BigDecimal("40.00"))
                .compositeRiskLevel(StudentRiskScore.RiskLevel.CRITICAL)
                .predictedDropoutProbability(new BigDecimal("0.6500"))
                .build();

        when(riskScoreRepository.save(any(StudentRiskScore.class))).thenReturn(savedRisk);

        DigitalTwinRiskProfileDto res = riskService.evaluateStudentRiskProfile(10L);

        assertThat(res).isNotNull();
        assertThat(res.compositeRiskLevel()).isEqualTo("CRITICAL");
        assertThat(res.predictedDropoutProbability()).isEqualTo(new BigDecimal("0.6500"));
        assertThat(res.recommendedInterventions()).isNotEmpty();
    }
}
