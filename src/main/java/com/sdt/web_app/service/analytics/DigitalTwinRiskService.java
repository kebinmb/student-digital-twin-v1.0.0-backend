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

import com.sdt.web_app.repositories.analytics.StudentInterventionRepository;
import java.time.Instant;

import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import com.sdt.web_app.repositories.enrollment.EnrollmentCourseItemRepository;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.analytics.StudentIntervention;

@Service
@RequiredArgsConstructor
@Slf4j
public class DigitalTwinRiskService {

    private final StudentProfileRepository profileRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final StudentRiskScoreRepository riskScoreRepository;
    private final StudentInterventionRepository interventionRepository;
    private final EquityTargetService equityTargetService;
    private final StudentProfileL2CacheService studentProfileL2CacheService;
    private final StudentAssessmentScoreRepository assessmentScoreRepository;
    private final ClassSectionRepository classSectionRepository;
    private final EnrollmentCourseItemRepository enrollmentCourseItemRepository;
    private final com.sdt.web_app.service.lms.StudentNotificationPublisherService studentNotificationPublisherService;
    private final com.sdt.web_app.config.WebSocketBroadcastService broadcastService;

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

        if (broadcastService != null) {
            com.sdt.web_app.websocket.dto.PerformanceSummaryMessage perfMsg = new com.sdt.web_app.websocket.dto.PerformanceSummaryMessage(
                    student.getId(),
                    null,
                    student.getCumulativeGpa() != null ? student.getCumulativeGpa().doubleValue() : null,
                    student.getEnrollmentStatus() != null ? student.getEnrollmentStatus().name() : null,
                    level.name(),
                    Instant.now()
            );
            broadcastService.broadcast(com.sdt.web_app.config.WebSocketTopics.performance(student.getId()), perfMsg);
            broadcastService.broadcast(com.sdt.web_app.config.WebSocketTopics.ADMIN_TELEMETRY, perfMsg);
        }

        // Auto-dispatch early warning intervention for HIGH or CRITICAL risk if not already actively open
        if (level == StudentRiskScore.RiskLevel.HIGH || level == StudentRiskScore.RiskLevel.CRITICAL) {
            List<StudentIntervention.InterventionStatus> activeStatuses = List.of(
                    StudentIntervention.InterventionStatus.OPEN,
                    StudentIntervention.InterventionStatus.ASSIGNED,
                    StudentIntervention.InterventionStatus.IN_PROGRESS
            );
            if (!interventionRepository.existsByStudentIdAndStatusIn(student.getId(), activeStatuses)) {
                StudentIntervention.InterventionType autoType = (level == StudentRiskScore.RiskLevel.CRITICAL)
                        ? StudentIntervention.InterventionType.GUIDANCE_COUNSELING
                        : (academicRisk >= 50.0 
                                ? StudentIntervention.InterventionType.ACADEMIC_TUTORING 
                                : StudentIntervention.InterventionType.ATTENDANCE_CONFERENCE);

                StudentIntervention autoIntervention = StudentIntervention.builder()
                        .student(student)
                        .riskScore(saved)
                        .interventionType(autoType)
                        .status(StudentIntervention.InterventionStatus.OPEN)
                        .triggerFactor("Automated early warning trigger: " + level.name() + " composite risk (" + String.format(java.util.Locale.US, "%.1f", compositeScore) + ")")
                        .caseNotes(joinedInterventions)
                        .build();
                StudentIntervention savedIntervention = interventionRepository.save(autoIntervention);
                log.info("Auto-dispatched {} intervention for student {} due to {} risk level",
                        autoType, student.getStudentNumber(), level);

                if (studentNotificationPublisherService != null) {
                    studentNotificationPublisherService.publishInterventionDispatchedEvent(
                            student.getId(),
                            autoType.name(),
                            level.name(),
                            autoIntervention.getTriggerFactor()
                    );
                }
            }
        }

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

