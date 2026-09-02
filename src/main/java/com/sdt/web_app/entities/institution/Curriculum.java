package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "curricula")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = "program")
public class Curriculum {

    public enum Status { DRAFT, UNDER_REVIEW, APPROVED, ACTIVE, ARCHIVED }

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

    @Column(name = "version_number", nullable = false)
    @Builder.Default
    private int versionNumber = 1;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public void transitionTo(Status newStatus) {
        if (this.status == Status.ACTIVE && newStatus != Status.ARCHIVED) {
            throw new IllegalStateException("Active curriculum is locked and can only transition to ARCHIVED.");
        }
        this.status = newStatus;
    }

    public boolean isEditable() {
        return this.status == Status.DRAFT || this.status == Status.UNDER_REVIEW;
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