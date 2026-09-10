package com.sdt.web_app.service.security;

import java.util.List;

public record AcademicScopeContext(
        Long userId,
        boolean isUnrestricted,   // ADMIN, REGISTRAR
        boolean isDean,
        boolean isChairperson,
        boolean isFaculty,
        Long collegeId,
        Long programId,
        List<Long> allowedProgramIds,
        List<Long> assignedSectionIds
) {
    public static AcademicScopeContext unrestricted(Long userId) {
        return new AcademicScopeContext(userId, true, false, false, false, null, null, List.of(), List.of());
    }

    public static AcademicScopeContext dean(Long userId, Long collegeId, List<Long> allowedProgramIds) {
        return new AcademicScopeContext(userId, false, true, false, false, collegeId, null, allowedProgramIds, List.of());
    }

    public static AcademicScopeContext chairperson(Long userId, Long collegeId, Long programId) {
        return new AcademicScopeContext(userId, false, false, true, false, collegeId, programId, List.of(programId), List.of());
    }

    public static AcademicScopeContext faculty(Long userId, Long collegeId, Long programId, List<Long> assignedSectionIds) {
        return new AcademicScopeContext(userId, false, false, false, true, collegeId, programId, programId != null ? List.of(programId) : List.of(), assignedSectionIds);
    }

    public static AcademicScopeContext other(Long userId) {
        return new AcademicScopeContext(userId, false, false, false, false, null, null, List.of(), List.of());
    }
}