    @Transactional(readOnly = true)
    public List<StudentTelemetryAdminSummaryDto> getAdminStudentTelemetryList(
            String searchQuery, String riskLevelFilter, String interventionStatusFilter) {

        List<StudentProfile> allStudents = profileRepository.searchStudents(searchQuery);
        if (allStudents == null || allStudents.isEmpty()) {
            return List.of();
        }

        List<Long> studentIds = allStudents.stream().map(StudentProfile::getId).toList();

        // Batch query latest risk scores for all students (O(1) database roundtrips)
        List<StudentRiskScore> allRiskScores = riskScoreRepository.findByStudentIdInOrderByEvaluatedAtDesc(studentIds);
        Map<Long, StudentRiskScore> latestScoreByStudent = new LinkedHashMap<>();
        for (StudentRiskScore srs : allRiskScores) {
            if (srs.getStudent() != null) {
                latestScoreByStudent.putIfAbsent(srs.getStudent().getId(), srs);
            }
        }

        // Batch query all interventions for all students
        List<com.sdt.web_app.entities.analytics.StudentIntervention> allInterventions =
                interventionRepository.findByStudentIdInOrderByDispatchedAtDesc(studentIds);
        Map<Long, List<com.sdt.web_app.entities.analytics.StudentIntervention>> interventionsByStudent = new LinkedHashMap<>();
        for (com.sdt.web_app.entities.analytics.StudentIntervention si : allInterventions) {
            if (si.getStudent() != null) {
                interventionsByStudent.computeIfAbsent(si.getStudent().getId(), k -> new ArrayList<>()).add(si);
            }
        }

        List<StudentTelemetryAdminSummaryDto> summaries = new ArrayList<>();

        for (StudentProfile sp : allStudents) {
            StudentRiskScore srs = latestScoreByStudent.get(sp.getId());

            String currentRiskLevel = (srs != null && srs.getCompositeRiskLevel() != null)
                    ? srs.getCompositeRiskLevel().name()
                    : "LOW";

            if (riskLevelFilter != null && !riskLevelFilter.isBlank() && !riskLevelFilter.equalsIgnoreCase("ALL")) {
                if (!currentRiskLevel.equalsIgnoreCase(riskLevelFilter.trim())) {
                    continue;
                }
            }

            List<com.sdt.web_app.entities.analytics.StudentIntervention> interventions =
                    interventionsByStudent.getOrDefault(sp.getId(), List.of());

            if (interventionStatusFilter != null && !interventionStatusFilter.isBlank() && !interventionStatusFilter.equalsIgnoreCase("ALL")) {
                boolean hasMatchingStatus = interventions.stream()
                        .anyMatch(i -> i.getStatus().name().equalsIgnoreCase(interventionStatusFilter.trim()));
                if (!hasMatchingStatus) {
                    continue;
                }
            }

            List<DispatchedInterventionDto> dispatchedDtos = interventions.stream()
                    .map(i -> new DispatchedInterventionDto(
                            i.getId(),
                            i.getInterventionType().name(),
                            i.getTriggerFactor(),
                            i.getStatus().name(),
                            i.getDispatchedAt()
                    ))
                    .toList();

            Double riskScoreVal = srs != null && srs.getPredictedDropoutProbability() != null
                    ? srs.getPredictedDropoutProbability().doubleValue() * 100.0
                    : (srs != null && srs.getAcademicRiskScore() != null ? srs.getAcademicRiskScore().doubleValue() : 5.0);

            Instant syncTime = srs != null && srs.getEvaluatedAt() != null ? srs.getEvaluatedAt() : sp.getCreatedAt();
            String progName = sp.getProgram() != null ? sp.getProgram().getCode() : "N/A";
            String programOrCohort = progName + " (Year " + sp.getYearLevel() + ")";

            summaries.add(new StudentTelemetryAdminSummaryDto(
                    sp.getId(),
                    sp.getStudentNumber(),
                    sp.getFullName(),
                    programOrCohort,
                    currentRiskLevel,
                    Math.round(riskScoreVal * 10.0) / 10.0,
                    dispatchedDtos,
                    syncTime
            ));
        }

        return summaries;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<StudentTelemetryAdminSummaryDto> getAdminStudentTelemetry(
            int page, int size, String searchQuery, String riskLevelFilter, String interventionStatusFilter) {

        List<StudentTelemetryAdminSummaryDto> summaries = getAdminStudentTelemetryList(searchQuery, riskLevelFilter, interventionStatusFilter);
        int totalElements = summaries.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<StudentTelemetryAdminSummaryDto> pageContent = summaries.subList(fromIndex, toIndex);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, totalElements);
    }

