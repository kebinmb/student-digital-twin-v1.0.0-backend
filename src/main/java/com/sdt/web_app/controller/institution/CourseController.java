package com.sdt.web_app.controller.institution;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.common.SliceResponse;
import com.sdt.web_app.dto.institution.CourseDtos.*;
import com.sdt.web_app.service.institution.CourseService;
import com.sdt.web_app.utils.SortPropertyMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@Validated
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @Auditable(action = "CREATE_COURSE", entityName = "Course")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        CourseResponse response = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Auditable(action = "READ_ALL_COURSES", entityName = "Course")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<CourseResponse>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @Auditable(action = "SEARCH_COURSES", entityName = "Course")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<Page<CourseResponse>> searchCourses(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "code") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {
        Pageable pageable = SortPropertyMapper.createCoursePageable(page, size, sortBy, sortDir);
        return ResponseEntity.ok(courseService.searchCourses(search, pageable));
    }

    @Auditable(action = "SEARCH_COURSES_SLICE", entityName = "Course")
    @GetMapping("/search-slice")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<SliceResponse<CourseResponse>> searchCoursesSlice(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "code") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDir) {
        Pageable pageable = SortPropertyMapper.createCoursePageable(page, size, sortBy, sortDir);
        return ResponseEntity.ok(courseService.searchCoursesSlice(search, pageable));
    }

    @Auditable(action = "READ_ACTIVE_COURSES", entityName = "Course")
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<CourseResponse>> getActiveCourses() {
        return ResponseEntity.ok(courseService.getActiveCourses());
    }

    @Auditable(action = "READ_COURSE", entityName = "Course", entityId = "#id")
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @Auditable(action = "UPDATE_COURSE", entityName = "Course", entityId = "#id")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCourseRequest request) {
        return ResponseEntity.ok(courseService.updateCourse(id, request));
    }

    @Auditable(action = "UPDATE_COURSE_STATUS", entityName = "Course", entityId = "#id")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<CourseResponse> toggleCourseStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(courseService.toggleCourseActive(id, active));
    }

    @Auditable(action = "DELETE_COURSE", entityName = "Course", entityId = "#id")
    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}
