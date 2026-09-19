package com.sdt.web_app.dto.scheduling;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public class SchedulingDtos {

    public record CreateRoomRequest(
            @NotNull(message = "Campus ID is required")
            Long campusId,

            @NotBlank(message = "Room code is required")
            @Size(max = 30, message = "Room code must not exceed 30 characters")
            String code,

            @NotBlank(message = "Room name is required")
            @Size(max = 100, message = "Room name must not exceed 100 characters")
            String name,

            @NotBlank(message = "Building name is required")
            @Size(max = 100, message = "Building name must not exceed 100 characters")
            String building,

            @Min(value = 1, message = "Floor must be at least 1")
            int floor,

            @Min(value = 1, message = "Capacity must be at least 1")
            @Max(value = 500, message = "Capacity cannot exceed 500")
            int capacity,

            @NotBlank(message = "Room type is required")
            @Pattern(regexp = "LECTURE|LABORATORY|SPEECH_LAB|GYMNASIUM", message = "Invalid room type")
            String roomType
    ) {}

    public record RoomResponse(
            Long id,
            Long campusId,
            String campusName,
            String code,
            String name,
            String building,
            int floor,
            int capacity,
            String roomType,
            boolean isActive
    ) {}

    public record CreateSectionRequest(
            @NotNull(message = "Term ID is required")
            Long termId,

            @NotNull(message = "Curriculum ID is required")
            Long curriculumId,

            @NotNull(message = "Course ID is required")
            Long courseId,

            @NotBlank(message = "Section code is required")
            @Size(max = 30, message = "Section code cannot exceed 30 characters")
            String sectionCode,

            @Min(value = 1, message = "Capacity must be at least 1")
            @Max(value = 100, message = "Capacity cannot exceed 100")
            int maxCapacity,

            @NotEmpty(message = "At least one schedule slot is required")
            List<@Valid ScheduleSlotDto> scheduleSlots
    ) {}

    public record UpdateSectionRequest(
            @NotBlank(message = "Section code is required")
            @Size(max = 30, message = "Section code cannot exceed 30 characters")
            String sectionCode,

            @Min(value = 1, message = "Capacity must be at least 1")
            @Max(value = 100, message = "Capacity cannot exceed 100")
            int maxCapacity,

            @NotEmpty(message = "At least one schedule slot is required")
            List<@Valid ScheduleSlotDto> scheduleSlots
    ) {}

    public record ScheduleSlotDto(
            @NotNull(message = "Room ID is required")
            Long roomId,

            Long instructorUserId,

            String dayOfWeek,

            List<String> daysOfWeek,

            @NotNull(message = "Start time is required")
            LocalTime startTime,

            @NotNull(message = "End time is required")
            LocalTime endTime,

            @NotBlank(message = "Schedule type is required")
            @Pattern(regexp = "LECTURE|LABORATORY", message = "Schedule type must be LECTURE or LABORATORY")
            String scheduleType
    ) {
        public ScheduleSlotDto(Long roomId, Long instructorUserId, String dayOfWeek, LocalTime startTime, LocalTime endTime, String scheduleType) {
            this(roomId, instructorUserId, dayOfWeek, null, startTime, endTime, scheduleType);
        }

        public List<String> getEffectiveDays() {
            if (daysOfWeek != null && !daysOfWeek.isEmpty()) {
                return daysOfWeek;
            }
            if (dayOfWeek != null && !dayOfWeek.isBlank()) {
                return List.of(dayOfWeek);
            }
            return List.of();
        }
    }

    public record CreateScheduleSlotRequest(
            Long sectionId,
            Long courseId,
            Long facultyUserId,
            @NotNull(message = "Room ID is required")
            Long roomId,
            @NotEmpty(message = "At least one day is required")
            List<String> daysOfWeek,
            @NotNull(message = "Start time is required")
            LocalTime startTime,
            @NotNull(message = "End time is required")
            LocalTime endTime,
            boolean isLaboratory
    ) {}

    public record SectionDetailResponse(
            Long id,
            Long termId,
            String termName,
            Long curriculumId,
            String curriculumCode,
            String curriculumName,
            Long courseId,
            String courseCode,
            String courseTitle,
            BigDecimal lectureUnits,
            BigDecimal labUnits,
            BigDecimal creditUnits,
            String sectionCode,
            int maxCapacity,
            int enrolledCount,
            String status,
            List<ScheduleSlotResponse> schedules
    ) {}

    public record ScheduleSlotResponse(
            Long id,
            Long roomId,
            String roomCode,
            String roomName,
            Long instructorUserId,
            String instructorName,
            String dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            String scheduleType
    ) {}

    public record FacultyLoadSummaryResponse(
            Long facultyUserId,
            String facultyName,
            String facultyEmail,
            Long termId,
            String termName,
            BigDecimal regularUnits,
            BigDecimal overloadUnits,
            BigDecimal totalContactHours,
            boolean isOverloadApproved,
            String approvedByName,
            int numberOfPreparations,
            BigDecimal customMaxLoadUnits,
            BigDecimal effectiveMaxUnits,
            String overrideReason,
            String overriddenByName,
            List<AssignedSectionDto> assignedSections
    ) {}

    public record AssignedSectionDto(
            Long sectionId,
            String sectionCode,
            String courseCode,
            String courseTitle,
            BigDecimal creditUnits,
            int contactHours,
            String scheduleSummary
    ) {}

    public record ApproveOverloadRequest(
            @NotNull(message = "Term ID is required")
            Long termId,

            @NotNull(message = "Faculty User ID is required")
            Long facultyUserId
    ) {}

    public record UpdateFacultyLoadLimitRequest(
            @NotNull(message = "Term ID is required")
            Long termId,

            @NotNull(message = "Custom max load units is required")
            @DecimalMin(value = "0.0", message = "Custom load limit cannot be negative")
            @DecimalMax(value = "36.0", message = "Custom load limit cannot exceed 36.0 units")
            BigDecimal customMaxUnits,

            @NotBlank(message = "Override reason is required")
            String reason
    ) {}

    public record UpdateTermClassHourLimitRequest(
            @NotNull(message = "Max hours per class is required")
            @DecimalMin(value = "0.5", message = "Class session duration must be at least 0.5 hours")
            @DecimalMax(value = "12.0", message = "Class session duration cannot exceed 12.0 hours")
            BigDecimal maxHoursPerClass
    ) {}

    public record SchedulingTermDto(
            Long id,
            Long academicYearId,
            String academicYearCode,
            String termType,
            String termName,
            boolean isCurrent,
            boolean isActive,
            boolean isEnrollmentOpen,
            BigDecimal maxHoursPerClass
    ) {
        public SchedulingTermDto(Long id, Long academicYearId, String academicYearCode, String termType, String termName, boolean isCurrent, boolean isActive, boolean isEnrollmentOpen) {
            this(id, academicYearId, academicYearCode, termType, termName, isCurrent, isActive, isEnrollmentOpen, new BigDecimal("3.0"));
        }
    }

    public record InstructorOptionDto(
            Long id,
            String username,
            String email
    ) {}
}
