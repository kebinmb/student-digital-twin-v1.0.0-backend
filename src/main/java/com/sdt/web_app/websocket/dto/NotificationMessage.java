package com.sdt.web_app.websocket.dto;

import java.time.Instant;

/**
 * Broadcast to /topic/notifications when system or campus notices are issued.
 */
public record NotificationMessage(
        Long noticeId,
        String title,
        String summary,
        String priority,
        String audienceScope,
        Instant publishedAt
) {}
