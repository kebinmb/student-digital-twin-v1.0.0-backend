package com.sdt.web_app.controller.scheduling;

import com.sdt.web_app.dto.scheduling.SchedulingDtos.*;
import com.sdt.web_app.service.scheduling.SchedulingService;
import com.sdt.web_app.service.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/scheduling")
@RequiredArgsConstructor
@Validated
public class SchedulingController {

    private final SchedulingService schedulingService;
    private final SecurityUtils securityUtils;

    // -------------------------------------------------------------------------
    // Room Endpoints
    // -------------------------------------------------------------------------
    @PostMapping("/rooms")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomResponse response = schedulingService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rooms")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        return ResponseEntity.ok(schedulingService.getAllRooms());
    }

    @GetMapping("/rooms/campus/{campusId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<RoomResponse>> getRoomsByCampus(@PathVariable("campusId") Long campusId) {
        return ResponseEntity.ok(schedulingService.getRoomsByCampus(campusId));
    }

    // -------------------------------------------------------------------------
    // Class Section & Timetable Endpoints (Gate 1 & Gate 2)
    // -------------------------------------------------------------------------
    @PostMapping("/sections")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<SectionDetailResponse> createSection(@Valid @RequestBody CreateSectionRequest request) {
        SectionDetailResponse response = schedulingService.createSection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/sections/term/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<SectionDetailResponse>> getSectionsByTerm(@PathVariable("termId") Long termId) {
        return ResponseEntity.ok(schedulingService.getSectionsByTerm(termId));
    }

    @GetMapping("/sections/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<SectionDetailResponse> getSectionById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(schedulingService.getSectionById(id));
    }

    @PostMapping("/sections/{sectionId}/slots")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<SectionDetailResponse> addScheduleSlots(
            @PathVariable("sectionId") Long sectionId,
            @Valid @RequestBody CreateScheduleSlotRequest request) {
        CreateScheduleSlotRequest effectiveRequest = request.sectionId() == null || !request.sectionId().equals(sectionId)
                ? new CreateScheduleSlotRequest(sectionId, request.courseId(), request.facultyUserId(), request.roomId(), request.daysOfWeek(), request.startTime(), request.endTime(), request.isLaboratory())
                : request;
        SectionDetailResponse response = schedulingService.addScheduleSlots(effectiveRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // -------------------------------------------------------------------------
    // Faculty Workload Endpoints
    // -------------------------------------------------------------------------
    @GetMapping("/faculty-workload/term/{termId}/faculty/{facultyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY')")
    public ResponseEntity<FacultyLoadSummaryResponse> getFacultyWorkload(
            @PathVariable("termId") Long termId,
            @PathVariable("facultyId") Long facultyId) {
        return ResponseEntity.ok(schedulingService.getFacultyWorkload(termId, facultyId));
    }

    @PostMapping("/faculty-workload/approve-overload")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public ResponseEntity<Void> approveOverload(
            @Valid @RequestBody ApproveOverloadRequest request,
            Authentication authentication) {
        Long approverId = securityUtils.resolveUserId(authentication);
        if (approverId == null) {
            throw new IllegalStateException("Cannot resolve authenticated administrative user ID.");
        }
        schedulingService.approveFacultyOverload(request.termId(), request.facultyUserId(), approverId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/faculty/{id}/workload-limit")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public ResponseEntity<FacultyLoadSummaryResponse> updateFacultyWorkloadLimit(
            @PathVariable("id") Long facultyUserId,
            @Valid @RequestBody UpdateFacultyLoadLimitRequest request,
            Authentication authentication) {
        Long adminUserId = securityUtils.resolveUserId(authentication);
        if (adminUserId == null) {
            throw new IllegalStateException("Cannot resolve authenticated administrative user ID.");
        }
        FacultyLoadSummaryResponse response = schedulingService.updateFacultyWorkloadLimit(
                facultyUserId, request.termId(), request.customMaxUnits(), request.reason(), adminUserId);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Scheduling Reference Data (Terms & Instructors)
    // -------------------------------------------------------------------------
    @GetMapping("/terms")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<SchedulingTermDto>> getSchedulingTerms() {
        return ResponseEntity.ok(schedulingService.getSchedulingTerms());
    }

    @PutMapping("/terms/{termId}/max-class-hours")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchedulingTermDto> updateTermMaxHoursPerClass(
            @PathVariable("termId") Long termId,
            @Valid @RequestBody UpdateTermClassHourLimitRequest request) {
        SchedulingTermDto response = schedulingService.updateTermMaxHoursPerClass(termId, request.maxHoursPerClass());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/instructors")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<InstructorOptionDto>> getAvailableInstructors() {
        return ResponseEntity.ok(schedulingService.getAvailableInstructors());
    }
}
