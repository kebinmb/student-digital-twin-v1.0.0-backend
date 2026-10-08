package com.sdt.web_app.websocket.dto;

import java.time.Instant;

/**
 * Broadcast to /topic/schedule.{classId} when class section schedule details change.
 */
public record ClassScheduleMessage(
        Long classSectionId,
        Long termId,
        String courseCode,
        String sectionName,
        String room,
        String dayOfWeek,
        String startTime,
        String endTime,
        String changeType,
        Instant updatedAt
) {}