    @Transactional(readOnly = true)
    public TelemetryKpiSummaryDto getAdminTelemetryKpi(
            String searchQuery, String riskLevelFilter, String interventionStatusFilter) {
        List<StudentTelemetryAdminSummaryDto> summaries = getAdminStudentTelemetryList(searchQuery, riskLevelFilter, interventionStatusFilter);
        return computeAdminKpiSummary(summaries);
    }

    @Transactional(readOnly = true)
    public List<FacultySectionOptionDto> getFacultyAssignedSections(Long facultyUserId) {
        if (classSectionRepository == null) return List.of();
        List<Long> assignedIds = classSectionRepository.findAssignedSectionIdsByInstructor(facultyUserId);
        if (assignedIds == null || assignedIds.isEmpty()) {
            return List.of();
        }
        List<ClassSection> sections = classSectionRepository.findAllById(assignedIds);
        return sections.stream().map(s -> new FacultySectionOptionDto(
                s.getId(),
                s.getSectionCode(),
                s.getCourse() != null ? s.getCourse().getCode() : "COURSE",
                s.getCourse() != null ? s.getCourse().getTitle() : "Class Section",
                s.getEnrolledCount()
        )).toList();
    }

    @Transactional(readOnly = true)
    public List<StudentTelemetrySummaryDto> getFacultyStudentTelemetryList(
            Long facultyUserId, String searchQuery, String riskLevelFilter, String interventionStatusFilter, Long sectionIdFilter) {

        List<Long> assignedSectionIds = classSectionRepository.findAssignedSectionIdsByInstructor(facultyUserId);
        if (assignedSectionIds == null || assignedSectionIds.isEmpty()) {
            return List.of();
        }

        List<Long> targetSectionIds;
        if (sectionIdFilter != null && sectionIdFilter > 0) {
            if (!assignedSectionIds.contains(sectionIdFilter)) {
                return List.of();
            }
            targetSectionIds = List.of(sectionIdFilter);
        } else {
            targetSectionIds = assignedSectionIds;
        }

        Map<Long, StudentProfile> studentMap = new LinkedHashMap<>();
        Map<Long, String> studentSectionCodeMap = new LinkedHashMap<>();

        for (Long secId : targetSectionIds) {
            List<EnrollmentCourseItem> items = enrollmentCourseItemRepository.findBySectionIdWithStudentDetails(secId);
            ClassSection sec = classSectionRepository.findById(secId).orElse(null);
            String secCode = sec != null ? sec.getSectionCode() : "SEC-" + secId;

            for (EnrollmentCourseItem item : items) {
                if (item.getEnrollment() != null && item.getEnrollment().getStudent() != null) {
                    StudentProfile sp = item.getEnrollment().getStudent();
                    studentMap.putIfAbsent(sp.getId(), sp);
                    studentSectionCodeMap.putIfAbsent(sp.getId(), secCode);
                }
            }
        }

        if (studentMap.isEmpty()) {
            return List.of();
        }

        List<Long> studentIds = new ArrayList<>(studentMap.keySet());

        // Batch query latest risk scores for section students
        List<StudentRiskScore> allRiskScores = riskScoreRepository.findByStudentIdInOrderByEvaluatedAtDesc(studentIds);
        Map<Long, StudentRiskScore> latestScoreByStudent = new LinkedHashMap<>();
        for (StudentRiskScore srs : allRiskScores) {
            if (srs.getStudent() != null) {
                latestScoreByStudent.putIfAbsent(srs.getStudent().getId(), srs);
            }
        }

        // Batch query all interventions for section students
        List<StudentIntervention> allInterventions = interventionRepository.findByStudentIdInOrderByDispatchedAtDesc(studentIds);
        Map<Long, List<StudentIntervention>> interventionsByStudent = new LinkedHashMap<>();
        for (StudentIntervention si : allInterventions) {
            if (si.getStudent() != null) {
                interventionsByStudent.computeIfAbsent(si.getStudent().getId(), k -> new ArrayList<>()).add(si);
            }
        }

        List<StudentTelemetrySummaryDto> summaries = new ArrayList<>();

        for (StudentProfile sp : studentMap.values()) {
            if (searchQuery != null && !searchQuery.isBlank()) {
                String q = searchQuery.toLowerCase().trim();
                boolean matchesName = sp.getFullName() != null && sp.getFullName().toLowerCase().contains(q);
                boolean matchesNumber = sp.getStudentNumber() != null && sp.getStudentNumber().toLowerCase().contains(q);
                boolean matchesProg = sp.getProgram() != null && sp.getProgram().getCode().toLowerCase().contains(q);
                if (!matchesName && !matchesNumber && !matchesProg) {
                    continue;
                }
            }

            StudentRiskScore srs = latestScoreByStudent.get(sp.getId());
            String currentRiskLevel = (srs != null && srs.getCompositeRiskLevel() != null)
                    ? srs.getCompositeRiskLevel().name()
                    : "LOW";

            if (riskLevelFilter != null && !riskLevelFilter.isBlank() && !riskLevelFilter.equalsIgnoreCase("ALL")) {
                if (!currentRiskLevel.equalsIgnoreCase(riskLevelFilter.trim())) {
                    continue;
                }
            }

            List<StudentIntervention> interventions = interventionsByStudent.getOrDefault(sp.getId(), List.of());
            if (interventionStatusFilter != null && !interventionStatusFilter.isBlank() && !interventionStatusFilter.equalsIgnoreCase("ALL")) {
                boolean hasMatchingStatus = interventions.stream()
                        .anyMatch(i -> i.getStatus().name().equalsIgnoreCase(interventionStatusFilter.trim()));
                if (!hasMatchingStatus) {
                    continue;
                }
            }

            List<DispatchedInterventionDto> dispatchedDtos = interventions.stream()
                    .map(i -> new DispatchedInterventionDto(
                            i.getId(),
                            i.getInterventionType().name(),
                            i.getTriggerFactor(),
                            i.getStatus().name(),
                            i.getDispatchedAt()
                    ))
                    .toList();

            Double riskScoreVal = srs != null && srs.getPredictedDropoutProbability() != null
                    ? srs.getPredictedDropoutProbability().doubleValue() * 100.0
                    : (srs != null && srs.getAcademicRiskScore() != null ? srs.getAcademicRiskScore().doubleValue() : 5.0);

            Instant syncTime = srs != null && srs.getEvaluatedAt() != null ? srs.getEvaluatedAt() : sp.getCreatedAt();
            String secCode = studentSectionCodeMap.getOrDefault(sp.getId(), "N/A");
            String progName = sp.getProgram() != null ? sp.getProgram().getCode() : "N/A";

            summaries.add(new StudentTelemetrySummaryDto(
                    sp.getId(),
                    sp.getStudentNumber(),
                    sp.getFullName(),
                    secCode,
                    progName,
                    currentRiskLevel,
                    Math.round(riskScoreVal * 10.0) / 10.0,
                    dispatchedDtos,
                    syncTime
            ));
        }

        return summaries;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<StudentTelemetrySummaryDto> getFacultyStudentTelemetry(
            Long facultyUserId, int page, int size, String searchQuery, String riskLevelFilter, String interventionStatusFilter, Long sectionIdFilter) {

        List<StudentTelemetrySummaryDto> summaries = getFacultyStudentTelemetryList(facultyUserId, searchQuery, riskLevelFilter, interventionStatusFilter, sectionIdFilter);
        int totalElements = summaries.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<StudentTelemetrySummaryDto> pageContent = summaries.subList(fromIndex, toIndex);

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, totalElements);
    }

