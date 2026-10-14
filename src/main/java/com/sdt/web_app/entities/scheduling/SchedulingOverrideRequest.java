package com.sdt.web_app.entities.scheduling;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.faculty.FacultyProfile;
import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.Program;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "scheduling_override_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"section", "schedule", "facultyUser", "facultyProfile", "requestedByUser", "homeProgram", "homeCollege", "targetProgram", "targetCollege", "approvals"})
@BatchSize(size = 50)
public class SchedulingOverrideRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private ClassSection section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private ClassSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_user_id", nullable = false)
    private User facultyUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_profile_id")
    private FacultyProfile facultyProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "override_type", nullable = false, length = 30)
    private OverrideType overrideType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_program_id")
    private Program homeProgram;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_college_id")
    private Department homeCollege;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_program_id", nullable = false)
    private Program targetProgram;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_college_id", nullable = false)
    private Department targetCollege;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private OverrideRequestStatus status = OverrideRequestStatus.PENDING_APPROVAL;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justification;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stepOrder ASC")
    @BatchSize(size = 50)
    @Builder.Default
    private List<SchedulingOverrideApproval> approvals = new ArrayList<>();

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addApproval(SchedulingOverrideApproval approval) {
        approvals.add(approval);
        approval.setRequest(this);
    }

    public void updateStatus(OverrideRequestStatus newStatus, String notes) {
        this.status = newStatus;
        if (notes != null && !notes.isBlank()) {
            this.resolutionNotes = notes;
        }
        if (newStatus == OverrideRequestStatus.APPROVED
                || newStatus == OverrideRequestStatus.REJECTED
                || newStatus == OverrideRequestStatus.CANCELLED
                || newStatus == OverrideRequestStatus.SUPERSEDED) {
            this.completedAt = Instant.now();
        }
    }

    public void setSchedule(ClassSchedule schedule) {
        this.schedule = schedule;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SchedulingOverrideRequest that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
