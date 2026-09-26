package com.sdt.web_app.service.analytics;

import com.sdt.web_app.events.analytics.AssessmentScoreChangedEvent;
import com.sdt.web_app.events.analytics.AttendanceScannedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StudentTelemetryEventListener {

    private final DigitalTwinRiskService riskService;

    @Async
    @EventListener
    public void onAttendanceScanned(AttendanceScannedEvent event) {
        if (event == null || event.studentId() == null) return;
        log.debug("Processing AttendanceScannedEvent for student #{}", event.studentId());
        try {
            riskService.evaluateStudentRiskProfile(event.studentId());
        } catch (Exception e) {
            log.warn("Asynchronous risk re-evaluation on attendance failed for student #{}: {}",
                    event.studentId(), e.getMessage());
        }
    }

    @Async
    @EventListener
    public void onAssessmentScoreChanged(AssessmentScoreChangedEvent event) {
        if (event == null || event.studentId() == null) return;
        log.debug("Processing AssessmentScoreChangedEvent for student #{}", event.studentId());
        try {
            riskService.evaluateStudentRiskProfile(event.studentId());
        } catch (Exception e) {
            log.warn("Asynchronous risk re-evaluation on assessment failed for student #{}: {}",
                    event.studentId(), e.getMessage());
        }
    }
}
