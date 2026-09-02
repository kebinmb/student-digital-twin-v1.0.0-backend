package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(
        name = "course_prerequisites",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_course_prereq", columnNames = {"course_id", "prerequisite_course_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"course", "prerequisiteCourse"})
public class CoursePrerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prerequisite_course_id", nullable = false)
    private Course prerequisiteCourse;

    @Column(name = "rule_type", nullable = false, length = 20)
    @Builder.Default
    private String ruleType = "HARD";

    @Column(name = "min_grade_required", nullable = false, length = 10)
    @Builder.Default
    private String minGradeRequired = "3.00";

    public void updateRule(String ruleType, String minGradeRequired) {
        if (ruleType == null || ruleType.isBlank()) {
            throw new IllegalArgumentException("Rule type cannot be blank");
        }
        if (minGradeRequired == null || minGradeRequired.isBlank()) {
            throw new IllegalArgumentException("Minimum grade required cannot be blank");
        }
        this.ruleType = ruleType;
        this.minGradeRequired = minGradeRequired;
    }

    public void updateRuleType(String ruleType) {
        if (ruleType == null || ruleType.isBlank()) {
            throw new IllegalArgumentException("Rule type cannot be blank");
        }
        this.ruleType = ruleType;
    }

    public void updateMinGradeRequired(String minGradeRequired) {
        if (minGradeRequired == null || minGradeRequired.isBlank()) {
            throw new IllegalArgumentException("Minimum grade required cannot be blank");
        }
        this.minGradeRequired = minGradeRequired;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CoursePrerequisite that)) return false;
        String thisCourseCode = course != null ? course.getCode() : null;
        String thatCourseCode = that.course != null ? that.course.getCode() : null;
        String thisPrereqCode = prerequisiteCourse != null ? prerequisiteCourse.getCode() : null;
        String thatPrereqCode = that.prerequisiteCourse != null ? that.prerequisiteCourse.getCode() : null;

        return Objects.equals(thisCourseCode, thatCourseCode) && Objects.equals(thisPrereqCode, thatPrereqCode);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}