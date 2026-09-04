package com.sdt.web_app.dto.institution;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public class CurriculumDesignerDtos {

    public record DesignerViewResponse(
            Long curriculumId,
            String code,
            String name,
            String status,
            BigDecimal totalUnits,
            int totalContactHours,
            List<YearBlockDto> yearBlocks
    ) {
    }

    public record YearBlockDto(
            int yearLevel,
            List<SemesterBlockDto> semesters
    ) {
    }

    public record SemesterBlockDto(
            String semester,
            BigDecimal totalUnits,
            int totalContactHours,
            List<CourseItemDto> courses
    ) {
    }

    public record CourseItemDto(
            Long curriculumCourseId,
            Long courseId,
            String code,
            String title,
            BigDecimal lectureUnits,
            BigDecimal labUnits,
            BigDecimal creditUnits,
            int contactHoursLec,
            int contactHoursLab,
            String category,
            int sequenceOrder,
            List<String> prerequisites
    ) {
    }

    public record RelocateCourseRequest(
            @NotNull(message = "CurriculumCourse ID is required")
            Long curriculumCourseId,

            @Min(value = 1, message = "Target year level must be at least 1")
            @Max(value = 6, message = "Target year level must not exceed 6")
            int targetYearLevel,

            @NotBlank(message = "Target semester is required")
            @Pattern(regexp = "^(1ST_SEM|2ND_SEM|SUMMER)$", message = "Target semester must be 1ST_SEM, 2ND_SEM, or SUMMER")
            String targetSemester,

            @Min(value = 1, message = "Target sequence order must be at least 1")
            int targetSequenceOrder
    ) {
    }

    public record AddPrerequisiteRequest(
            @NotNull(message = "Course ID is required")
            Long courseId,

            @NotNull(message = "Prerequisite Course ID is required")
            Long prerequisiteCourseId,

            @Pattern(regexp = "^(HARD|CO_REQUISITE|STANDING)$", message = "Rule type must be HARD, CO_REQUISITE, or STANDING")
            String ruleType,

            String minGradeRequired
    ) {
    }

    public record ValidationReportDto(
            boolean valid,
            ValidationSummary summary,
            List<DiagnosticMessage> errors,
            List<DiagnosticMessage> warnings
    ) {
    }

    public record ValidationSummary(
            BigDecimal totalUnits,
            int requiredUnits,
            BigDecimal unitDeficit
    ) {
    }

    public record DiagnosticMessage(
            String code,
            String severity,
            String message,
            String targetCourseCode,
            Integer yearLevel,
            String semester
    ) {
    }

    public record CreateCurriculumRequest(
            @NotNull(message = "Program ID is required")
            Long programId,

            @NotBlank(message = "Curriculum code is required")
            @Size(max = 30, message = "Curriculum code must not exceed 30 characters")
            String code,

            @NotBlank(message = "Curriculum name is required")
            @Size(max = 150, message = "Curriculum name must not exceed 150 characters")
            String name,

            @NotBlank(message = "Effective academic year is required")
            @Size(max = 20, message = "Effective academic year must not exceed 20 characters")
            String effectiveAcademicYear
    ) {
    }

    public record AddCourseToCurriculumRequest(
            @NotNull(message = "Course ID is required")
            Long courseId,

            @Min(value = 1, message = "Year level must be at least 1")
            @Max(value = 6, message = "Year level must not exceed 6")
            int yearLevel,

            @NotBlank(message = "Semester is required")
            @Pattern(regexp = "^(1ST_SEM|2ND_SEM|SUMMER)$", message = "Semester must be 1ST_SEM, 2ND_SEM, or SUMMER")
            String semester,

            @NotBlank(message = "Category is required")
            @Pattern(regexp = "^(GEN_ED|PROFESSIONAL_MAJOR|ELECTIVE|CAPSTONE|PRACTICUM|MANDATED)$", message = "Category must be GEN_ED, PROFESSIONAL_MAJOR, ELECTIVE, CAPSTONE, PRACTICUM, or MANDATED")
            String category,

            Integer sequenceOrder
    ) {
    }

    public record CloneCurriculumRequest(
            @NotBlank(message = "New curriculum code is required")
            @Size(max = 30, message = "New curriculum code must not exceed 30 characters")
            String newCode,

            @NotBlank(message = "New curriculum name is required")
            @Size(max = 150, message = "New curriculum name must not exceed 150 characters")
            String newName,

            @NotBlank(message = "Effective academic year is required")
            @Size(max = 20, message = "Effective academic year must not exceed 20 characters")
            String effectiveAcademicYear
    ) {
    }

    public record CurriculumSummaryResponse(
            Long id,
            String code,
            String name,
            String programCode,
            String effectiveAcademicYear,
            String status,
            int versionNumber
    ) {
    }

    public record CurriculumLookupOption(
            Long id,
            String code,
            String name,
            String programCode,
            String effectiveAcademicYear,
            String status,
            int versionNumber
    ) {
    }

    public record AvailableCourseDto(
            Long courseId,
            String code,
            String title,
            BigDecimal lectureUnits,
            BigDecimal labUnits,
            BigDecimal creditUnits,
            int contactHoursLec,
            int contactHoursLab,
            String category
    ) {
        public AvailableCourseDto(
                Long courseId,
                String code,
                String title,
                BigDecimal lectureUnits,
                BigDecimal labUnits,
                BigDecimal creditUnits,
                int contactHoursLec,
                int contactHoursLab
        ) {
            this(courseId, code, title, lectureUnits, labUnits, creditUnits, contactHoursLec, contactHoursLab, "PROFESSIONAL_MAJOR");
        }
    }
}