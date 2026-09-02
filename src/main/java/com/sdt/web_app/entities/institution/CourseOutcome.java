package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(
        name = "course_outcomes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_course_cilo", columnNames = {"course_id", "code"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = "course")
public class CourseOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "blooms_level", nullable = false, length = 30)
    private String bloomsLevel;

    public void updateOutcomeDetails(String description, String bloomsLevel) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Course outcome description cannot be blank");
        }
        if (bloomsLevel == null || bloomsLevel.isBlank()) {
            throw new IllegalArgumentException("Bloom's taxonomy level cannot be blank");
        }
        this.description = description;
        this.bloomsLevel = bloomsLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CourseOutcome that)) return false;
        String thisCourseCode = course != null ? course.getCode() : null;
        String thatCourseCode = that.course != null ? that.course.getCode() : null;

        return Objects.equals(code, that.code) && Objects.equals(thisCourseCode, thatCourseCode);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
