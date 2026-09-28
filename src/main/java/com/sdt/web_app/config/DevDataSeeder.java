package com.sdt.web_app.config;

import com.sdt.web_app.entities.analytics.StudentIntervention;
import com.sdt.web_app.entities.analytics.StudentRiskScore;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.analytics.StudentInterventionRepository;
import com.sdt.web_app.repositories.analytics.StudentRiskScoreRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.AcademicYearRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final StudentProfileRepository studentProfileRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TermRepository termRepository;
    private final StudentRiskScoreRepository studentRiskScoreRepository;
    private final StudentInterventionRepository studentInterventionRepository;

    @Override
    public void run(String... args) {
        log.info("--- [DEV PROFILE SEEDER VERIFICATION] ---");
        long studentCount = studentProfileRepository.count();
        long activeAyCount = academicYearRepository.findAllByIsCurrentTrue().size();
        long activeTermCount = termRepository.findAllByIsActiveTrue().size();
        long riskScoreCount = studentRiskScoreRepository.count();

        // Seed interventions if none exist
        if (studentInterventionRepository.count() == 0 && studentCount > 0) {
            List<StudentProfile> students = studentProfileRepository.findAll();
            for (int i = 0; i < students.size(); i++) {
                StudentProfile sp = students.get(i);
                StudentRiskScore.RiskLevel level = (i % 4 == 0) ? StudentRiskScore.RiskLevel.CRITICAL :
                                                  (i % 4 == 1) ? StudentRiskScore.RiskLevel.HIGH :
                                                  (i % 4 == 2) ? StudentRiskScore.RiskLevel.MODERATE :
                                                  StudentRiskScore.RiskLevel.LOW;

                final int index = i;
                StudentRiskScore srs = studentRiskScoreRepository.findTopByStudentIdOrderByEvaluatedAtDesc(sp.getId()).orElseGet(() -> 
                    studentRiskScoreRepository.save(StudentRiskScore.builder()
                        .student(sp)
                        .academicRiskScore(new BigDecimal(15 + index * 10))
                        .attendanceRiskScore(new BigDecimal(10 + index * 5))
                        .socioeconomicRiskScore(new BigDecimal(5 + index * 3))
                        .compositeRiskLevel(level)
                        .predictedDropoutProbability(new BigDecimal(level == StudentRiskScore.RiskLevel.CRITICAL ? "0.8500" :
                                                                    level == StudentRiskScore.RiskLevel.HIGH ? "0.5500" :
                                                                    level == StudentRiskScore.RiskLevel.MODERATE ? "0.3000" : "0.0500"))
                        .recommendedInterventions("Automated AI Telemetry Monitoring")
                        .evaluatedAt(Instant.now())
                        .build())
                );

                if (level == StudentRiskScore.RiskLevel.CRITICAL || level == StudentRiskScore.RiskLevel.HIGH) {
                    studentInterventionRepository.save(StudentIntervention.builder()
                            .student(sp)
                            .riskScore(srs)
                            .interventionType(StudentIntervention.InterventionType.GUIDANCE_COUNSELING)
                            .status(StudentIntervention.InterventionStatus.DISPATCHED)
                            .triggerFactor("High Dropout Risk Triggered: Academic Deficit & Attendance Velocity Drop")
                            .caseNotes("Automated tutoring recommendation dispatched to student portal.")
                            .dispatchedAt(Instant.now())
                            .build());
                }
            }
            log.info("Seeded AI Interventions across LOW, MODERATE, HIGH, and CRITICAL risk levels.");
        }

        log.info("Seeded Students: {}", studentCount);
        log.info("Active Academic Years: {}", activeAyCount);
        log.info("Active Terms: {}", activeTermCount);
        log.info("Digital Twin Risk Scores: {}", studentRiskScoreRepository.count());
        log.info("Dispatched Interventions: {}", studentInterventionRepository.count());
        log.info("--- [DEV PROFILE SEEDER VERIFICATION COMPLETE] ---");
    }
}
