package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.CurriculumDesignerDtos.*;
import com.sdt.web_app.entities.institution.*;
import com.sdt.web_app.repositories.institution.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CurriculumDesignerService {

    private final CurriculumRepository curriculumRepository;
    private final CurriculumCourseRepository curriculumCourseRepository;
    private final CourseRepository courseRepository;
    private final CoursePrerequisiteRepository prerequisiteRepository;
    private final CurriculumValidationService validationService;

    public CurriculumDesignerService(
            CurriculumRepository curriculumRepository,
            CurriculumCourseRepository curriculumCourseRepository,
            CourseRepository courseRepository,
            CoursePrerequisiteRepository prerequisiteRepository,
            CurriculumValidationService validationService) {
        this.curriculumRepository = curriculumRepository;
        this.curriculumCourseRepository = curriculumCourseRepository;
        this.courseRepository = courseRepository;
        this.prerequisiteRepository = prerequisiteRepository;
        this.validationService = validationService;
    }

    @Transactional(readOnly = true)
    public DesignerViewResponse getDesignerView(Long curriculumId) {
        Curriculum curriculum = getCurriculum(curriculumId);
        List<CurriculumCourse> courses = curriculumCourseRepository.findByCurriculumId(curriculumId);

        BigDecimal totalUnits = courses.stream()
                .map(cc -> cc.getCourse().getCreditUnits())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalHours = courses.stream()
                .mapToInt(cc -> cc.getCourse().getContactHoursLec() + cc.getCourse().getContactHoursLab())
                .sum();

        // Group into Year Blocks
        Map<Integer, Map<String, List<CurriculumCourse>>> grouped = courses.stream()
                .collect(Collectors.groupingBy(
                        CurriculumCourse::getYearLevel,
                        TreeMap::new,
                        Collectors.groupingBy(CurriculumCourse::getSemester, TreeMap::new, Collectors.toList())
                ));

        List<YearBlockDto> yearBlocks = grouped.entrySet().stream().map(yearEntry -> {
            List<SemesterBlockDto> semesterBlocks = yearEntry.getValue().entrySet().stream().map(semEntry -> {
                List<CurriculumCourse> semCourses = semEntry.getValue();
                semCourses.sort(Comparator.comparingInt(CurriculumCourse::getSequenceOrder));

                BigDecimal semUnits = semCourses.stream()
                        .map(c -> c.getCourse().getCreditUnits())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                int semHours = semCourses.stream()
                        .mapToInt(c -> c.getCourse().getContactHoursLec() + c.getCourse().getContactHoursLab())
                        .sum();

                List<CourseItemDto> courseDtos = semCourses.stream().map(cc -> {
                    Course c = cc.getCourse();
                    List<String> prereqs = prerequisiteRepository.findByCourseId(c.getId()).stream()
                            .map(p -> p.getPrerequisiteCourse().getCode())
                            .toList();

                    return new CourseItemDto(
                            cc.getId(), c.getId(), c.getCode(), c.getTitle(),
                            c.getLectureUnits(), c.getLabUnits(), c.getCreditUnits(),
                            c.getContactHoursLec(), c.getContactHoursLab(),
                            cc.getCategory(), cc.getSequenceOrder(), prereqs
                    );
                }).toList();

                return new SemesterBlockDto(semEntry.getKey(), semUnits, semHours, courseDtos);
            }).toList();

            return new YearBlockDto(yearEntry.getKey(), semesterBlocks);
        }).toList();

        return new DesignerViewResponse(
                curriculum.getId(), curriculum.getCode(), curriculum.getName(),
                curriculum.getStatus().name(), totalUnits, totalHours, yearBlocks
        );
    }

    public void relocateCoursePosition(Long curriculumId, RelocateCourseRequest request) {
        Curriculum curriculum = getCurriculum(curriculumId);
        assertEditable(curriculum);

        CurriculumCourse course = curriculumCourseRepository.findById(request.curriculumCourseId())
                .orElseThrow(() -> new IllegalArgumentException("CurriculumCourse not found: " + request.curriculumCourseId()));

        course.relocatePosition(request.targetYearLevel(), request.targetSemester(), request.targetSequenceOrder());
    }

    public void addPrerequisite(Long curriculumId, AddPrerequisiteRequest request) {
        Curriculum curriculum = getCurriculum(curriculumId);
        assertEditable(curriculum);

        if (request.courseId().equals(request.prerequisiteCourseId())) {
            throw new IllegalArgumentException("A course cannot have itself as a prerequisite.");
        }

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + request.courseId()));
        Course prereq = courseRepository.findById(request.prerequisiteCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Prerequisite course not found: " + request.prerequisiteCourseId()));

        CoursePrerequisite rule = CoursePrerequisite.builder()
                .course(course)
                .prerequisiteCourse(prereq)
                .ruleType(request.ruleType() != null ? request.ruleType() : "HARD")
                .minGradeRequired(request.minGradeRequired() != null ? request.minGradeRequired() : "3.00")
                .build();

        prerequisiteRepository.save(rule);

        // Run validation to catch cycles immediately
        ValidationReportDto report = validationService.validateCurriculum(curriculumId);
        if (!report.valid()) {
            boolean hasCycle = report.errors().stream().anyMatch(e -> "CIRCULAR_DEPENDENCY_DETECTED".equals(e.code()));
            if (hasCycle) {
                // Rollback by throwing an exception
                throw new IllegalStateException("Prerequisite insertion rejected. It introduces a circular dependency.");
            }
        }
    }

    public void transitionCurriculumState(Long curriculumId, Curriculum.Status targetStatus) {
        Curriculum curriculum = getCurriculum(curriculumId);
        if (targetStatus == Curriculum.Status.APPROVED || targetStatus == Curriculum.Status.ACTIVE) {
            ValidationReportDto report = validationService.validateCurriculum(curriculumId);
            if (!report.valid()) {
                throw new IllegalStateException("Cannot approve or activate an invalid curriculum. Fix existing errors first.");
            }
        }
        curriculum.transitionTo(targetStatus);
    }

    private Curriculum getCurriculum(Long id) {
        return curriculumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Curriculum not found with ID: " + id));
    }

    private void assertEditable(Curriculum curriculum) {
        if (!curriculum.isEditable()) {
            throw new IllegalStateException("Curriculum is locked under status: " + curriculum.getStatus());
        }
    }
}