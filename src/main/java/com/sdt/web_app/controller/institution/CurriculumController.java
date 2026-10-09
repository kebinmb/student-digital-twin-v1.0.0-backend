package com.sdt.web_app.controller.institution;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.institution.CurriculumDesignerDtos.*;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.service.institution.CurriculumDesignerService;
import com.sdt.web_app.service.institution.CurriculumValidationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.sdt.web_app.service.institution.CurriculumService;

@RestController
@RequestMapping("/api/v1/curricula")
@Validated
public class CurriculumController {

    private final CurriculumDesignerService designerService;
    private final CurriculumValidationService validationService;
    private final CurriculumService curriculumService;

    public CurriculumController(CurriculumDesignerService designerService,
                                CurriculumValidationService validationService,
                                CurriculumService curriculumService) {
        this.designerService = designerService;
        this.validationService = validationService;
        this.curriculumService = curriculumService;
    }

    @Auditable(action = "READ_CURRICULUM", entityName = "Curriculum", entityId = "#id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<com.sdt.web_app.dto.institution.CurriculumDtos.CurriculumResponse> getCurriculumById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(curriculumService.getCurriculumById(id));
    }

    @Auditable(action = "UPDATE_CURRICULUM", entityName = "Curriculum", entityId = "#id")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<com.sdt.web_app.dto.institution.CurriculumDtos.CurriculumResponse> updateCurriculum(
            @PathVariable("id") Long id,
            @Valid @RequestBody com.sdt.web_app.dto.institution.CurriculumDtos.UpdateCurriculumRequest request) {
        return ResponseEntity.ok(curriculumService.updateCurriculum(id, request));
    }

    @Auditable(action = "DELETE_CURRICULUM", entityName = "Curriculum", entityId = "#id")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<Void> deleteCurriculum(@PathVariable("id") Long id) {
        designerService.deleteCurriculum(id);
        return ResponseEntity.noContent().build();
    }

    @Auditable(action = "READ_CURRICULUM_COURSES", entityName = "CurriculumCourse", entityId = "#id")
    @GetMapping("/{id}/courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<com.sdt.web_app.dto.institution.CurriculumCourseDtos.CurriculumCourseResponse>> getCurriculumCourses(@PathVariable("id") Long id) {
        return ResponseEntity.ok(curriculumService.getCurriculumCourses(id));
    }

