package com.sdt.web_app.dto.grade;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public class ClassRecordDtos {

    public record SectionGradingConfigResponse(
            Long id,
            Long sectionId,
            BigDecimal midtermWeight,
            BigDecimal finalWeight,
            boolean isLocked,
            List<SectionGradingCategoryDto> categories
    ) {}

    public record SectionGradingCategoryDto(
            Long id,
            String categoryName,
            BigDecimal weightPercentage,
            String termPeriod,
            int displayOrder,
            List<ClassRecordItemDto> items
    ) {}

    public record ClassRecordItemDto(
            Long id,
            Long categoryId,
            String itemTitle,
            BigDecimal maxPoints,
            int sequenceOrder
    ) {}

    public record UpdateSectionGradingConfigRequest(
            @NotNull(message = "Midterm weight is required")
            @DecimalMin(value = "0.00", message = "Midterm weight must be >= 0")
            @DecimalMax(value = "100.00", message = "Midterm weight must be <= 100")
            BigDecimal midtermWeight,

            @NotNull(message = "Final weight is required")
            @DecimalMin(value = "0.00", message = "Final weight must be >= 0")
            @DecimalMax(value = "100.00", message = "Final weight must be <= 100")
            BigDecimal finalWeight,

            List<CategoryWeightRequest> categories
    ) {}

    public record CategoryWeightRequest(
            Long id,
            @NotBlank(message = "Category name is required")
            String categoryName,
            @NotNull(message = "Weight percentage is required")
            BigDecimal weightPercentage,
            @NotBlank(message = "Term period is required")
            String termPeriod,
            int displayOrder
    ) {}

    public record CreateClassRecordItemRequest(
            @NotNull(message = "Category ID is required")
            Long categoryId,
            @NotBlank(message = "Item title is required")
            String itemTitle,
            @NotNull(message = "Max points is required")
            @DecimalMin(value = "0.01", message = "Max points must be greater than zero")
            BigDecimal maxPoints,
            int sequenceOrder
    ) {}

    public record StudentScoreEntryDto(
            Long itemId,
            Long studentId,
            BigDecimal scoreEarned,
            boolean isExcused
    ) {}

    public record BatchSaveScoresRequest(
            @NotNull(message = "Scores list cannot be null")
            List<StudentScoreEntryDto> scores
    ) {}

    public record StudentScoreMatrixRowDto(
            Long studentId,
            String studentNumber,
            String studentName,
            String programCode,
            int yearLevel,
            List<StudentScoreEntryDto> scores,
            BigDecimal midtermRawPercentage,
            BigDecimal finalRawPercentage,
            BigDecimal totalRawPercentage,
            BigDecimal transmutedGrade,
            String completionStatus
    ) {}

    public record ClassRecordMatrixResponse(
            Long sectionId,
            String sectionCode,
            String courseCode,
            String courseTitle,
            SectionGradingConfigResponse config,
            List<StudentScoreMatrixRowDto> rows
    ) {}
}
