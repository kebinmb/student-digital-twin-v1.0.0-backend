package com.sdt.web_app.dto.grade;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class GradeDtos {

    public record RosterStudentDto(
            Long enrollmentItemId,
            Long studentId,
            String studentNumber,
            String studentName,
            String programCode,
            int yearLevel,
            BigDecimal finalNumericalGrade,
            String completionStatus
    ) {}

    public record SectionRosterResponse(
            Long sectionId,
            String sectionCode,
            Long courseId,
            String courseCode,
            String courseTitle,
            BigDecimal creditUnits,
            Long termId,
            String termName,
            String gradeStatus,
            Long primaryInstructorId,
            String primaryInstructorName,
            int enrolledCount,
            int maxCapacity,
            List<RosterStudentDto> students,
            Long updatedAtEpochMs
    ) {
        public SectionRosterResponse(
                Long sectionId,
                String sectionCode,
                Long courseId,
                String courseCode,
                String courseTitle,
                BigDecimal creditUnits,
                Long termId,
                String termName,
                String gradeStatus,
                Long primaryInstructorId,
                String primaryInstructorName,
                int enrolledCount,
                int maxCapacity,
                List<RosterStudentDto> students
        ) {
            this(sectionId, sectionCode, courseId, courseCode, courseTitle, creditUnits, termId, termName, gradeStatus, primaryInstructorId, primaryInstructorName, enrolledCount, maxCapacity, students, null);
        }
    }

    public record GradeEntryDto(
            @NotNull(message = "Enrollment item ID is required")
            Long enrollmentItemId,

            BigDecimal finalNumericalGrade,

            String completionStatus
    ) {}

    public record SaveSectionGradesRequest(
            @NotNull(message = "Grades list cannot be null")
            List<GradeEntryDto> grades,

            boolean submitForVerification,

            Long expectedUpdatedAtEpochMs
    ) {
        public SaveSectionGradesRequest(List<GradeEntryDto> grades, boolean submitForVerification) {
            this(grades, submitForVerification, null);
        }
    }

    public record GradeActionResponse(
            Long sectionId,
            String sectionCode,
            String gradeStatus,
            int updatedCount,
            String message
    ) {}

    public record RejectGradesRequest(
            String reason
    ) {}
}
