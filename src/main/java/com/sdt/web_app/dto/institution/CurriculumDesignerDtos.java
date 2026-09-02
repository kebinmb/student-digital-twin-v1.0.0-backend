package com.sdt.web_app.dto.institution;

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
            Long curriculumCourseId,
            int targetYearLevel,
            String targetSemester,
            int targetSequenceOrder
    ) {
    }

    public record AddPrerequisiteRequest(
            Long courseId,
            Long prerequisiteCourseId,
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
            Long programId,
            String code,
            String name,
            String effectiveAcademicYear
    ) {
    }

    public record AddCourseToCurriculumRequest(
            Long courseId,
            int yearLevel,
            String semester,
            String category,
            Integer sequenceOrder
    ) {
    }

    public record CloneCurriculumRequest(
            String newCode,
            String newName,
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
}