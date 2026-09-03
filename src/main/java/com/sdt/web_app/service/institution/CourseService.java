package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.CourseDtos.*;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.repositories.institution.CoursePrerequisiteRepository;
import com.sdt.web_app.repositories.institution.CourseRepository;
import com.sdt.web_app.repositories.institution.CurriculumCourseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final CurriculumCourseRepository curriculumCourseRepository;
    private final CoursePrerequisiteRepository prerequisiteRepository;

    public CourseResponse createCourse(CreateCourseRequest request) {
        if (courseRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Course with code already exists: " + request.code());
        }

        BigDecimal creditUnits = request.lectureUnits().add(request.labUnits());

        Course course = Course.builder()
                .code(request.code().trim().toUpperCase())
                .title(request.title().trim())
                .lectureUnits(request.lectureUnits())
                .labUnits(request.labUnits())
                .creditUnits(creditUnits)
                .contactHoursLec(request.contactHoursLec())
                .contactHoursLab(request.contactHoursLab())
                .description(request.description())
                .isActive(true)
                .build();

        Course saved = courseRepository.save(course);
        return mapToResponse(saved);
    }

    public CourseResponse updateCourse(Long id, UpdateCourseRequest request) {
        Course course = findEntityById(id);
        course.updateCourseDetails(
                request.title(),
                request.lectureUnits(),
                request.labUnits(),
                request.contactHoursLec(),
                request.contactHoursLab(),
                request.description()
        );
        return mapToResponse(course);
    }

    public CourseResponse toggleCourseActive(Long id, boolean active) {
        Course course = findEntityById(id);
        if (active) {
            course.activate();
        } else {
            course.deactivate();
        }
        return mapToResponse(course);
    }

    public void deleteCourse(Long id) {
        Course course = findEntityById(id);
        if (curriculumCourseRepository.existsByCourseId(id)) {
            throw new IllegalStateException("Cannot delete course assigned to existing curricula");
        }
        if (prerequisiteRepository.existsByCourseIdOrPrerequisiteCourseId(id, id)) {
            throw new IllegalStateException("Cannot delete course referenced in prerequisite rules");
        }
        courseRepository.delete(course);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        return mapToResponse(findEntityById(id));
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<CourseResponse> searchCourses(String search, Pageable pageable) {
        return courseRepository.searchCourses(search, pageable)
                .map(this::mapToResponse);
    }

    private Course findEntityById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with ID: " + id));
    }

    private CourseResponse mapToResponse(Course c) {
        return new CourseResponse(
                c.getId(),
                c.getCode(),
                c.getTitle(),
                c.getLectureUnits(),
                c.getLabUnits(),
                c.getCreditUnits(),
                c.getContactHoursLec(),
                c.getContactHoursLab(),
                c.getDescription(),
                c.isActive()
        );
    }
}
