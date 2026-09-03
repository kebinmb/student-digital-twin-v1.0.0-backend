package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.CourseOutcomeDtos.*;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.CourseOutcome;
import com.sdt.web_app.repositories.institution.CiloPiloMappingRepository;
import com.sdt.web_app.repositories.institution.CourseOutcomeRepository;
import com.sdt.web_app.repositories.institution.CourseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseOutcomeService {

    private final CourseOutcomeRepository courseOutcomeRepository;
    private final CourseRepository courseRepository;
    private final CiloPiloMappingRepository ciloPiloMappingRepository;

    public CourseOutcomeResponse createCourseOutcome(Long courseId, CreateCourseOutcomeRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + courseId));

        if (courseOutcomeRepository.existsByCourseIdAndCode(courseId, request.code())) {
            throw new IllegalArgumentException("Course outcome with code " + request.code() + " already exists for course " + course.getCode());
        }

        CourseOutcome outcome = CourseOutcome.builder()
                .course(course)
                .code(request.code().trim().toUpperCase())
                .description(request.description().trim())
                .bloomsLevel(request.bloomsLevel().trim())
                .build();

        CourseOutcome saved = courseOutcomeRepository.save(outcome);
        return mapToResponse(saved);
    }

    public CourseOutcomeResponse updateCourseOutcome(Long id, UpdateCourseOutcomeRequest request) {
        CourseOutcome outcome = findEntityById(id);
        outcome.updateOutcomeDetails(request.description(), request.bloomsLevel());
        return mapToResponse(outcome);
    }

    public void deleteCourseOutcome(Long id) {
        CourseOutcome outcome = findEntityById(id);
        if (ciloPiloMappingRepository.existsByCourseOutcomeId(id)) {
            throw new IllegalStateException("Cannot delete course outcome linked to program outcomes in CILO-PILO matrix");
        }
        courseOutcomeRepository.delete(outcome);
    }

    @Transactional(readOnly = true)
    public CourseOutcomeResponse getCourseOutcomeById(Long id) {
        return mapToResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<CourseOutcomeResponse> getOutcomesByCourseId(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new IllegalArgumentException("Course not found with ID: " + courseId);
        }
        return courseOutcomeRepository.findByCourseId(courseId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private CourseOutcome findEntityById(Long id) {
        return courseOutcomeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course outcome not found with ID: " + id));
    }

    private CourseOutcomeResponse mapToResponse(CourseOutcome co) {
        return new CourseOutcomeResponse(
                co.getId(),
                co.getCourse().getId(),
                co.getCourse().getCode(),
                co.getCode(),
                co.getDescription(),
                co.getBloomsLevel()
        );
    }
}
