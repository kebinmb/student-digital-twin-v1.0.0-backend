package com.sdt.web_app.websocket.dto;

import java.time.LocalDate;

/**
 * Broadcast to /topic/terms/active whenever term status or window flags mutate.
 */
public record ActiveTermMessage(
        Long id,
        Long academicYearId,
        String academicYearCode,
        String termType,
        String termName,
        LocalDate startDate,
        LocalDate endDate,
        boolean isCurrent,
        boolean isActive,
        boolean enrollmentOpen,
        boolean gradingOpen,
        boolean addDropOpen
) {}
