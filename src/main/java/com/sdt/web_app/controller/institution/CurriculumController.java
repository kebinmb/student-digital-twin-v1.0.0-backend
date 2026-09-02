package com.sdt.web_app.controller.institution;

import com.sdt.web_app.dto.institution.CurriculumDesignerDtos.*;
import com.sdt.web_app.entities.institution.Curriculum;

import com.sdt.web_app.service.institution.CurriculumDesignerService;
import com.sdt.web_app.service.institution.CurriculumValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/curricula")
@CrossOrigin(origins = "*") // Match to your Angular 19 port
public class CurriculumController {

    private final CurriculumDesignerService designerService;
    private final CurriculumValidationService validationService;

    public CurriculumController(CurriculumDesignerService designerService,
                                CurriculumValidationService validationService) {
        this.designerService = designerService;
        this.validationService = validationService;
    }

    @PostMapping
    public ResponseEntity<CurriculumSummaryResponse> createCurriculum(@RequestBody CreateCurriculumRequest request) {
        CurriculumSummaryResponse response = designerService.createCurriculum(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/courses")
    public ResponseEntity<Void> addCourseToCurriculum(@PathVariable Long id, @RequestBody AddCourseToCurriculumRequest request) {
        designerService.addCourseToCurriculum(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/courses/{curriculumCourseId}")
    public ResponseEntity<Void> removeCourseFromCurriculum(@PathVariable Long id, @PathVariable Long curriculumCourseId) {
        designerService.removeCourseFromCurriculum(id, curriculumCourseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<CurriculumSummaryResponse> cloneCurriculumAsNewRevision(@PathVariable Long id, @RequestBody CloneCurriculumRequest request) {
        CurriculumSummaryResponse response = designerService.cloneCurriculumAsNewRevision(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/designer")
    public ResponseEntity<DesignerViewResponse> getDesignerView(@PathVariable Long id) {
        return ResponseEntity.ok(designerService.getDesignerView(id));
    }

    @PutMapping("/{id}/courses/position")
    public ResponseEntity<Void> updateCoursePosition(@PathVariable Long id, @RequestBody RelocateCourseRequest request) {
        designerService.relocateCoursePosition(id, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/prerequisites")
    public ResponseEntity<Void> addPrerequisite(@PathVariable Long id, @RequestBody AddPrerequisiteRequest request) {
        designerService.addPrerequisite(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<ValidationReportDto> validateCurriculum(@PathVariable Long id) {
        return ResponseEntity.ok(validationService.validateCurriculum(id));
    }

    @PostMapping("/{id}/transition-state")
    public ResponseEntity<Void> transitionState(@PathVariable Long id, @RequestParam Curriculum.Status status) {
        designerService.transitionCurriculumState(id, status);
        return ResponseEntity.ok().build();
    }
}