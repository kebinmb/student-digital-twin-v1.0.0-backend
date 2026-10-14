package com.sdt.web_app.entities.scheduling;

import com.sdt.web_app.entities.authentication.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "scheduling_override_approvals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"request", "assignedApproverUser", "decisionByUser"})
@BatchSize(size = 50)
public class SchedulingOverrideApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    @Setter
    private SchedulingOverrideRequest request;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_type", nullable = false, length = 30)
    private ApprovalStepType approvalType;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "required_role", nullable = false, length = 30)
    private String requiredRole;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_approver_user_id")
    private User assignedApproverUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decision_by_user_id")
    private User decisionByUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApprovalStepStatus status = ApprovalStepStatus.PENDING;

    @Column(name = "decision_at")
    private Instant decisionAt;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void approve(User decisionMaker, String remarks) {
        this.status = ApprovalStepStatus.APPROVED;
        this.decisionByUser = decisionMaker;
        this.remarks = remarks;
        this.decisionAt = Instant.now();
    }

    public void reject(User decisionMaker, String remarks) {
        this.status = ApprovalStepStatus.REJECTED;
        this.decisionByUser = decisionMaker;
        this.remarks = remarks;
        this.decisionAt = Instant.now();
    }

    public void cancel(String remarks) {
        this.status = ApprovalStepStatus.CANCELLED;
        this.remarks = remarks;
        this.decisionAt = Instant.now();
    }

    public void skip(String remarks) {
        this.status = ApprovalStepStatus.SKIPPED;
        this.remarks = remarks;
        this.decisionAt = Instant.now();
    }

    public void setAssignedApproverUser(User user) {
        this.assignedApproverUser = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SchedulingOverrideApproval that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
