package com.sdt.web_app.dto.institution;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class CurriculumCourseDtos {

    public record AssignCourseToCurriculumRequest(
            @NotNull(message = "Course ID is required")
            Long courseId,

            @Min(value = 1, message = "Year level must be between 1 and 6")
            @Max(value = 6, message = "Year level must be between 1 and 6")
            int yearLevel,

            @NotBlank(message = "Semester is required")
            @Pattern(regexp = "^(1ST_SEM|2ND_SEM|SUMMER)$", message = "Semester must be 1ST_SEM, 2ND_SEM, or SUMMER")
            String semester,

            @NotBlank(message = "Category is required")
            @Pattern(regexp = "^(GEN_ED|PROFESSIONAL_MAJOR|ELECTIVE|MANDATED)$", message = "Category must be GEN_ED, PROFESSIONAL_MAJOR, ELECTIVE, or MANDATED")
            String category,

            Integer sequenceOrder
    ) {
    }

    public record CurriculumCourseResponse(
            Long id,
            Long curriculumId,
            String curriculumCode,
            Long courseId,
            String courseCode,
            String courseTitle,
            BigDecimal creditUnits,
            int yearLevel,
            String semester,
            int sequenceOrder,
            String category
    ) {
    }
}
