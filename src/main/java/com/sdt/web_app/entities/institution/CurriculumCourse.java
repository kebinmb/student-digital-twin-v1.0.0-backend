package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(
        name = "curriculum_courses",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_curriculum_course", columnNames = {"curriculum_id", "course_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"curriculum", "course"})
public class CurriculumCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curriculum_id", nullable = false)
    private Curriculum curriculum;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "year_level", nullable = false)
    private int yearLevel;

    @Column(nullable = false, length = 20)
    private String semester;

    public void updateCurriculumPosition(int yearLevel, String semester) {
        if (yearLevel < 1 || yearLevel > 6) {
            throw new IllegalArgumentException("Invalid year level: " + yearLevel);
        }
        if (semester == null || semester.isBlank()) {
            throw new IllegalArgumentException("Semester cannot be blank");
        }
        this.yearLevel = yearLevel;
        this.semester = semester;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CurriculumCourse that)) return false;
        String thisCurrCode = curriculum != null ? curriculum.getCode() : null;
        String thatCurrCode = that.curriculum != null ? that.curriculum.getCode() : null;
        String thisCourseCode = course != null ? course.getCode() : null;
        String thatCourseCode = that.course != null ? that.course.getCode() : null;

        return Objects.equals(thisCurrCode, thatCurrCode) && Objects.equals(thisCourseCode, thatCourseCode);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