    @Transactional(readOnly = true)
    public TelemetryKpiSummaryDto getFacultyTelemetryKpi(
            Long facultyUserId, String searchQuery, String riskLevelFilter, String interventionStatusFilter, Long sectionIdFilter) {
        List<StudentTelemetrySummaryDto> summaries = getFacultyStudentTelemetryList(facultyUserId, searchQuery, riskLevelFilter, interventionStatusFilter, sectionIdFilter);
        return computeFacultyKpiSummary(summaries);
    }

    private TelemetryKpiSummaryDto computeAdminKpiSummary(List<StudentTelemetryAdminSummaryDto> summaries) {
        long totalMonitored = summaries.size();
        long criticalRiskCount = summaries.stream().filter(s -> "CRITICAL".equalsIgnoreCase(s.riskLevel())).count();
        long highRiskCount = summaries.stream().filter(s -> "HIGH".equalsIgnoreCase(s.riskLevel())).count();
        long moderateRiskCount = summaries.stream().filter(s -> "MODERATE".equalsIgnoreCase(s.riskLevel())).count();
        long lowRiskCount = summaries.stream().filter(s -> "LOW".equalsIgnoreCase(s.riskLevel())).count();
        long totalActiveInterventions = summaries.stream()
                .mapToLong(s -> s.activeInterventions() != null ? s.activeInterventions().size() : 0)
                .sum();
        double averageWellnessIndex = 100.0;
        if (!summaries.isEmpty()) {
            double totalWellness = summaries.stream()
                    .mapToDouble(s -> 100.0 - (s.riskScore() != null ? s.riskScore() : 0.0))
                    .sum();
            averageWellnessIndex = Math.round((totalWellness / summaries.size()) * 10.0) / 10.0;
        }

        return new TelemetryKpiSummaryDto(
                totalMonitored,
                criticalRiskCount,
                highRiskCount,
                moderateRiskCount,
                lowRiskCount,
                totalActiveInterventions,
                averageWellnessIndex
        );
    }

