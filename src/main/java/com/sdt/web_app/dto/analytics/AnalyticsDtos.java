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
            Long studentId,
            String studentNumber,
            String studentName,
            String attendanceStatus,
            boolean isGeofenceValid,
            Instant scannedAt
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
            Instant evaluatedAt
    ) {}

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
}
