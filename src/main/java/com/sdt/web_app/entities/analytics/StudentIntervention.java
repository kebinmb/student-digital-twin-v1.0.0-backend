package com.sdt.web_app.entities.analytics;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "student_interventions", indexes = {
    @Index(name = "idx_interventions_student", columnList = "student_profile_id"),
    @Index(name = "idx_interventions_status", columnList = "status"),
    @Index(name = "idx_interventions_counselor", columnList = "assigned_counselor_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StudentIntervention {

    public enum InterventionType {
        ACADEMIC_TUTORING,
        ATTENDANCE_CONFERENCE,
        FINANCIAL_SUBSIDY_AID,
        GUIDANCE_COUNSELING,
        PEER_MENTORING
    }

    public enum InterventionStatus {
        OPEN,
        ASSIGNED,
        IN_PROGRESS,
        RESOLVED,
        ESCALATED,
        PENDING,
        DISPATCHED,
        ACKNOWLEDGED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "risk_score_id")
    private StudentRiskScore riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "intervention_type", nullable = false, length = 50)
    private InterventionType interventionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private InterventionStatus status = InterventionStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_counselor_id")
    private User assignedCounselor;

    @Column(name = "trigger_factor", nullable = false)
    private String triggerFactor;

    @Column(name = "case_notes", columnDefinition = "TEXT")
    private String caseNotes;

    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;

    @Column(name = "dispatched_at", updatable = false)
    @Builder.Default
    private Instant dispatchedAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public void assignCounselor(User counselor) {
        this.assignedCounselor = counselor;
        if (this.status == InterventionStatus.OPEN) {
            this.status = InterventionStatus.ASSIGNED;
        }
    }

    public void updateStatus(InterventionStatus newStatus, String resolutionSummary, String additionalNotes) {
        this.status = newStatus;
        if (resolutionSummary != null && !resolutionSummary.isBlank()) {
            this.resolutionSummary = resolutionSummary;
        }
        if (additionalNotes != null && !additionalNotes.isBlank()) {
            if (this.caseNotes == null) {
                this.caseNotes = additionalNotes;
            } else {
                this.caseNotes = this.caseNotes + "\n[" + Instant.now() + "] " + additionalNotes;
            }
        }
        if (newStatus == InterventionStatus.RESOLVED) {
            this.resolvedAt = Instant.now();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentIntervention that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
