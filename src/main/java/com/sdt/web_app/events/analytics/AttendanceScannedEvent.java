package com.sdt.web_app.events.analytics;

public record AttendanceScannedEvent(
        Long studentId,
        Long sessionId,
        Long recordId
) {}
