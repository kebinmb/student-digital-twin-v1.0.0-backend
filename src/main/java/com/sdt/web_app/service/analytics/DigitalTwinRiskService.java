package com.sdt.web_app.service.analytics;

import com.sdt.web_app.dto.analytics.AnalyticsDtos.*;
import com.sdt.web_app.entities.analytics.StudentRiskScore;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.analytics.AttendanceRecordRepository;
import com.sdt.web_app.repositories.analytics.StudentRiskScoreRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalTwinRiskService {

    private final StudentProfileRepository profileRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final StudentRiskScoreRepository riskScoreRepository;
    private final EquityTargetService equityTargetService;

    @Transactional
    public DigitalTwinRiskProfileDto evaluateStudentRiskProfile(Long studentId) {
        StudentProfile student = profileRepository.findByIdWithProgramAndCurriculum(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found: " + studentId));

        // 1. Calculate Academic Risk Score (0.00 to 100.00) based on GPA
        BigDecimal gpa = student.getCumulativeGpa();
        double academicRisk = 10.0;
        if (gpa != null) {
            // Philippine grading scale: 1.00 is highest, 3.00 is passing, 5.00 is failing.
            double gpaVal = gpa.doubleValue();
            if (gpaVal > 3.00) {
                academicRisk = 90.0;
            } else if (gpaVal > 2.50) {
                academicRisk = 60.0;
            } else if (gpaVal > 2.00) {
                academicRisk = 30.0;
            } else {
                academicRisk = 10.0;
            }
        }

        // 2. Calculate Attendance Risk Score (0.00 to 100.00)
        long totalAtt = attendanceRecordRepository.countTotalByStudentId(student.getId());
        long presentAtt = attendanceRecordRepository.countPresentByStudentId(student.getId());
        double attendanceRisk = 15.0;
        if (totalAtt > 0) {
            double rate = (double) presentAtt / totalAtt;
            if (rate < 0.70) attendanceRisk = 85.0;
            else if (rate < 0.85) attendanceRisk = 45.0;
            else attendanceRisk = 10.0;
        }

        // 3. Socioeconomic Equity Risk Score
        BigDecimal socioeconomicRisk = equityTargetService.calculateSocioeconomicRiskScore(student.getId());

        // 4. Composite Risk Level & Dropout Probability
        double compositeScore = (academicRisk * 0.50) + (attendanceRisk * 0.30) + (socioeconomicRisk.doubleValue() * 0.20);
        StudentRiskScore.RiskLevel level;
        double dropoutProb;

        if (compositeScore >= 70.0) {
            level = StudentRiskScore.RiskLevel.CRITICAL;
            dropoutProb = 0.6500;
        } else if (compositeScore >= 50.0) {
            level = StudentRiskScore.RiskLevel.HIGH;
            dropoutProb = 0.3500;
        } else if (compositeScore >= 30.0) {
            level = StudentRiskScore.RiskLevel.MODERATE;
            dropoutProb = 0.1500;
        } else {
            level = StudentRiskScore.RiskLevel.LOW;
            dropoutProb = 0.0300;
        }

        List<String> interventions = new ArrayList<>();
        if (academicRisk >= 50.0) interventions.add("Dispatch peer-tutoring and academic remediation counseling.");
        if (attendanceRisk >= 45.0) interventions.add("Notify academic advisor for attendance warning and class engagement interview.");
        if (socioeconomicRisk.doubleValue() >= 35.0) interventions.add("Refer to Guidance Office for UniFAST FHE emergency student subsidy assistance.");
        if (interventions.isEmpty()) interventions.add("Student performance is on track. Maintain standard academic advising monitoring.");

        StudentRiskScore riskEntity = StudentRiskScore.builder()
                .student(student)
                .academicRiskScore(new BigDecimal(academicRisk).setScale(2, RoundingMode.HALF_UP))
                .attendanceRiskScore(new BigDecimal(attendanceRisk).setScale(2, RoundingMode.HALF_UP))
                .socioeconomicRiskScore(socioeconomicRisk)
                .compositeRiskLevel(level)
                .predictedDropoutProbability(new BigDecimal(dropoutProb).setScale(4, RoundingMode.HALF_UP))
                .recommendedInterventions(String.join("; ", interventions))
                .build();

        StudentRiskScore saved = riskScoreRepository.save(riskEntity);
        log.info("Digital Twin ML Risk evaluated for student {}. Level: {}, Dropout Prob: {}", student.getStudentNumber(), level, dropoutProb);

        String studentName = student.getUser() != null ? student.getUser().getUsername() : "Student #" + student.getStudentNumber();
        return new DigitalTwinRiskProfileDto(
                student.getId(),
                student.getStudentNumber(),
                studentName,
                student.getProgram() != null ? student.getProgram().getCode() : "N/A",
                student.getYearLevel(),
                saved.getAcademicRiskScore(),
                saved.getAttendanceRiskScore(),
                saved.getSocioeconomicRiskScore(),
                saved.getCompositeRiskLevel().name(),
                saved.getPredictedDropoutProbability(),
                interventions,
                saved.getEvaluatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<EarlyWarningRadarItemDto> getEarlyWarningRadar() {
        List<StudentRiskScore> highRiskScores = riskScoreRepository.findByRiskLevelsWithDetails(
                List.of(StudentRiskScore.RiskLevel.HIGH, StudentRiskScore.RiskLevel.CRITICAL)
        );

        return highRiskScores.stream()
                .map(srs -> {
                    StudentProfile sp = srs.getStudent();
                    String name = sp.getUser() != null ? sp.getUser().getUsername() : "Student #" + sp.getStudentNumber();
                    String factor = srs.getAcademicRiskScore().doubleValue() > srs.getAttendanceRiskScore().doubleValue()
                            ? "Academic Deficit (GPA " + (sp.getCumulativeGpa() != null ? sp.getCumulativeGpa() : "3.50") + ")"
                            : "Attendance Absences";

                    return new EarlyWarningRadarItemDto(
                            sp.getId(),
                            sp.getStudentNumber(),
                            name,
                            sp.getProgram() != null ? sp.getProgram().getCode() : "N/A",
                            sp.getYearLevel(),
                            srs.getCompositeRiskLevel().name(),
                            srs.getPredictedDropoutProbability(),
                            factor,
                            srs.getRecommendedInterventions() != null ? srs.getRecommendedInterventions() : "Dispatch Academic Counselor"
                    );
                })
                .toList();
    }
}
