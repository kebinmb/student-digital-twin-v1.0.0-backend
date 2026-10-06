package com.sdt.web_app.service.lms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class StudentNotificationPublisherService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    private final com.sdt.web_app.repositories.enrollment.StudentProfileRepository studentProfileRepository;
    private final com.sdt.web_app.service.push.WebPushSubscriptionService webPushService;

    // Map of studentProfileId -> list of active SseEmitters
    private final Map<Long, List<SseEmitter>> studentEmitters = new ConcurrentHashMap<>();

    public StudentNotificationPublisherService() {
        this.studentProfileRepository = null;
        this.webPushService = null;
    }

    public StudentNotificationPublisherService(
            com.sdt.web_app.repositories.enrollment.StudentProfileRepository studentProfileRepository,
            com.sdt.web_app.service.push.WebPushSubscriptionService webPushService) {
        this.studentProfileRepository = studentProfileRepository;
        this.webPushService = webPushService;
    }

    public SseEmitter subscribeToStudentEvents(Long studentId) {
        if (studentId == null || studentId <= 0) {
            throw new IllegalArgumentException("Valid studentId is required for event streaming subscription.");
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        studentEmitters.computeIfAbsent(studentId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(studentId, emitter));
        emitter.onTimeout(() -> removeEmitter(studentId, emitter));
        emitter.onError(e -> removeEmitter(studentId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data(Map.of(
                            "status", "CONNECTED",
                            "studentId", studentId,
                            "timestamp", Instant.now().toString()
                    )));
        } catch (IOException e) {
            removeEmitter(studentId, emitter);
        }

        return emitter;
    }

    private void removeEmitter(Long studentId, SseEmitter emitter) {
        List<SseEmitter> list = studentEmitters.get(studentId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                studentEmitters.remove(studentId);
            }
        }
    }

    public void publishGradeReleasedEvent(Long studentId, Long sectionId, String courseCode, String courseTitle, Double grade, String status) {
        if (studentId == null) return;
        Map<String, Object> data = Map.of(
                "eventType", "GRADE_RELEASED",
                "studentId", studentId,
                "sectionId", sectionId != null ? sectionId : 0L,
                "courseCode", courseCode != null ? courseCode : "",
                "courseTitle", courseTitle != null ? courseTitle : "",
                "grade", grade != null ? grade : 0.0,
                "status", status != null ? status : "SEALED",
                "timestamp", Instant.now().toString()
        );
        sendToStudent(studentId, "grade-released", data);
        triggerPushIfConfigured(studentId, "GRADES", "Official Grade Sealed",
                courseCode + " - Final Grade: " + grade + " (" + status + ")", "/dashboard/portal/student");
    }

    public void publishClearanceUpdatedEvent(Long studentId, Long termId, String departmentType, String signoffStatus, String overallStatus, String remarks) {
        if (studentId == null) return;
        Map<String, Object> data = Map.of(
                "eventType", "CLEARANCE_UPDATED",
                "studentId", studentId,
                "termId", termId != null ? termId : 0L,
                "departmentType", departmentType != null ? departmentType : "",
                "signoffStatus", signoffStatus != null ? signoffStatus : "",
                "overallStatus", overallStatus != null ? overallStatus : "",
                "remarks", remarks != null ? remarks : "",
                "timestamp", Instant.now().toString()
        );
        sendToStudent(studentId, "clearance-updated", data);
        triggerPushIfConfigured(studentId, "CLEARANCE", "Clearance Updated",
                departmentType + ": " + signoffStatus + " (" + overallStatus + ")", "/dashboard/portal/student");
    }

    public void publishStandingUpdatedEvent(Long studentId, Double gpa, BigDecimal totalUnitsEarned, String academicStatus) {
        if (studentId == null) return;
        Map<String, Object> data = Map.of(
                "eventType", "STANDING_UPDATED",
                "studentId", studentId,
                "gpa", gpa != null ? gpa : 0.0,
                "totalUnitsEarned", totalUnitsEarned != null ? totalUnitsEarned : BigDecimal.ZERO,
                "academicStatus", academicStatus != null ? academicStatus : "REGULAR",
                "timestamp", Instant.now().toString()
        );
        sendToStudent(studentId, "standing-updated", data);
        triggerPushIfConfigured(studentId, "HONORS", "Academic Standing Updated",
                "Term GPA: " + String.format("%.2f", gpa) + " — " + academicStatus, "/dashboard/portal/student");
    }

    public void publishEnrollmentUpdatedEvent(Long studentId, Long termId, Long sectionId, String action, String courseCode) {
        if (studentId == null) return;
        Map<String, Object> data = Map.of(
                "eventType", "ENROLLMENT_UPDATED",
                "studentId", studentId,
                "termId", termId != null ? termId : 0L,
                "sectionId", sectionId != null ? sectionId : 0L,
                "action", action != null ? action : "ENLISTED",
                "courseCode", courseCode != null ? courseCode : "",
                "timestamp", Instant.now().toString()
        );
        sendToStudent(studentId, "enrollment-updated", data);
    }

    public void publishAttendanceVerifiedEvent(Long studentId, Long sessionId, String courseCode, String sectionCode, String status) {
        if (studentId == null) return;
        Map<String, Object> data = Map.of(
                "eventType", "ATTENDANCE_VERIFIED",
                "studentId", studentId,
                "sessionId", sessionId != null ? sessionId : 0L,
                "courseCode", courseCode != null ? courseCode : "",
                "sectionCode", sectionCode != null ? sectionCode : "",
                "status", status != null ? status : "PRESENT",
                "timestamp", Instant.now().toString()
        );
        sendToStudent(studentId, "attendance-verified", data);
        triggerPushIfConfigured(studentId, "ATTENDANCE", "Attendance Verified",
                courseCode + " (" + sectionCode + ") marked " + status, "/dashboard/analytics/qr-attendance");
    }

    public void publishInterventionDispatchedEvent(Long studentId, String interventionType, String riskLevel, String triggerFactor) {
        if (studentId == null) return;
        Map<String, Object> data = Map.of(
                "eventType", "INTERVENTION_DISPATCHED",
                "studentId", studentId,
                "interventionType", interventionType != null ? interventionType : "ACADEMIC_TUTORING",
                "riskLevel", riskLevel != null ? riskLevel : "MODERATE",
                "triggerFactor", triggerFactor != null ? triggerFactor : "",
                "timestamp", Instant.now().toString()
        );
        sendToStudent(studentId, "intervention-dispatched", data);
        triggerPushIfConfigured(studentId, "ATTENDANCE", "Academic Support Notice",
                "Support assigned: " + interventionType + " (" + riskLevel + " priority)", "/dashboard/portal/student");
    }

    private void triggerPushIfConfigured(Long studentId, String category, String title, String body, String url) {
        if (studentId == null || webPushService == null || studentProfileRepository == null) return;
        try {
            studentProfileRepository.findById(studentId).ifPresent(profile -> {
                if (profile.getUser() != null) {
                    webPushService.dispatchPushToUser(profile.getUser().getId(), category, title, body, url);
                }
            });
        } catch (Exception e) {
            log.debug("Push alert bypass for student {}: {}", studentId, e.getMessage());
        }
    }

    public void dispatchLocally(Long studentId, String eventName, Object data) {
        sendToStudent(studentId, eventName, data);
    }

    private void sendToStudent(Long studentId, String eventName, Object data) {
        List<SseEmitter> list = studentEmitters.get(studentId);
        if (list == null || list.isEmpty()) return;

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        if (!deadEmitters.isEmpty()) {
            list.removeAll(deadEmitters);
            if (list.isEmpty()) {
                studentEmitters.remove(studentId);
            }
        }
    }
}
