package com.sdt.web_app.entities.scheduling;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Term;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "class_sections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"term", "curriculum", "course", "schedules", "primaryInstructor"})
public class ClassSection {

    public enum Status {
        PLANNED, OPEN, CLOSED, CANCELLED
    }

    public enum GradeStatus {
        DRAFT, SUBMITTED, VERIFIED, SEALED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curriculum_id", nullable = false)
    private Curriculum curriculum;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "section_code", nullable = false, length = 30)
    private String sectionCode;

    @Column(name = "max_capacity", nullable = false)
    @Builder.Default
    private int maxCapacity = 40;

    @Column(name = "enrolled_count", nullable = false)
    @Builder.Default
    private int enrolledCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PLANNED;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_status", nullable = false, length = 20)
    @Builder.Default
    private GradeStatus gradeStatus = GradeStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_instructor_id")
    private User primaryInstructor;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClassSchedule> schedules = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void updateStatus(Status newStatus) {
        this.status = newStatus;
    }

    public void updateGradeStatus(GradeStatus newGradeStatus) {
        this.gradeStatus = newGradeStatus;
    }

    public void setPrimaryInstructor(User instructor) {
        this.primaryInstructor = instructor;
    }

    public void addSchedule(ClassSchedule schedule) {
        schedules.add(schedule);
        schedule.setSection(this);
    }

    public void removeSchedule(ClassSchedule schedule) {
        schedules.remove(schedule);
        schedule.setSection(null);
    }

    public void incrementEnrolledCount() {
        if (this.enrolledCount >= this.maxCapacity) {
            throw new IllegalStateException("Cannot enroll: section " + this.sectionCode + " has reached maximum capacity (" + this.maxCapacity + ")");
        }
        this.enrolledCount++;
        if (this.enrolledCount == this.maxCapacity) {
            this.status = Status.CLOSED;
        }
    }

    public void decrementEnrolledCount() {
        if (this.enrolledCount > 0) {
            this.enrolledCount--;
            if (this.status == Status.CLOSED) {
                this.status = Status.OPEN;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassSection that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
