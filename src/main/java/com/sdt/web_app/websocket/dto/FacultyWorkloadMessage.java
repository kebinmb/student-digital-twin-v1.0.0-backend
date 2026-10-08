package com.sdt.web_app.websocket.dto;

import java.time.Instant;

/**
 * Broadcast to /topic/faculty.{facultyId} when faculty workload or assignment updates.
 */
public record FacultyWorkloadMessage(
        Long facultyId,
        Long termId,
        Double totalTeachingUnits,
        Integer assignedSectionsCount,
        Double prepHours,
        String status,
        Instant updatedAt
) {}