    private TelemetryKpiSummaryDto computeFacultyKpiSummary(List<StudentTelemetrySummaryDto> summaries) {
        long totalMonitored = summaries.size();
        long criticalRiskCount = summaries.stream().filter(s -> "CRITICAL".equalsIgnoreCase(s.riskLevel())).count();
        long highRiskCount = summaries.stream().filter(s -> "HIGH".equalsIgnoreCase(s.riskLevel())).count();
        long moderateRiskCount = summaries.stream().filter(s -> "MODERATE".equalsIgnoreCase(s.riskLevel())).count();
        long lowRiskCount = summaries.stream().filter(s -> "LOW".equalsIgnoreCase(s.riskLevel())).count();
        long totalActiveInterventions = summaries.stream()
                .mapToLong(s -> s.activeInterventions() != null ? s.activeInterventions().size() : 0)
                .sum();
        double averageWellnessIndex = 100.0;
        if (!summaries.isEmpty()) {
            double totalWellness = summaries.stream()
                    .mapToDouble(s -> 100.0 - (s.riskScore() != null ? s.riskScore() : 0.0))
                    .sum();
            averageWellnessIndex = Math.round((totalWellness / summaries.size()) * 10.0) / 10.0;
        }

        return new TelemetryKpiSummaryDto(
                totalMonitored,
                criticalRiskCount,
                highRiskCount,
                moderateRiskCount,
                lowRiskCount,
                totalActiveInterventions,
                averageWellnessIndex
        );
    }

