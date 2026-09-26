package com.sdt.web_app.dto.analytics;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class AnalyticsDtos {

    public record StartAttendanceSessionRequest(
            @NotNull Long sectionScheduleId,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer allowedRadiusMeters
    ) {}

    public record AttendanceSessionResponse(
            Long sessionId,
            Long sectionScheduleId,
            String qrSeed,
            Instant expiresAt,
            BigDecimal latitude,
            BigDecimal longitude,
            int allowedRadiusMeters,
            String qrCodeDataUrl
    ) {}

    public record ScanAttendanceRequest(
            @NotNull String qrSeed,
            @NotNull Long studentId,
            BigDecimal latitude,
            BigDecimal longitude,
            String deviceFingerprint
    ) {}

    public record AttendanceRecordResponse(
            Long recordId,
            Long sessionId,
            String sectionCode,
            String courseCode,
            Long studentId,
            String studentNumber,
            String studentName,
            String attendanceStatus,
            boolean isGeofenceValid,
            Instant scannedAt,
            String deviceFingerprint
    ) {}

    public record VerifyCreatorAttendanceRequest(
            @NotNull String qrSeed,
            BigDecimal latitude,
            BigDecimal longitude,
            String deviceFingerprint
    ) {}

    public record FacultyAttendanceRecordResponse(
            Long recordId,
            Long sessionId,
            String sectionCode,
            String courseCode,
            Long facultyUserId,
            String facultyName,
            String facultyRole,
            String attendanceStatus,
            boolean isGeofenceValid,
            Instant verifiedAt,
            String deviceFingerprint
    ) {}

    public record ActivityAlertDto(
            String activityTitle,
            String categoryName,
            BigDecimal scoreEarned,
            BigDecimal maxPoints,
            BigDecimal percentage,
            String suggestion
    ) {}

    public record DigitalTwinRiskProfileDto(
            Long studentId,
            String studentNumber,
            String studentName,
            String programCode,
            Integer yearLevel,
            BigDecimal academicRiskScore,
            BigDecimal attendanceRiskScore,
            BigDecimal socioeconomicRiskScore,
            String compositeRiskLevel,
            BigDecimal predictedDropoutProbability,
            List<String> recommendedInterventions,
            Instant evaluatedAt,
            List<ActivityAlertDto> activityAlerts
    ) {
        public DigitalTwinRiskProfileDto(
                Long studentId,
                String studentNumber,
                String studentName,
                String programCode,
                Integer yearLevel,
                BigDecimal academicRiskScore,
                BigDecimal attendanceRiskScore,
                BigDecimal socioeconomicRiskScore,
                String compositeRiskLevel,
                BigDecimal predictedDropoutProbability,
                List<String> recommendedInterventions,
                Instant evaluatedAt
        ) {
            this(studentId, studentNumber, studentName, programCode, yearLevel, academicRiskScore, attendanceRiskScore, socioeconomicRiskScore, compositeRiskLevel, predictedDropoutProbability, recommendedInterventions, evaluatedAt, List.of());
        }
    }

    public record EarlyWarningRadarItemDto(
            Long studentId,
            String studentNumber,
            String studentName,
            String programCode,
            Integer yearLevel,
            String riskLevel,
            BigDecimal dropoutProbability,
            String primaryRiskFactor,
            String suggestedAction
    ) {}

    public record DispatchInterventionRequest(
            @NotNull Long studentId,
            Long riskScoreId,
            @NotNull String interventionType,
            Long assignedCounselorId,
            String triggerFactor,
            String notes
    ) {}

    public record UpdateInterventionStatusRequest(
            @NotNull String status,
            String resolutionSummary,
            String additionalNotes
    ) {}

    public record StudentInterventionDto(
            Long id,
            Long studentId,
            String studentNumber,
            String studentName,
            Long riskScoreId,
            String interventionType,
            String status,
            Long assignedCounselorId,
            String assignedCounselorName,
            String triggerFactor,
            String caseNotes,
            String resolutionSummary,
            Instant dispatchedAt,
            Instant resolvedAt
    ) {}
}
