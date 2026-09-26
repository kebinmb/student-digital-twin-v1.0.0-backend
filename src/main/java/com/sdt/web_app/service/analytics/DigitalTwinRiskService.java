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

import com.sdt.web_app.dto.common.SliceResponse;
import com.sdt.web_app.utils.SortPropertyMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sdt.web_app.service.security.StudentProfileL2CacheService;
import java.util.Optional;

import com.sdt.web_app.entities.grade.ClassRecordItem;
import com.sdt.web_app.entities.grade.SectionGradingCategory;
import com.sdt.web_app.entities.grade.StudentAssessmentScore;
import com.sdt.web_app.repositories.grade.StudentAssessmentScoreRepository;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalTwinRiskService {

    private final StudentProfileRepository profileRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final StudentRiskScoreRepository riskScoreRepository;
    private final EquityTargetService equityTargetService;
    private final StudentProfileL2CacheService studentProfileL2CacheService;
    private final StudentAssessmentScoreRepository assessmentScoreRepository;

    @Transactional
    public DigitalTwinRiskProfileDto evaluateStudentRiskProfile(Long studentId) {
        StudentProfile student = Optional.ofNullable(studentProfileL2CacheService.findById(studentId))
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found: " + studentId));

        // 1. Calculate Continuous Academic Risk Score (0.00 to 100.00)
        // Sigmoidal mapping of Philippine grading scale: 1.00 (highest) to 3.00 (passing) to 5.00 (failing)
        BigDecimal gpa = student.getCumulativeGpa();
        double gpaRisk = 10.0;
        if (gpa != null) {
            double gpaVal = gpa.doubleValue();
            // Continuous sigmoid centered at midpoint 2.75 with steepness 3.5
            gpaRisk = 100.0 / (1.0 + Math.exp(-3.5 * (gpaVal - 2.75)));
        }

        // Live In-Term Continuous Assessment Telemetry from StudentAssessmentScore
        // Accounting for Dynamic Class Record Categories & Weights
        List<StudentAssessmentScore> inTermScores = assessmentScoreRepository != null 
                ? assessmentScoreRepository.findByStudentIdWithDetails(student.getId()) 
                : List.of();
        if (inTermScores == null) {
            inTermScores = List.of();
        }

        List<ActivityAlertDto> activityAlerts = new ArrayList<>();
        Map<Long, List<StudentAssessmentScore>> scoresByCategory = new LinkedHashMap<>();
        Map<Long, SectionGradingCategory> categoryMap = new LinkedHashMap<>();
        double rawTotalEarned = 0.0;
        double rawTotalMax = 0.0;

        for (StudentAssessmentScore score : inTermScores) {
            if (score == null || score.isExcused()) {
                continue;
            }
            ClassRecordItem item = score.getItem();
            if (item == null || item.getMaxPoints() == null || item.getMaxPoints().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (score.getScoreEarned() == null) {
                continue;
            }

            double earnedVal = score.getScoreEarned().doubleValue();
            double maxVal = item.getMaxPoints().doubleValue();
            rawTotalEarned += earnedVal;
            rawTotalMax += maxVal;

            double itemPct = (earnedVal / maxVal) * 100.0;
            SectionGradingCategory cat = item.getCategory();

            // Low or failing score threshold (< 75.0% passing mark)
            if (itemPct < 75.0) {
                String itemTitle = (item.getItemTitle() != null && !item.getItemTitle().isBlank())
                        ? item.getItemTitle().trim()
                        : "Activity";
                String catName = (cat != null && cat.getCategoryName() != null && !cat.getCategoryName().isBlank())
                        ? cat.getCategoryName().trim()
                        : "Assessment";
                String suggestion = "Review " + itemTitle + " Topics";

                activityAlerts.add(new ActivityAlertDto(
                        itemTitle,
                        catName,
                        score.getScoreEarned(),
                        item.getMaxPoints(),
                        BigDecimal.valueOf(itemPct).setScale(2, RoundingMode.HALF_UP),
                        suggestion
                ));
            }

            if (cat != null && cat.getId() != null && cat.getWeightPercentage() != null && cat.getWeightPercentage().compareTo(BigDecimal.ZERO) > 0) {
                scoresByCategory.computeIfAbsent(cat.getId(), k -> new ArrayList<>()).add(score);
                categoryMap.putIfAbsent(cat.getId(), cat);
            }
        }

        double academicRisk;
        double weightedSum = 0.0;
        double assessedWeightSum = 0.0;

        for (Map.Entry<Long, List<StudentAssessmentScore>> entry : scoresByCategory.entrySet()) {
            SectionGradingCategory cat = categoryMap.get(entry.getKey());
            double catEarned = 0.0;
            double catMax = 0.0;
            for (StudentAssessmentScore sas : entry.getValue()) {
                catEarned += sas.getScoreEarned().doubleValue();
                catMax += sas.getItem().getMaxPoints().doubleValue();
            }
            if (catMax > 0.0) {
                double catPct = (catEarned / catMax) * 100.0;
                double weight = cat.getWeightPercentage().doubleValue();
                weightedSum += catPct * weight;
                assessedWeightSum += weight;
            }
        }

        if (assessedWeightSum > 0.0) {
            double normalizedInTermPct = weightedSum / assessedWeightSum;
            double inTermRisk = Math.max(0.0, Math.min(100.0, 100.0 - normalizedInTermPct));
            // 40% historical cumulative GPA + 60% live continuous assessments in progress
            academicRisk = (gpaRisk * 0.40) + (inTermRisk * 0.60);
        } else if (rawTotalMax > 0.0) {
            double inTermRate = rawTotalEarned / rawTotalMax;
            double inTermRisk = Math.max(0.0, Math.min(100.0, (1.0 - inTermRate) * 100.0));
            academicRisk = (gpaRisk * 0.40) + (inTermRisk * 0.60);
        } else {
            academicRisk = gpaRisk;
        }

        // 2. Calculate Continuous Attendance Risk Score (0.00 to 100.00) with Trailing Momentum Velocity
        long totalAtt = attendanceRecordRepository.countTotalByStudentId(student.getId());
        long presentAtt = attendanceRecordRepository.countPresentByStudentId(student.getId());
        double attendanceRisk = 15.0;
        double velocity = 0.0;

        if (totalAtt > 0) {
            double lifetimeRate = (double) presentAtt / totalAtt;
            double baseAttendanceRisk = Math.max(0.0, (1.0 - lifetimeRate) * 100.0);

            // Compute 14-day trailing velocity
            java.time.Instant fourteenDaysAgo = java.time.Instant.now().minus(14, java.time.temporal.ChronoUnit.DAYS);
            long recentTotal = attendanceRecordRepository.countRecentTotalByStudentId(student.getId(), fourteenDaysAgo);
            long recentPresent = attendanceRecordRepository.countRecentPresentByStudentId(student.getId(), fourteenDaysAgo);

            if (recentTotal >= 2) {
                double recentRate = (double) recentPresent / recentTotal;
                velocity = recentRate - lifetimeRate; // Negative indicates rapid drop in engagement
                if (velocity < 0) {
                    baseAttendanceRisk = Math.min(100.0, baseAttendanceRisk + ((-velocity) * 40.0));
                } else if (velocity > 0.10) {
                    baseAttendanceRisk = Math.max(0.0, baseAttendanceRisk - (velocity * 20.0));
                }
            }
            attendanceRisk = Math.min(100.0, Math.max(0.0, baseAttendanceRisk));
        }

        // 3. Socioeconomic Equity Risk Score (UniFAST RA 10931 Indicators)
        BigDecimal socioeconomicRisk = equityTargetService.calculateSocioeconomicRiskScore(student.getId());

        // 4. Composite Risk Level & Smooth Dropout Probability
        double compositeScore = (academicRisk * 0.50) + (attendanceRisk * 0.30) + (socioeconomicRisk.doubleValue() * 0.20);
        StudentRiskScore.RiskLevel level;
        // Smooth continuous logistic function for dropout probability
        double dropoutProb = 1.0 / (1.0 + Math.exp(-0.06 * (compositeScore - 55.0)));

        if (compositeScore >= 70.0) {
            level = StudentRiskScore.RiskLevel.CRITICAL;
        } else if (compositeScore >= 50.0) {
            level = StudentRiskScore.RiskLevel.HIGH;
        } else if (compositeScore >= 30.0) {
            level = StudentRiskScore.RiskLevel.MODERATE;
        } else {
            level = StudentRiskScore.RiskLevel.LOW;
        }

        List<String> interventions = new ArrayList<>();
        // 1. Add specific activity review recommendations for low and failing scores
        for (ActivityAlertDto alert : activityAlerts) {
            if (!interventions.contains(alert.suggestion())) {
                interventions.add(alert.suggestion());
            }
        }

        // 2. High-level systemic interventions
        if (academicRisk >= 50.0) interventions.add("Dispatch peer-tutoring and academic remediation counseling.");
        if (velocity < -0.15) interventions.add("Attendance Velocity Warning: Sharp 14-day engagement decline detected.");
        if (attendanceRisk >= 45.0 && velocity >= -0.15) interventions.add("Notify academic advisor for attendance warning and class engagement interview.");
        if (socioeconomicRisk.doubleValue() >= 35.0) interventions.add("Refer to Guidance Office for UniFAST FHE emergency student subsidy assistance.");
        if (interventions.isEmpty()) interventions.add("Student performance is on track. Maintain standard academic advising monitoring.");

        String joinedInterventions = String.join("; ", interventions);
        if (joinedInterventions.length() > 950) {
            joinedInterventions = joinedInterventions.substring(0, 947) + "...";
        }

        Optional<StudentRiskScore> existingOpt = riskScoreRepository.findTopByStudentIdOrderByEvaluatedAtDesc(student.getId());
        StudentRiskScore riskEntity;
        if (existingOpt.isPresent()) {
            riskEntity = existingOpt.get();
            riskEntity.updateEvaluation(
                    new BigDecimal(academicRisk).setScale(2, RoundingMode.HALF_UP),
                    new BigDecimal(attendanceRisk).setScale(2, RoundingMode.HALF_UP),
                    socioeconomicRisk,
                    level,
                    new BigDecimal(dropoutProb).setScale(4, RoundingMode.HALF_UP),
                    joinedInterventions
            );
        } else {
            riskEntity = StudentRiskScore.builder()
                    .student(student)
                    .academicRiskScore(new BigDecimal(academicRisk).setScale(2, RoundingMode.HALF_UP))
                    .attendanceRiskScore(new BigDecimal(attendanceRisk).setScale(2, RoundingMode.HALF_UP))
                    .socioeconomicRiskScore(socioeconomicRisk)
                    .compositeRiskLevel(level)
                    .predictedDropoutProbability(new BigDecimal(dropoutProb).setScale(4, RoundingMode.HALF_UP))
                    .recommendedInterventions(joinedInterventions)
                    .build();
        }

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
                saved.getEvaluatedAt(),
                activityAlerts
        );
    }

    @Transactional(readOnly = true)
    public List<EarlyWarningRadarItemDto> getEarlyWarningRadar() {
        List<StudentRiskScore> highRiskScores = riskScoreRepository.findByRiskLevelsWithDetails(
                List.of(StudentRiskScore.RiskLevel.HIGH, StudentRiskScore.RiskLevel.CRITICAL)
        );

        // Deduplicate by studentId, keeping the most recently evaluated record
        java.util.Map<Long, StudentRiskScore> latestScoreByStudent = new java.util.LinkedHashMap<>();
        for (StudentRiskScore srs : highRiskScores) {
            if (srs.getStudent() != null) {
                Long studentId = srs.getStudent().getId();
                StudentRiskScore existing = latestScoreByStudent.get(studentId);
                if (existing == null || (srs.getEvaluatedAt() != null && existing.getEvaluatedAt() != null && srs.getEvaluatedAt().isAfter(existing.getEvaluatedAt()))) {
                    latestScoreByStudent.put(studentId, srs);
                }
            }
        }

        return latestScoreByStudent.values().stream()
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

    @Transactional(readOnly = true)
    public SliceResponse<DigitalTwinRiskProfileDto> getEarlyWarningRadarSlice(int page, int size, String sortBy, String sortDir) {
        Pageable pageable = SortPropertyMapper.createStudentRiskScorePageable(page, size, sortBy, sortDir);
        List<StudentRiskScore.RiskLevel> levels = List.of(StudentRiskScore.RiskLevel.HIGH, StudentRiskScore.RiskLevel.CRITICAL);
        Slice<StudentRiskScore> slice = riskScoreRepository.findByCompositeRiskLevelIn(levels, pageable);
        Slice<DigitalTwinRiskProfileDto> responseSlice = slice.map(srs -> {
            StudentProfile sp = srs.getStudent();
            String name = sp != null && sp.getUser() != null ? sp.getUser().getUsername() : "Student #" + (sp != null ? sp.getStudentNumber() : srs.getId());
            List<String> interventions = srs.getRecommendedInterventions() != null
                    ? Arrays.asList(srs.getRecommendedInterventions().split("; "))
                    : List.of("Dispatch Academic Counselor");

            return new DigitalTwinRiskProfileDto(
                    sp != null ? sp.getId() : null,
                    sp != null ? sp.getStudentNumber() : "N/A",
                    name,
                    sp != null && sp.getProgram() != null ? sp.getProgram().getCode() : "N/A",
                    sp != null ? sp.getYearLevel() : 1,
                    srs.getAcademicRiskScore(),
                    srs.getAttendanceRiskScore(),
                    srs.getSocioeconomicRiskScore(),
                    srs.getCompositeRiskLevel().name(),
                    srs.getPredictedDropoutProbability(),
                    interventions,
                    srs.getEvaluatedAt()
            );
        });
        return SliceResponse.from(responseSlice);
    }
}
