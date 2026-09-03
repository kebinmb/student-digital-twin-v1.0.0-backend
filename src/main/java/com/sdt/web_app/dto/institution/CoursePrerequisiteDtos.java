package com.sdt.web_app.dto.institution;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CoursePrerequisiteDtos {

    public record CreateCoursePrerequisiteRequest(
            @NotNull(message = "Course ID is required")
            Long courseId,

            @NotNull(message = "Prerequisite Course ID is required")
            Long prerequisiteCourseId,

            @Pattern(regexp = "^(HARD|CO_REQUISITE|STANDING)$", message = "Rule type must be HARD, CO_REQUISITE, or STANDING")
            String ruleType,

            String minGradeRequired
    ) {
    }

    public record CoursePrerequisiteResponse(
            Long id,
            Long courseId,
            String courseCode,
            Long prerequisiteCourseId,
            String prerequisiteCourseCode,
            String prerequisiteCourseTitle,
            String ruleType,
            String minGradeRequired
    ) {
    }
}