    @Auditable(action = "CREATE_CURRICULUM", entityName = "Curriculum")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<CurriculumSummaryResponse> createCurriculum(@Valid @RequestBody CreateCurriculumRequest request) {
        CurriculumSummaryResponse response = designerService.createCurriculum(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Auditable(action = "ADD_COURSE_TO_CURRICULUM", entityName = "CurriculumCourse", entityId = "#id")
    @PostMapping("/{id}/courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<Void> addCourseToCurriculum(
            @PathVariable("id") Long id,
            @Valid @RequestBody AddCourseToCurriculumRequest request) {
        designerService.addCourseToCurriculum(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Auditable(action = "REMOVE_COURSE_FROM_CURRICULUM", entityName = "CurriculumCourse", entityId = "#curriculumCourseId")
    @DeleteMapping("/{id}/courses/{curriculumCourseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<Void> removeCourseFromCurriculum(
            @PathVariable("id") Long id,
            @PathVariable("curriculumCourseId") Long curriculumCourseId) {
        designerService.removeCourseFromCurriculum(id, curriculumCourseId);
        return ResponseEntity.noContent().build();
    }

    @Auditable(action = "CLONE_CURRICULUM", entityName = "Curriculum", entityId = "#id")
    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<CurriculumSummaryResponse> cloneCurriculumAsNewRevision(
            @PathVariable("id") Long id,
            @Valid @RequestBody CloneCurriculumRequest request) {
        CurriculumSummaryResponse response = designerService.cloneCurriculumAsNewRevision(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Auditable(action = "READ_CURRICULUM_DESIGNER_VIEW", entityName = "Curriculum", entityId = "#id")
    @GetMapping("/{id}/designer")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY')")
    public ResponseEntity<DesignerViewResponse> getDesignerView(@PathVariable("id") Long id) {
        return ResponseEntity.ok(designerService.getDesignerView(id));
    }

    @Auditable(action = "UPDATE_COURSE_POSITION", entityName = "CurriculumCourse", entityId = "#id")
    @PutMapping("/{id}/courses/position")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<Void> updateCoursePosition(
            @PathVariable("id") Long id,
            @Valid @RequestBody RelocateCourseRequest request) {
        designerService.relocateCoursePosition(id, request);
        return ResponseEntity.noContent().build();
    }

    @Auditable(action = "ADD_CURRICULUM_PREREQUISITE", entityName = "CurriculumPrerequisite", entityId = "#id")
    @PostMapping("/{id}/prerequisites")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<Void> addPrerequisite(
            @PathVariable("id") Long id,
            @Valid @RequestBody AddPrerequisiteRequest request) {
        designerService.addPrerequisite(id, request);
        return ResponseEntity.ok().build();
    }

    @Auditable(action = "VALIDATE_CURRICULUM", entityName = "Curriculum", entityId = "#id")
    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<ValidationReportDto> validateCurriculum(@PathVariable("id") Long id) {
        return ResponseEntity.ok(validationService.validateCurriculum(id));
    }

    @Auditable(action = "TRANSITION_CURRICULUM_STATE", entityName = "Curriculum", entityId = "#id")
    @PostMapping("/{id}/transition-state")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<Void> transitionState(
            @PathVariable("id") Long id,
            @RequestParam Curriculum.Status status) {
        designerService.transitionCurriculumState(id, status);
        return ResponseEntity.ok().build();
    }

    // 1. Available Courses Drawer (Palette of unassigned subjects)
    @Auditable(action = "READ_AVAILABLE_COURSES_FOR_CURRICULUM", entityName = "Course", entityId = "#id")
    @GetMapping("/{id}/available-courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<List<AvailableCourseDto>> getAvailableCourses(
            @PathVariable("id") Long id,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(designerService.getAvailableCourses(id, search));
    }

    // 2. Batch Reorder (Prevents N+1 requests during Angular CDK Drag-and-Drop)
    @Auditable(action = "BATCH_UPDATE_COURSE_POSITIONS", entityName = "CurriculumCourse", entityId = "#id")
    @PutMapping("/{id}/courses/batch-positions")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<Void> updateBatchCoursePositions(
            @PathVariable("id") Long id,
            @RequestBody List<@Valid RelocateCourseRequest> requests) {
        designerService.batchRelocatePositions(id, requests);
        return ResponseEntity.noContent().build();
    }

    @Auditable(action = "REMOVE_CURRICULUM_PREREQUISITE", entityName = "CurriculumPrerequisite", entityId = "#prerequisiteId")
    @DeleteMapping("/{id}/prerequisites/{prerequisiteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<Void> removePrerequisite(
            @PathVariable("id") Long id,
            @PathVariable("prerequisiteId") Long prerequisiteId) {
        designerService.removePrerequisite(id, prerequisiteId);
        return ResponseEntity.noContent().build();
    }

    @Auditable(action = "READ_CURRICULA_BY_PROGRAM", entityName = "Curriculum", entityId = "#programId")
    @GetMapping("/program/{programId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY')")
    public ResponseEntity<List<CurriculumSummaryResponse>> getCurriculaByProgram(
            @PathVariable("programId") Long programId) {
        return ResponseEntity.ok(designerService.getCurriculaByProgram(programId));
    }

    @Auditable(action = "READ_CURRICULA_BY_PROGRAM_MAJOR", entityName = "Curriculum", entityId = "#programId")
    @GetMapping("/program/{programId}/major/{majorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY')")
    public ResponseEntity<List<com.sdt.web_app.dto.institution.CurriculumDtos.CurriculumResponse>> getCurriculaByProgramAndMajor(
            @PathVariable("programId") Long programId,
            @PathVariable("majorId") Long majorId) {
        return ResponseEntity.ok(curriculumService.getCurriculaByProgramAndMajor(programId, majorId));
    }

    @Auditable(action = "READ_CURRICULUM_LOOKUP_OPTIONS", entityName = "Curriculum")
    @GetMapping({"", "/lookup"})
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'CASHIER', 'ACCOUNTANT')")
    public ResponseEntity<List<CurriculumLookupOption>> getCurriculumLookupOptions() {
        return ResponseEntity.ok(designerService.getCurriculumLookupOptions());
    }
}