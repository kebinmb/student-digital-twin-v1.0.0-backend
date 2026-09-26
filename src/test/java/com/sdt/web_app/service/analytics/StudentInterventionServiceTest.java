package com.sdt.web_app.service.analytics;

import com.sdt.web_app.dto.analytics.AnalyticsDtos.*;
import com.sdt.web_app.entities.analytics.StudentIntervention;
import com.sdt.web_app.entities.analytics.StudentRiskScore;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.analytics.StudentInterventionRepository;
import com.sdt.web_app.repositories.analytics.StudentRiskScoreRepository;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.service.security.StudentProfileL2CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentInterventionServiceTest {

    @Mock private StudentInterventionRepository interventionRepository;
    @Mock private StudentRiskScoreRepository riskScoreRepository;
    @Mock private UserRepository userRepository;
    @Mock private StudentProfileL2CacheService studentProfileL2CacheService;

    @InjectMocks
    private StudentInterventionService interventionService;

    private StudentProfile mockStudent;
    private User mockCounselor;
    private StudentRiskScore mockRiskScore;

    @BeforeEach
    void setUp() {
        mockStudent = StudentProfile.builder().id(100L).studentNumber("2024-00100").build();
        mockCounselor = User.builder().id(50L).username("counselor_jane").build();
        mockRiskScore = StudentRiskScore.builder()
                .id(1L)
                .student(mockStudent)
                .academicRiskScore(new BigDecimal("75.00"))
                .attendanceRiskScore(new BigDecimal("80.00"))
                .socioeconomicRiskScore(new BigDecimal("30.00"))
                .compositeRiskLevel(StudentRiskScore.RiskLevel.CRITICAL)
                .predictedDropoutProbability(new BigDecimal("0.7200"))
                .build();
    }

    @Test
    @DisplayName("Dispatch intervention assigns counselor and returns OPEN/ASSIGNED case")
    void dispatchIntervention_Success() {
        when(studentProfileL2CacheService.findById(100L)).thenReturn(mockStudent);
        when(riskScoreRepository.findTopByStudentIdOrderByEvaluatedAtDesc(100L)).thenReturn(Optional.of(mockRiskScore));
        when(userRepository.findById(50L)).thenReturn(Optional.of(mockCounselor));

        StudentIntervention saved = StudentIntervention.builder()
                .id(10L)
                .student(mockStudent)
                .riskScore(mockRiskScore)
                .interventionType(StudentIntervention.InterventionType.ACADEMIC_TUTORING)
                .status(StudentIntervention.InterventionStatus.ASSIGNED)
                .assignedCounselor(mockCounselor)
                .triggerFactor("Academic Deficit")
                .caseNotes("Schedule remedial session")
                .build();

        when(interventionRepository.save(any(StudentIntervention.class))).thenReturn(saved);

        DispatchInterventionRequest request = new DispatchInterventionRequest(
                100L, null, "ACADEMIC_TUTORING", 50L, "Academic Deficit", "Schedule remedial session"
        );

        StudentInterventionDto dto = interventionService.dispatchIntervention(request);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.status()).isEqualTo("ASSIGNED");
        assertThat(dto.assignedCounselorName()).isEqualTo("counselor_jane");
        assertThat(dto.triggerFactor()).isEqualTo("Academic Deficit");
    }

    @Test
    @DisplayName("Update intervention status to RESOLVED sets resolvedAt timestamp")
    void updateInterventionStatus_Resolved() {
        StudentIntervention existing = StudentIntervention.builder()
                .id(10L)
                .student(mockStudent)
                .interventionType(StudentIntervention.InterventionType.GUIDANCE_COUNSELING)
                .status(StudentIntervention.InterventionStatus.IN_PROGRESS)
                .triggerFactor("Attendance Warning")
                .build();

        when(interventionRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(interventionRepository.save(any(StudentIntervention.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateInterventionStatusRequest req = new UpdateInterventionStatusRequest(
                "RESOLVED", "Student agreed to make up classes", "Conference held on 2026-09-25"
        );

        StudentInterventionDto dto = interventionService.updateInterventionStatus(10L, req);

        assertThat(dto.status()).isEqualTo("RESOLVED");
        assertThat(dto.resolutionSummary()).isEqualTo("Student agreed to make up classes");
        assertThat(dto.resolvedAt()).isNotNull();
    }
}
