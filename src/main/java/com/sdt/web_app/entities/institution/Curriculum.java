package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "curricula")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = "program")
public class Curriculum {

    public enum Status {DRAFT, UNDER_REVIEW, APPROVED, ACTIVE, ARCHIVED}

    private static final Map<Status, Set<Status>> VALID_TRANSITIONS = Map.of(
            Status.DRAFT, Set.of(Status.UNDER_REVIEW),
            Status.UNDER_REVIEW, Set.of(Status.DRAFT, Status.APPROVED),
            Status.APPROVED, Set.of(Status.DRAFT, Status.ACTIVE),
            Status.ACTIVE, Set.of(Status.ARCHIVED),
            Status.ARCHIVED, Set.of()
    );
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(unique = true, nullable = false, length = 30, updatable = false)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "effective_academic_year", nullable = false, length = 20)
    private String effectiveAcademicYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Version
    @Column(name = "version_number", nullable = false)
    @Builder.Default
    private int versionNumber = 1;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public void transitionTo(Status newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Target status cannot be null");
        }
        Set<Status> allowed = VALID_TRANSITIONS.getOrDefault(this.status, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(String.format(
                    "Invalid state transition: Cannot transition curriculum '%s' from %s to %s. Allowed targets: %s",
                    this.code, this.status, newStatus, allowed));
        }
        this.status = newStatus;
    }

    public boolean isEditable() {
        return this.status == Status.DRAFT || this.status == Status.UNDER_REVIEW;
    }

    public void updateDetails(String name, String effectiveAcademicYear) {
        if (!isEditable()) {
            throw new IllegalStateException("Curriculum is locked under status: " + this.status);
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Curriculum name cannot be blank");
        }
        if (effectiveAcademicYear == null || effectiveAcademicYear.isBlank()) {
            throw new IllegalArgumentException("Effective academic year cannot be blank");
        }
        this.name = name;
        this.effectiveAcademicYear = effectiveAcademicYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Curriculum that)) return false;
        return code != null && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}