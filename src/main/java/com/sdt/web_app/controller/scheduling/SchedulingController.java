package com.sdt.web_app.controller.scheduling;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.scheduling.SchedulingDtos.*;
import com.sdt.web_app.service.scheduling.SchedulingService;
import com.sdt.web_app.service.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    private final com.sdt.web_app.service.security.DataScopingService dataScopingService;
    private final com.sdt.web_app.service.security.AcademicScopeAssertionService academicScopeAssertionService;

    // -------------------------------------------------------------------------
    // Room Endpoints
    // -------------------------------------------------------------------------
    @Auditable(action = "CREATE_ROOM", entityName = "Room")
    @PostMapping("/rooms")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomResponse response = schedulingService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Auditable(action = "READ_ALL_ROOMS", entityName = "Room")
    @GetMapping("/rooms")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<RoomResponse>> getAllRooms() {
        return ResponseEntity.ok(schedulingService.getAllRooms());
    }

    @Auditable(action = "READ_ROOMS_BY_CAMPUS", entityName = "Room", entityId = "#campusId")
    @GetMapping("/rooms/campus/{campusId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<RoomResponse>> getRoomsByCampus(@PathVariable("campusId") Long campusId) {
        return ResponseEntity.ok(schedulingService.getRoomsByCampus(campusId));
    }

    // -------------------------------------------------------------------------
    // Class Section & Timetable Endpoints (Gate 1 & Gate 2)
    // -------------------------------------------------------------------------
    @Auditable(action = "CREATE_SECTION", entityName = "ClassSection")
    @PostMapping("/sections")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<SectionDetailResponse> createSection(@Valid @RequestBody CreateSectionRequest request) {
        SectionDetailResponse response = schedulingService.createSection(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Auditable(action = "READ_SECTIONS_BY_TERM", entityName = "ClassSection", entityId = "#termId")
    @GetMapping("/sections/term/{termId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<SectionDetailResponse>> getSectionsByTerm(
            @PathVariable("termId") Long termId,
            Authentication authentication) {
        if (authentication != null && academicScopeAssertionService != null) {
            com.sdt.web_app.service.security.AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(authentication);
            if (scope.isDean()) {
                return ResponseEntity.ok(schedulingService.getSectionsByTerm(termId, java.util.Optional.of(scope.allowedProgramIds()), scope.userId()));
            } else if (scope.isChairperson()) {
                return ResponseEntity.ok(schedulingService.getSectionsByTerm(termId, java.util.Optional.of(List.of(scope.programId())), scope.userId()));
            } else if (scope.isFaculty()) {
                return ResponseEntity.ok(schedulingService.getSectionsByTerm(termId, java.util.Optional.empty(), scope.userId()));
            }
        }
        return ResponseEntity.ok(schedulingService.getSectionsByTerm(termId, java.util.Optional.empty(), null));
    }

    @Auditable(action = "READ_SECTION", entityName = "ClassSection", entityId = "#id")
    @GetMapping("/sections/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<SectionDetailResponse> getSectionById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(schedulingService.getSectionById(id));
    }

    @Auditable(action = "UPDATE_SECTION", entityName = "ClassSection", entityId = "#id")
    @PutMapping("/sections/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<SectionDetailResponse> updateSection(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateSectionRequest request) {
        SectionDetailResponse response = schedulingService.updateSection(id, request);
        return ResponseEntity.ok(response);
    }

    @Auditable(action = "DELETE_SECTION", entityName = "ClassSection", entityId = "#id")
    @DeleteMapping("/sections/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<Void> deleteSection(@PathVariable("id") Long id) {
        schedulingService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }

    @Auditable(action = "CREATE_SCHEDULE_SLOT", entityName = "ScheduleSlot", entityId = "#sectionId")
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
    @Auditable(action = "READ_FACULTY_WORKLOAD", entityName = "FacultyLoadSummary", entityId = "#facultyId")
    @GetMapping("/faculty-workload/term/{termId}/faculty/{facultyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY')")
    public ResponseEntity<FacultyLoadSummaryResponse> getFacultyWorkload(
            @PathVariable("termId") Long termId,
            @PathVariable("facultyId") Long facultyId) {
        return ResponseEntity.ok(schedulingService.getFacultyWorkload(termId, facultyId));
    }

    @Auditable(action = "APPROVE_FACULTY_OVERLOAD", entityName = "FacultyLoadSummary")
    @PostMapping("/faculty-workload/approve-overload")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public ResponseEntity<Void> approveOverload(
            @Valid @RequestBody ApproveOverloadRequest request,
            Authentication authentication) {
        Long approverId = securityUtils.resolveUserId(authentication);
        if (approverId == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Unable to resolve authenticated administrator identity.");
        }
        schedulingService.approveFacultyOverload(request.termId(), request.facultyUserId(), approverId);
        return ResponseEntity.ok().build();
    }

    @Auditable(action = "UPDATE_FACULTY_WORKLOAD_LIMIT", entityName = "FacultyLoadSummary", entityId = "#facultyUserId")
    @PutMapping("/faculty/{id}/workload-limit")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public ResponseEntity<FacultyLoadSummaryResponse> updateFacultyWorkloadLimit(
            @PathVariable("id") Long facultyUserId,
            @Valid @RequestBody UpdateFacultyLoadLimitRequest request,
            Authentication authentication) {
        Long adminUserId = securityUtils.resolveUserId(authentication);
        if (adminUserId == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Unable to resolve authenticated administrator identity.");
        }
        FacultyLoadSummaryResponse response = schedulingService.updateFacultyWorkloadLimit(
                facultyUserId, request.termId(), request.customMaxUnits(), request.reason(), adminUserId);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Scheduling Reference Data (Terms & Instructors)
    // -------------------------------------------------------------------------
    @Auditable(action = "READ_SCHEDULING_TERMS", entityName = "Term")
    @GetMapping("/terms")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<SchedulingTermDto>> getSchedulingTerms() {
        return ResponseEntity.ok(schedulingService.getSchedulingTerms());
    }

    @Auditable(action = "UPDATE_TERM_MAX_HOURS_PER_CLASS", entityName = "Term", entityId = "#termId")
    @PutMapping("/terms/{termId}/max-class-hours")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SchedulingTermDto> updateTermMaxHoursPerClass(
            @PathVariable("termId") Long termId,
            @Valid @RequestBody UpdateTermClassHourLimitRequest request) {
        SchedulingTermDto response = schedulingService.updateTermMaxHoursPerClass(termId, request.maxHoursPerClass());
        return ResponseEntity.ok(response);
    }

    @Auditable(action = "READ_AVAILABLE_INSTRUCTORS", entityName = "InstructorOption")
    @GetMapping("/instructors")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<InstructorOptionDto>> getAvailableInstructors() {
        return ResponseEntity.ok(schedulingService.getAvailableInstructors());
    }
}
