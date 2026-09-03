package com.sdt.web_app.controller.institution;

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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<com.sdt.web_app.dto.institution.CurriculumDtos.CurriculumResponse> getCurriculumById(@PathVariable Long id) {
        return ResponseEntity.ok(curriculumService.getCurriculumById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<com.sdt.web_app.dto.institution.CurriculumDtos.CurriculumResponse> updateCurriculum(
            @PathVariable Long id,
            @Valid @RequestBody com.sdt.web_app.dto.institution.CurriculumDtos.UpdateCurriculumRequest request) {
        return ResponseEntity.ok(curriculumService.updateCurriculum(id, request));
    }

    @GetMapping("/{id}/courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<com.sdt.web_app.dto.institution.CurriculumCourseDtos.CurriculumCourseResponse>> getCurriculumCourses(@PathVariable Long id) {
        return ResponseEntity.ok(curriculumService.getCurriculumCourses(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<CurriculumSummaryResponse> createCurriculum(@Valid @RequestBody CreateCurriculumRequest request) {
        CurriculumSummaryResponse response = designerService.createCurriculum(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<Void> addCourseToCurriculum(
            @PathVariable Long id,
            @Valid @RequestBody AddCourseToCurriculumRequest request) {
        designerService.addCourseToCurriculum(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/courses/{curriculumCourseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<Void> removeCourseFromCurriculum(
            @PathVariable Long id,
            @PathVariable Long curriculumCourseId) {
        designerService.removeCourseFromCurriculum(id, curriculumCourseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<CurriculumSummaryResponse> cloneCurriculumAsNewRevision(
            @PathVariable Long id,
            @Valid @RequestBody CloneCurriculumRequest request) {
        CurriculumSummaryResponse response = designerService.cloneCurriculumAsNewRevision(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/designer")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY')")
    public ResponseEntity<DesignerViewResponse> getDesignerView(@PathVariable Long id) {
        return ResponseEntity.ok(designerService.getDesignerView(id));
    }

    @PutMapping("/{id}/courses/position")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<Void> updateCoursePosition(
            @PathVariable Long id,
            @Valid @RequestBody RelocateCourseRequest request) {
        designerService.relocateCoursePosition(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/prerequisites")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<Void> addPrerequisite(
            @PathVariable Long id,
            @Valid @RequestBody AddPrerequisiteRequest request) {
        designerService.addPrerequisite(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<ValidationReportDto> validateCurriculum(@PathVariable Long id) {
        return ResponseEntity.ok(validationService.validateCurriculum(id));
    }

    @PostMapping("/{id}/transition-state")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<Void> transitionState(
            @PathVariable Long id,
            @RequestParam Curriculum.Status status) {
        designerService.transitionCurriculumState(id, status);
        return ResponseEntity.ok().build();
    }

    // 1. Available Courses Drawer (Palette of unassigned subjects)
    @GetMapping("/{id}/available-courses")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<List<AvailableCourseDto>> getAvailableCourses(
            @PathVariable Long id,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(designerService.getAvailableCourses(id, search));
    }

    // 2. Batch Reorder (Prevents N+1 requests during Angular CDK Drag-and-Drop)
    @PutMapping("/{id}/courses/batch-positions")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<Void> updateBatchCoursePositions(
            @PathVariable Long id,
            @RequestBody List<@Valid RelocateCourseRequest> requests) {
        designerService.batchRelocatePositions(id, requests);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/prerequisites/{prerequisiteId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<Void> removePrerequisite(
            @PathVariable Long id,
            @PathVariable Long prerequisiteId) {
        designerService.removePrerequisite(id, prerequisiteId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/program/{programId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY')")
    public ResponseEntity<List<CurriculumSummaryResponse>> getCurriculaByProgram(
            @PathVariable Long programId) {
        return ResponseEntity.ok(designerService.getCurriculaByProgram(programId));
    }
}