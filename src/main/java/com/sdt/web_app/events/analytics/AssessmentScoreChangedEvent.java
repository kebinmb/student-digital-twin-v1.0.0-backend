package com.sdt.web_app.events.analytics;

public record AssessmentScoreChangedEvent(
        Long studentId,
        Long classRecordItemId
) {}
