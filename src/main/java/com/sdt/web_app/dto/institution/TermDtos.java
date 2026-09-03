package com.sdt.web_app.dto.institution;

import com.sdt.web_app.entities.institution.TermType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public final class TermDtos {

    public record TermResponse(
            Long id,
            Long academicYearId,
            String academicYearCode,
            TermType termType,
            LocalDate startDate,
            LocalDate endDate,
            boolean isCurrent,
            boolean isActive,
            boolean enrollmentOpen,
            boolean gradingOpen,
            boolean addDropOpen
    ) {}

    public record CreateTermRequest(
            @NotNull(message = "Academic year ID is mandatory")
            Long academicYearId,

            @NotNull(message = "Term type is mandatory")
            TermType termType,

            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record UpdateTermScheduleRequest(
            LocalDate startDate,
            LocalDate endDate
    ) {}

    private TermDtos() {}
}
