package com.sdt.web_app.service.analytics;

import com.sdt.web_app.dto.analytics.AnalyticsDtos.*;
import com.sdt.web_app.dto.analytics.AcknowledgeInterventionRequest;
import com.sdt.web_app.dto.common.SliceResponse;
import com.sdt.web_app.entities.analytics.StudentIntervention;
import com.sdt.web_app.entities.analytics.StudentRiskScore;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.analytics.StudentInterventionRepository;
import com.sdt.web_app.repositories.analytics.StudentRiskScoreRepository;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.service.security.StudentProfileL2CacheService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.sdt.web_app.config.WebSocketBroadcastService;
import com.sdt.web_app.config.WebSocketTopics;
import com.sdt.web_app.websocket.dto.PerformanceSummaryMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentInterventionService {

    private final StudentInterventionRepository interventionRepository;
    private final StudentRiskScoreRepository riskScoreRepository;
    private final UserRepository userRepository;
    private final StudentProfileL2CacheService studentProfileL2CacheService;
    private final WebSocketBroadcastService broadcastService;

    @Transactional
    public StudentInterventionDto dispatchIntervention(DispatchInterventionRequest request) {
        StudentProfile student = Optional.ofNullable(studentProfileL2CacheService.findById(request.studentId()))
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found: " + request.studentId()));

        StudentRiskScore riskScore = null;
        if (request.riskScoreId() != null) {
            riskScore = riskScoreRepository.findById(request.riskScoreId()).orElse(null);
        } else {
            // Find latest risk score for student if available
            riskScore = riskScoreRepository.findTopByStudentIdOrderByEvaluatedAtDesc(student.getId()).orElse(null);
        }

        User counselor = null;
        if (request.assignedCounselorId() != null) {
            counselor = userRepository.findById(request.assignedCounselorId())
                    .orElseThrow(() -> new EntityNotFoundException("Assigned counselor user not found: " + request.assignedCounselorId()));
        }

        StudentIntervention.InterventionType type;
        try {
            type = StudentIntervention.InterventionType.valueOf(request.interventionType().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            type = StudentIntervention.InterventionType.ACADEMIC_TUTORING;
        }

        String triggerFactor = request.triggerFactor() != null && !request.triggerFactor().isBlank()
                ? request.triggerFactor()
                : (riskScore != null ? "Composite Risk " + riskScore.getCompositeRiskLevel().name() : "Academic Early Warning Flag");

        StudentIntervention intervention = StudentIntervention.builder()
                .student(student)
                .riskScore(riskScore)
                .interventionType(type)
                .status(counselor != null ? StudentIntervention.InterventionStatus.ASSIGNED : StudentIntervention.InterventionStatus.OPEN)
                .assignedCounselor(counselor)
                .triggerFactor(triggerFactor)
                .caseNotes(request.notes())
                .build();

        StudentIntervention saved = interventionRepository.save(intervention);
        log.info("Student intervention #{} dispatched for student {} (Type: {}, Status: {})",
                saved.getId(), student.getStudentNumber(), saved.getInterventionType(), saved.getStatus());

        PerformanceSummaryMessage dispatchMsg = new PerformanceSummaryMessage(
                student.getId(),
                null,
                null,
                saved.getInterventionType().name(),
                saved.getStatus().name(),
                Instant.now()
        );
        broadcastService.broadcast(WebSocketTopics.performance(student.getId()), dispatchMsg);
        broadcastService.broadcast(WebSocketTopics.ADMIN_TELEMETRY, dispatchMsg);

        return mapToDto(saved);
    }

    @Transactional
    public StudentInterventionDto updateInterventionStatus(Long interventionId, UpdateInterventionStatusRequest request) {
        StudentIntervention intervention = interventionRepository.findById(interventionId)
                .orElseThrow(() -> new EntityNotFoundException("Student intervention not found: " + interventionId));

        StudentIntervention.InterventionStatus newStatus = StudentIntervention.InterventionStatus.valueOf(request.status().toUpperCase());
        intervention.updateStatus(newStatus, request.resolutionSummary(), request.additionalNotes());

        StudentIntervention saved = interventionRepository.save(intervention);
        log.info("Student intervention #{} status updated to {}", saved.getId(), saved.getStatus());

        if (saved.getStudent() != null) {
            PerformanceSummaryMessage updateMsg = new PerformanceSummaryMessage(
                    saved.getStudent().getId(),
                    null,
                    null,
                    saved.getInterventionType().name(),
                    saved.getStatus().name(),
                    Instant.now()
            );
            broadcastService.broadcast(WebSocketTopics.performance(saved.getStudent().getId()), updateMsg);
            broadcastService.broadcast(WebSocketTopics.ADMIN_TELEMETRY, updateMsg);
        }

        return mapToDto(saved);
    }

    @Transactional(readOnly = true)
    public SliceResponse<StudentInterventionDto> getInterventionsByStudentSlice(Long studentId, Pageable pageable) {
        Slice<StudentIntervention> slice = interventionRepository.findByStudentId(studentId, pageable);
        return SliceResponse.from(slice.map(this::mapToDto));
    }

    @Transactional(readOnly = true)
    public List<StudentInterventionDto> getInterventionsByStudent(Long studentId) {
        return interventionRepository.findByStudentIdOrderByDispatchedAtDesc(studentId).stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<StudentInterventionDto> getAllInterventions(String status, Pageable pageable) {
        StudentIntervention.InterventionStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = StudentIntervention.InterventionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return interventionRepository.findAllWithDetails(statusEnum, pageable).map(this::mapToDto);
    }

    private StudentInterventionDto mapToDto(StudentIntervention entity) {
        StudentProfile sp = entity.getStudent();
        String studentName = sp != null ? sp.getFullName() : "Student";
        String counselorName = entity.getAssignedCounselor() != null ? entity.getAssignedCounselor().getUsername() : null;

        return new StudentInterventionDto(
                entity.getId(),
                sp.getId(),
                sp.getStudentNumber(),
                studentName,
                entity.getRiskScore() != null ? entity.getRiskScore().getId() : null,
                entity.getInterventionType().name(),
                entity.getStatus().name(),
                entity.getAssignedCounselor() != null ? entity.getAssignedCounselor().getId() : null,
                counselorName,
                entity.getTriggerFactor(),
                entity.getCaseNotes(),
                entity.getResolutionSummary(),
                entity.getDispatchedAt(),
                entity.getResolvedAt()
        );
    }

    @Transactional
    public StudentInterventionDto acknowledgeIntervention(Long interventionId, Long studentUserId, AcknowledgeInterventionRequest request) {
        StudentIntervention intervention = interventionRepository.findById(interventionId)
                .orElseThrow(() -> new EntityNotFoundException("Student intervention not found: " + interventionId));

        if (studentUserId != null && intervention.getStudent() != null) {
            boolean matchesUser = intervention.getStudent().getUser() != null
                    && intervention.getStudent().getUser().getId().equals(studentUserId);
            boolean matchesStudentProfile = intervention.getStudent().getId() != null
                    && intervention.getStudent().getId().equals(studentUserId);

            if (!matchesUser && !matchesStudentProfile) {
                throw new IllegalStateException("Unauthorized: intervention does not belong to student.");
            }
        }

        if (intervention.getStatus() == StudentIntervention.InterventionStatus.ACKNOWLEDGED) {
            if (request != null && request.response() != null && !request.response().isBlank()) {
                intervention.updateStatus(StudentIntervention.InterventionStatus.ACKNOWLEDGED,
                        "Acknowledged by student in digital twin portal.",
                        request.response());
                interventionRepository.save(intervention);
            }
            return mapToDto(intervention);
        }

        String notes = (request != null && request.response() != null && !request.response().isBlank())
                ? request.response()
                : "Student self-service feedback";

        intervention.updateStatus(StudentIntervention.InterventionStatus.ACKNOWLEDGED,
                "Acknowledged by student in digital twin portal.",
                notes);
        StudentIntervention saved = interventionRepository.save(intervention);

        if (saved.getStudent() != null) {
            PerformanceSummaryMessage updateMsg = new PerformanceSummaryMessage(
                    saved.getStudent().getId(),
                    null,
                    null,
                    saved.getInterventionType().name(),
                    saved.getStatus().name(),
                    Instant.now()
            );
            broadcastService.broadcast(WebSocketTopics.performance(saved.getStudent().getId()), updateMsg);
            broadcastService.broadcast(WebSocketTopics.ADMIN_TELEMETRY, updateMsg);
        }

        return mapToDto(saved);
    }
}
