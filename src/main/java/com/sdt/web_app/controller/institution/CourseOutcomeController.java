package com.sdt.web_app.controller.institution;

import com.sdt.web_app.dto.institution.CourseOutcomeDtos.*;
import com.sdt.web_app.service.institution.CourseOutcomeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
public class CourseOutcomeController {

    private final CourseOutcomeService courseOutcomeService;

    @PostMapping("/api/v1/courses/{courseId}/outcomes")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<CourseOutcomeResponse> createCourseOutcome(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateCourseOutcomeRequest request) {
        CourseOutcomeResponse response = courseOutcomeService.createCourseOutcome(courseId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/courses/{courseId}/outcomes")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<CourseOutcomeResponse>> getOutcomesByCourseId(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseOutcomeService.getOutcomesByCourseId(courseId));
    }

    @GetMapping({"/api/v1/courses/{courseId}/outcomes/{outcomeId}", "/api/v1/course-outcomes/{outcomeId}"})
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<CourseOutcomeResponse> getOutcomeById(
            @PathVariable(required = false) Long courseId,
            @PathVariable Long outcomeId) {
        return ResponseEntity.ok(courseOutcomeService.getCourseOutcomeById(outcomeId));
    }

    @PutMapping({"/api/v1/courses/{courseId}/outcomes/{outcomeId}", "/api/v1/course-outcomes/{outcomeId}"})
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<CourseOutcomeResponse> updateCourseOutcome(
            @PathVariable(required = false) Long courseId,
            @PathVariable Long outcomeId,
            @Valid @RequestBody UpdateCourseOutcomeRequest request) {
        return ResponseEntity.ok(courseOutcomeService.updateCourseOutcome(outcomeId, request));
    }

    @DeleteMapping({"/api/v1/courses/{courseId}/outcomes/{outcomeId}", "/api/v1/course-outcomes/{outcomeId}"})
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<Void> deleteCourseOutcome(
            @PathVariable(required = false) Long courseId,
            @PathVariable Long outcomeId) {
        courseOutcomeService.deleteCourseOutcome(outcomeId);
        return ResponseEntity.noContent().build();
    }
}
