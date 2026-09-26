package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.CoursePrerequisiteDtos.*;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.CoursePrerequisite;
import com.sdt.web_app.repositories.institution.CoursePrerequisiteRepository;
import com.sdt.web_app.repositories.institution.CourseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class CoursePrerequisiteService {

    private final CoursePrerequisiteRepository prerequisiteRepository;
    private final CourseRepository courseRepository;

    public CoursePrerequisiteResponse createPrerequisite(CreateCoursePrerequisiteRequest request) {
        if (request.courseId().equals(request.prerequisiteCourseId())) {
            throw new IllegalArgumentException("Course cannot be its own prerequisite");
        }

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + request.courseId()));

        Course prerequisiteCourse = courseRepository.findById(request.prerequisiteCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Prerequisite course not found with ID: " + request.prerequisiteCourseId()));

        if (prerequisiteRepository.existsByCourseIdAndPrerequisiteCourseId(request.courseId(), request.prerequisiteCourseId())) {
            throw new IllegalArgumentException("Prerequisite relationship already exists between " + course.getCode() + " and " + prerequisiteCourse.getCode());
        }

        String ruleType = (request.ruleType() != null && !request.ruleType().isBlank())
                ? request.ruleType()
                : "HARD";

        // Cycle check: verify that prerequisiteCourse cannot reach course in the dependency graph for HARD prerequisites
        if ("HARD".equalsIgnoreCase(ruleType) && createsCycle(request.courseId(), request.prerequisiteCourseId())) {
            throw new IllegalStateException("Adding prerequisite introduces a circular dependency between " + course.getCode() + " and " + prerequisiteCourse.getCode());
        }

        String minGrade = (request.minGradeRequired() != null && !request.minGradeRequired().isBlank())
                ? request.minGradeRequired()
                : "3.00";

        CoursePrerequisite rule = CoursePrerequisite.builder()
                .course(course)
                .prerequisiteCourse(prerequisiteCourse)
                .ruleType(ruleType)
                .minGradeRequired(minGrade)
                .build();

        CoursePrerequisite saved = prerequisiteRepository.save(rule);
        return mapToResponse(saved);
    }

    public void deletePrerequisite(Long id) {
        CoursePrerequisite rule = prerequisiteRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prerequisite rule not found with ID: " + id));
        prerequisiteRepository.delete(rule);
    }

    @Transactional(readOnly = true)
    public List<CoursePrerequisiteResponse> getPrerequisitesByCourseId(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new IllegalArgumentException("Course not found with ID: " + courseId);
        }
        return prerequisiteRepository.findByCourseId(courseId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private boolean createsCycle(Long targetCourseId, Long newPrerequisiteId) {
        // We are adding: targetCourseId -> newPrerequisiteId
        // A cycle occurs if newPrerequisiteId can already reach targetCourseId (newPrerequisiteId requires targetCourseId)
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        queue.add(newPrerequisiteId);
        visited.add(newPrerequisiteId);

        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            if (currentId.equals(targetCourseId)) {
                return true;
            }

            List<CoursePrerequisite> prereqs = prerequisiteRepository.findByCourseId(currentId);
            for (CoursePrerequisite cp : prereqs) {
                if (!"HARD".equalsIgnoreCase(cp.getRuleType())) {
                    continue;
                }
                Long nextId = cp.getPrerequisiteCourse().getId();
                if (visited.add(nextId)) {
                    queue.add(nextId);
                }
            }
        }
        return false;
    }

    private CoursePrerequisiteResponse mapToResponse(CoursePrerequisite cp) {
        return new CoursePrerequisiteResponse(
                cp.getId(),
                cp.getCourse().getId(),
                cp.getCourse().getCode(),
                cp.getPrerequisiteCourse().getId(),
                cp.getPrerequisiteCourse().getCode(),
                cp.getPrerequisiteCourse().getTitle(),
                cp.getRuleType(),
                cp.getMinGradeRequired()
        );
    }
}
