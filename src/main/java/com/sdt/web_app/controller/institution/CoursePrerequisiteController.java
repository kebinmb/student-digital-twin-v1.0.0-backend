package com.sdt.web_app.controller.institution;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.institution.CoursePrerequisiteDtos.*;
import com.sdt.web_app.service.institution.CoursePrerequisiteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/prerequisites")
@Validated
@RequiredArgsConstructor
public class CoursePrerequisiteController {

    private final CoursePrerequisiteService prerequisiteService;

    @Auditable(action = "CREATE_PREREQUISITE", entityName = "CoursePrerequisite")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<CoursePrerequisiteResponse> createPrerequisite(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateCoursePrerequisiteRequest request) {
        if (!courseId.equals(request.courseId())) {
            throw new IllegalArgumentException("Path courseId " + courseId + " must match request courseId " + request.courseId());
        }
        CoursePrerequisiteResponse response = prerequisiteService.createPrerequisite(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<CoursePrerequisiteResponse>> getPrerequisitesByCourseId(@PathVariable Long courseId) {
        return ResponseEntity.ok(prerequisiteService.getPrerequisitesByCourseId(courseId));
    }

    @Auditable(action = "DELETE_PREREQUISITE", entityName = "CoursePrerequisite", entityId = "#prereqId")
    @DeleteMapping("/{prereqId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<Void> deletePrerequisite(
            @PathVariable Long courseId,
            @PathVariable Long prereqId) {
        prerequisiteService.deletePrerequisite(prereqId);
        return ResponseEntity.noContent().build();
    }
}
