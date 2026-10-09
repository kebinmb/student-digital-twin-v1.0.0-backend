package com.sdt.web_app.dto.grade;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public class GradeChangeDtos {

    public record CreateGradeChangeRequest(
            @NotNull Long studentId,
            @NotNull Long courseId,
            @NotNull Long termId,
            @NotNull BigDecimal previousGrade,
            @NotNull BigDecimal newGrade,
            @NotNull @Size(min = 10, max = 500) String reason
    ) {}

    public record GradeChangeResponse(
            Long id,
            Long studentId,
            String studentNumber,
            String studentName,
            Long programId,
            String programCode,
            Long collegeId,
            Long courseId,
            String courseCode,
            String courseTitle,
            Long termId,
            String termName,
            BigDecimal previousGrade,
            BigDecimal newGrade,
            String reason,
            String status,
            String requestedByUsername,
            String approvedByUsername,
            Instant createdAt
    ) {
        public GradeChangeResponse(
                Long id,
                Long studentId,
                String studentNumber,
                String studentName,
                Long courseId,
                String courseCode,
                String courseTitle,
                Long termId,
                String termName,
                BigDecimal previousGrade,
                BigDecimal newGrade,
                String reason,
                String status,
                String requestedByUsername,
                String approvedByUsername,
                Instant createdAt
        ) {
            this(id, studentId, studentNumber, studentName, null, null, null, courseId, courseCode, courseTitle, termId, termName, previousGrade, newGrade, reason, status, requestedByUsername, approvedByUsername, createdAt);
        }
    }
}
