package com.sdt.web_app.service.analytics;

import com.sdt.web_app.dto.analytics.AnalyticsDtos.*;
import com.sdt.web_app.entities.analytics.StudentRiskScore;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.grade.ClassRecordItem;
import com.sdt.web_app.entities.grade.SectionGradingCategory;
import com.sdt.web_app.entities.grade.StudentAssessmentScore;
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
import java.util.List;
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
    @Mock private com.sdt.web_app.repositories.grade.StudentAssessmentScoreRepository assessmentScoreRepository;
    @Mock private com.sdt.web_app.repositories.analytics.StudentInterventionRepository interventionRepository;
    @Mock private com.sdt.web_app.service.lms.StudentNotificationPublisherService studentNotificationPublisherService;
    @Mock private com.sdt.web_app.config.WebSocketBroadcastService broadcastService;

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

    @Test
    @DisplayName("Recalculate ML Model updates existing StudentRiskScore in place without creating duplicate")
    void evaluateStudentRiskProfile_ExistingRecord_UpdatesInPlace() {
        StudentRiskScore existingRisk = StudentRiskScore.builder()
                .id(99L)
                .student(mockStudent)
                .academicRiskScore(new BigDecimal("20.00"))
                .attendanceRiskScore(new BigDecimal("15.00"))
                .socioeconomicRiskScore(new BigDecimal("10.00"))
                .compositeRiskLevel(StudentRiskScore.RiskLevel.LOW)
                .predictedDropoutProbability(new BigDecimal("0.0500"))
                .build();

        when(studentProfileL2CacheService.findById(10L)).thenReturn(mockStudent);
        when(riskScoreRepository.findTopByStudentIdOrderByEvaluatedAtDesc(10L)).thenReturn(Optional.of(existingRisk));
        when(attendanceRecordRepository.countTotalByStudentId(10L)).thenReturn(10L);
        when(attendanceRecordRepository.countPresentByStudentId(10L)).thenReturn(5L);
        when(equityTargetService.calculateSocioeconomicRiskScore(10L)).thenReturn(new BigDecimal("40.00"));
        when(riskScoreRepository.save(any(StudentRiskScore.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DigitalTwinRiskProfileDto res = riskService.evaluateStudentRiskProfile(10L);

        // Verify that the same existing entity was mutated and saved, maintaining id 99L
        verify(riskScoreRepository).save(existingRisk);
        assertThat(res).isNotNull();
        assertThat(existingRisk.getId()).isEqualTo(99L);
        assertThat(existingRisk.getCompositeRiskLevel()).isEqualTo(StudentRiskScore.RiskLevel.CRITICAL);
    }

    @Test
    @DisplayName("Early Warning Radar deduplicates multiple records for the same student, keeping latest")
    void getEarlyWarningRadar_DeduplicatesMultipleRecordsForSameStudent() {
        StudentRiskScore olderScore = StudentRiskScore.builder()
                .id(1L)
                .student(mockStudent)
                .evaluatedAt(java.time.Instant.now().minusSeconds(3600))
                .academicRiskScore(new BigDecimal("70.00"))
                .attendanceRiskScore(new BigDecimal("60.00"))
                .socioeconomicRiskScore(new BigDecimal("20.00"))
                .compositeRiskLevel(StudentRiskScore.RiskLevel.HIGH)
                .predictedDropoutProbability(new BigDecimal("0.4500"))
                .build();

        StudentRiskScore newerScore = StudentRiskScore.builder()
                .id(2L)
                .student(mockStudent)
                .evaluatedAt(java.time.Instant.now())
                .academicRiskScore(new BigDecimal("95.00"))
                .attendanceRiskScore(new BigDecimal("80.00"))
                .socioeconomicRiskScore(new BigDecimal("40.00"))
                .compositeRiskLevel(StudentRiskScore.RiskLevel.CRITICAL)
                .predictedDropoutProbability(new BigDecimal("0.8500"))
                .build();

        when(riskScoreRepository.findByRiskLevelsWithDetails(anyList())).thenReturn(List.of(olderScore, newerScore));

        List<EarlyWarningRadarItemDto> radar = riskService.getEarlyWarningRadar();

        // Must only return 1 item for student 10L, with the newer score
        assertThat(radar).hasSize(1);
        assertThat(radar.get(0).studentId()).isEqualTo(10L);
        assertThat(radar.get(0).riskLevel()).isEqualTo("CRITICAL");
        assertThat(radar.get(0).dropoutProbability()).isEqualTo(new BigDecimal("0.8500"));
    }

    @Test
    @DisplayName("Evaluate risk profile computes weighted in-term scores and generates activity-specific review suggestions")
    void evaluateStudentRiskProfile_DynamicClassRecordWeightsAndActivitySuggestions() {
        when(studentProfileL2CacheService.findById(10L)).thenReturn(mockStudent);
        when(attendanceRecordRepository.countTotalByStudentId(10L)).thenReturn(10L);
        when(attendanceRecordRepository.countPresentByStudentId(10L)).thenReturn(8L);
        when(equityTargetService.calculateSocioeconomicRiskScore(10L)).thenReturn(new BigDecimal("10.00"));

        SectionGradingCategory quizCategory = SectionGradingCategory.builder()
                .id(1L)
                .categoryName("Quizzes")
                .weightPercentage(new BigDecimal("20.00"))
                .termPeriod(SectionGradingCategory.TermPeriod.MIDTERM)
                .build();

        SectionGradingCategory examCategory = SectionGradingCategory.builder()
                .id(2L)
                .categoryName("Major Exam")
                .weightPercentage(new BigDecimal("50.00"))
                .termPeriod(SectionGradingCategory.TermPeriod.MIDTERM)
                .build();

        ClassRecordItem quiz1 = ClassRecordItem.builder()
                .id(101L)
                .category(quizCategory)
                .itemTitle("Quiz 1")
                .maxPoints(new BigDecimal("50.00"))
                .build();

        ClassRecordItem quiz2 = ClassRecordItem.builder()
                .id(102L)
                .category(quizCategory)
                .itemTitle("Quiz 2")
                .maxPoints(new BigDecimal("50.00"))
                .build();

        ClassRecordItem exam1 = ClassRecordItem.builder()
                .id(201L)
                .category(examCategory)
                .itemTitle("Midterm Exam")
                .maxPoints(new BigDecimal("100.00"))
                .build();

        // Quiz 1: 20/50 (40.0% -> failing)
        StudentAssessmentScore scoreQuiz1 = StudentAssessmentScore.builder()
                .id(1001L)
                .student(mockStudent)
                .item(quiz1)
                .scoreEarned(new BigDecimal("20.00"))
                .isExcused(false)
                .build();

        // Quiz 2: 45/50 (90.0% -> passing)
        StudentAssessmentScore scoreQuiz2 = StudentAssessmentScore.builder()
                .id(1002L)
                .student(mockStudent)
                .item(quiz2)
                .scoreEarned(new BigDecimal("45.00"))
                .isExcused(false)
                .build();

        // Midterm Exam: 50/100 (50.0% -> failing)
        StudentAssessmentScore scoreExam1 = StudentAssessmentScore.builder()
                .id(2001L)
                .student(mockStudent)
                .item(exam1)
                .scoreEarned(new BigDecimal("50.00"))
                .isExcused(false)
                .build();

        when(assessmentScoreRepository.findByStudentIdWithDetails(10L))
                .thenReturn(List.of(scoreQuiz1, scoreQuiz2, scoreExam1));
        when(riskScoreRepository.save(any(StudentRiskScore.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DigitalTwinRiskProfileDto res = riskService.evaluateStudentRiskProfile(10L);

        assertThat(res).isNotNull();
        // Check activity alerts
        assertThat(res.activityAlerts()).hasSize(2);
        assertThat(res.activityAlerts()).extracting(ActivityAlertDto::activityTitle)
                .containsExactly("Quiz 1", "Midterm Exam");
        assertThat(res.activityAlerts()).extracting(ActivityAlertDto::suggestion)
                .containsExactly("Review Quiz 1 Topics", "Review Midterm Exam Topics");

        // Check interventions list
        assertThat(res.recommendedInterventions())
                .contains("Review Quiz 1 Topics", "Review Midterm Exam Topics");
    }
}
