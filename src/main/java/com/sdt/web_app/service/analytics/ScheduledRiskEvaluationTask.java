package com.sdt.web_app.service.analytics;

import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledRiskEvaluationTask {

    private final StudentProfileRepository profileRepository;
    private final DigitalTwinRiskService riskService;

    /**
     * Automated nightly batch risk sweep at 02:00 AM (Asia/Manila time).
     * Recalculates risk scores and updates Early Warning Radar profiles for all enrolled students.
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Manila")
    public void runNightlyRiskSweep() {
        log.info("Initiating scheduled nightly Digital Twin Risk Sweep...");
        List<Long> studentIds = profileRepository.findAllStudentIds();
        if (studentIds.isEmpty()) {
            log.info("No student profiles registered for risk sweep.");
            return;
        }

        int evaluated = 0;
        int failures = 0;
        for (Long studentId : studentIds) {
            try {
                riskService.evaluateStudentRiskProfile(studentId);
                evaluated++;
            } catch (Exception e) {
                failures++;
                log.warn("Nightly risk evaluation skipped for student #{}: {}", studentId, e.getMessage());
            }
        }
        log.info("Completed nightly Digital Twin Risk Sweep: {} evaluated successfully, {} failures.", evaluated, failures);
    }
}
