package com.sdt.web_app.dto.institution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public final class GradingScaleDtos {

    public record GradingScaleResponse(
            Long id,
            String code,
            BigDecimal numericGrade,
            BigDecimal percentageMin,
            BigDecimal percentageMax,
            String transmutedGrade,
            String remarks,
            boolean isPassing,
            boolean isNonNumeric
    ) {}

    public record CreateGradingScaleRequest(
            @NotBlank(message = "Grade code is mandatory")
            String code,

            BigDecimal numericGrade,

            @NotNull(message = "Minimum percentage is mandatory")
            BigDecimal percentageMin,

            @NotNull(message = "Maximum percentage is mandatory")
            BigDecimal percentageMax,

            String transmutedGrade,

            @NotBlank(message = "Remarks are mandatory")
            String remarks,

            boolean isPassing,
            boolean isNonNumeric
    ) {}

    public record UpdateGradingScaleRequest(
            @NotNull(message = "Minimum percentage is mandatory")
            BigDecimal percentageMin,

            @NotNull(message = "Maximum percentage is mandatory")
            BigDecimal percentageMax,

            @NotBlank(message = "Remarks are mandatory")
            String remarks,

            boolean isPassing
    ) {}

    private GradingScaleDtos() {}
}
