package com.sdt.web_app.dto.institution;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CourseDtos {

    public record CreateCourseRequest(
            @NotBlank(message = "Course code is required")
            @Size(max = 30, message = "Code must not exceed 30 characters")
            String code,

            @NotBlank(message = "Course title is required")
            @Size(max = 150, message = "Title must not exceed 150 characters")
            String title,

            @NotNull(message = "Lecture units cannot be null")
            @DecimalMin(value = "0.0", message = "Lecture units must be non-negative")
            BigDecimal lectureUnits,

            @NotNull(message = "Lab units cannot be null")
            @DecimalMin(value = "0.0", message = "Lab units must be non-negative")
            BigDecimal labUnits,

            @Min(value = 0, message = "Contact hours (lecture) must be non-negative")
            int contactHoursLec,

            @Min(value = 0, message = "Contact hours (lab) must be non-negative")
            int contactHoursLab,

            @NotBlank(message = "Course category is required")
            @Pattern(
                    regexp = "^(GEN_ED|PROFESSIONAL_MAJOR|ELECTIVE|CAPSTONE|PRACTICUM|MANDATED)$",
                    message = "Invalid course category"
            )
            String category,

            String description
    ) {
        public CreateCourseRequest(
                String code,
                String title,
                BigDecimal lectureUnits,
                BigDecimal labUnits,
                int contactHoursLec,
                int contactHoursLab,
                String description
        ) {
            this(code, title, lectureUnits, labUnits, contactHoursLec, contactHoursLab, "PROFESSIONAL_MAJOR", description);
        }
    }

    public record UpdateCourseRequest(
            @NotBlank(message = "Course title is required")
            @Size(max = 150, message = "Title must not exceed 150 characters")
            String title,

            @NotNull(message = "Lecture units cannot be null")
            @DecimalMin(value = "0.0", message = "Lecture units must be non-negative")
            BigDecimal lectureUnits,

            @NotNull(message = "Lab units cannot be null")
            @DecimalMin(value = "0.0", message = "Lab units must be non-negative")
            BigDecimal labUnits,

            @Min(value = 0, message = "Contact hours (lecture) must be non-negative")
            int contactHoursLec,

            @Min(value = 0, message = "Contact hours (lab) must be non-negative")
            int contactHoursLab,

            @NotBlank(message = "Course category is required")
            @Pattern(
                    regexp = "^(GEN_ED|PROFESSIONAL_MAJOR|ELECTIVE|CAPSTONE|PRACTICUM|MANDATED)$",
                    message = "Invalid course category"
            )
            String category,

            String description
    ) {
        public UpdateCourseRequest(
                String title,
                BigDecimal lectureUnits,
                BigDecimal labUnits,
                int contactHoursLec,
                int contactHoursLab,
                String description
        ) {
            this(title, lectureUnits, labUnits, contactHoursLec, contactHoursLab, "PROFESSIONAL_MAJOR", description);
        }
    }

    public record CourseResponse(
            Long id,
            String code,
            String title,
            BigDecimal lectureUnits,
            BigDecimal labUnits,
            BigDecimal creditUnits,
            int contactHoursLec,
            int contactHoursLab,
            String category,
            String description,
            boolean isActive
    ) {
    }
}
