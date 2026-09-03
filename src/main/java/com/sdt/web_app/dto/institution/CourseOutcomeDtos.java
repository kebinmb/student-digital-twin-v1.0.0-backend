package com.sdt.web_app.dto.institution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CourseOutcomeDtos {

    public record CreateCourseOutcomeRequest(
            @NotBlank(message = "Course outcome code is required")
            @Size(max = 30, message = "Code must not exceed 30 characters")
            String code,

            @NotBlank(message = "Outcome description is required")
            String description,

            @NotBlank(message = "Bloom's taxonomy level is required")
            @Size(max = 30, message = "Bloom's level must not exceed 30 characters")
            String bloomsLevel
    ) {
    }

    public record UpdateCourseOutcomeRequest(
            @NotBlank(message = "Outcome description is required")
            String description,

            @NotBlank(message = "Bloom's taxonomy level is required")
            @Size(max = 30, message = "Bloom's level must not exceed 30 characters")
            String bloomsLevel
    ) {
    }

    public record CourseOutcomeResponse(
            Long id,
            Long courseId,
            String courseCode,
            String code,
            String description,
            String bloomsLevel
    ) {
    }
}