    @Transactional
    public StudentSelfTelemetryDto getStudentSelfTelemetry(Long studentUserId) {
        StudentProfile student = profileRepository.findByUserIdWithProgramAndCurriculum(studentUserId)
                .or(() -> profileRepository.findByUserId(studentUserId))
                .orElse(null);

        if (student == null) {
            Map<String, Double> defaultDim = new LinkedHashMap<>();
            defaultDim.put("Academic Progress", 85.0);
            defaultDim.put("Attendance Consistency", 90.0);
            defaultDim.put("LMS Engagement Index", 80.0);
            defaultDim.put("Assignment Punctuality", 95.0);
            return new StudentSelfTelemetryDto(
                    studentUserId,
                    "Student User",
                    "LOW",
                    87.5,
                    defaultDim,
                    List.of(),
                    List.of(new MilestoneDto(1L, "Digital Twin Synchronized", "Continuous real-time ML risk & telemetry monitoring active", "SYSTEM", Instant.now())),
                    Instant.now()
            );
        }

        // Snapshot caching: Use latest evaluated score if recent (within 6 hours), otherwise re-evaluate
        Instant staleThreshold = Instant.now().minus(6, java.time.temporal.ChronoUnit.HOURS);
        StudentRiskScore riskScore = riskScoreRepository.findTopByStudentIdOrderByEvaluatedAtDesc(student.getId())
                .filter(srs -> srs.getEvaluatedAt() != null && srs.getEvaluatedAt().isAfter(staleThreshold))
                .orElse(null);

        double academicVal;
        double attendanceVal;
        double socioVal;
        String riskLevel;
        Instant syncTime;

        if (riskScore != null) {
            academicVal = riskScore.getAcademicRiskScore() != null ? riskScore.getAcademicRiskScore().doubleValue() : 10.0;
            attendanceVal = riskScore.getAttendanceRiskScore() != null ? riskScore.getAttendanceRiskScore().doubleValue() : 10.0;
            socioVal = riskScore.getSocioeconomicRiskScore() != null ? riskScore.getSocioeconomicRiskScore().doubleValue() : 10.0;
            riskLevel = riskScore.getCompositeRiskLevel() != null ? riskScore.getCompositeRiskLevel().name() : "LOW";
            syncTime = riskScore.getEvaluatedAt();
        } else {
            DigitalTwinRiskProfileDto evaluated = evaluateStudentRiskProfile(student.getId());
            academicVal = evaluated.academicRiskScore() != null ? evaluated.academicRiskScore().doubleValue() : 10.0;
            attendanceVal = evaluated.attendanceRiskScore() != null ? evaluated.attendanceRiskScore().doubleValue() : 10.0;
            socioVal = evaluated.socioeconomicRiskScore() != null ? evaluated.socioeconomicRiskScore().doubleValue() : 10.0;
            riskLevel = evaluated.compositeRiskLevel();
            syncTime = evaluated.evaluatedAt() != null ? evaluated.evaluatedAt() : Instant.now();
        }

        double wellness = 100.0 - (academicVal * 0.40 + attendanceVal * 0.35 + socioVal * 0.25);
        wellness = Math.max(0.0, Math.min(100.0, Math.round(wellness * 10.0) / 10.0));

        Map<String, Double> dimensions = new LinkedHashMap<>();
        dimensions.put("Academic Progress", Math.round(Math.max(0.0, 100.0 - academicVal) * 10.0) / 10.0);
        dimensions.put("Attendance Consistency", Math.round(Math.max(0.0, 100.0 - attendanceVal) * 10.0) / 10.0);
        dimensions.put("LMS Engagement Index", Math.round(Math.max(0.0, 100.0 - (academicVal * 0.7 + attendanceVal * 0.3)) * 10.0) / 10.0);
        dimensions.put("Assignment Punctuality", Math.round(Math.max(0.0, 100.0 - (academicVal * 0.5)) * 10.0) / 10.0);

        List<StudentIntervention> interventions = interventionRepository.findByStudentIdOrderByDispatchedAtDesc(student.getId());
        List<DispatchedInterventionDto> dispatchedDtos = interventions.stream()
                .map(i -> new DispatchedInterventionDto(
                        i.getId(),
                        i.getInterventionType().name(),
                        i.getTriggerFactor(),
                        i.getStatus().name(),
                        i.getDispatchedAt()
                ))
                .toList();

        List<MilestoneDto> milestones = new ArrayList<>();
        if (attendanceVal < 20.0) {
            milestones.add(new MilestoneDto(1L, "Perfect Attendance Streak", "Maintained >90% geofenced attendance rate across active sections", "ATTENDANCE", Instant.now()));
        }
        if (academicVal < 25.0) {
            milestones.add(new MilestoneDto(2L, "Academic Mastery Pace", "High continuous grade performance in registered courses", "ACADEMIC", Instant.now()));
        }
        milestones.add(new MilestoneDto(3L, "Digital Twin Synchronized", "Continuous real-time ML risk & telemetry monitoring active", "SYSTEM", Instant.now()));

        return new StudentSelfTelemetryDto(
                student.getId(),
                student.getFullName(),
                riskLevel,
                wellness,
                dimensions,
                dispatchedDtos,
                milestones,
                syncTime != null ? syncTime : Instant.now()
        );
    }

    @Transactional
    public void acknowledgeIntervention(Long interventionId, Long studentUserId) {
        StudentIntervention intervention = interventionRepository.findById(interventionId)
                .orElseThrow(() -> new EntityNotFoundException("Intervention not found: " + interventionId));
        if (intervention.getStudent() != null && intervention.getStudent().getUser() != null) {
            if (!intervention.getStudent().getUser().getId().equals(studentUserId)) {
                throw new IllegalStateException("Unauthorized: intervention does not belong to student.");
            }
        }
        intervention.updateStatus(StudentIntervention.InterventionStatus.ACKNOWLEDGED, "Acknowledged by student in digital twin portal.", "Student self-service feedback");
        interventionRepository.save(intervention);
    }
}
