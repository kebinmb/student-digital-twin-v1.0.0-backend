package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.CurriculumDesignerDtos.*;
import com.sdt.web_app.entities.institution.*;
import com.sdt.web_app.repositories.institution.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final ProgramRepository programRepository;
    private final CurriculumValidationService validationService;

    public CurriculumDesignerService(
            CurriculumRepository curriculumRepository,
            CurriculumCourseRepository curriculumCourseRepository,
            CourseRepository courseRepository,
            CoursePrerequisiteRepository prerequisiteRepository,
            ProgramRepository programRepository,
            CurriculumValidationService validationService) {
        this.curriculumRepository = curriculumRepository;
        this.curriculumCourseRepository = curriculumCourseRepository;
        this.courseRepository = courseRepository;
        this.prerequisiteRepository = prerequisiteRepository;
        this.programRepository = programRepository;
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

        // Batch-fetch all prerequisites for all courses in this curriculum in a single query (Eliminates N+1)
        List<Long> courseIds = courses.stream().map(cc -> cc.getCourse().getId()).distinct().toList();
        Map<Long, List<String>> prereqMap = courseIds.isEmpty() ? Collections.emptyMap() :
                prerequisiteRepository.findPrerequisitesForCourseIds(courseIds).stream()
                        .collect(Collectors.groupingBy(
                                cp -> cp.getCourse().getId(),
                                Collectors.mapping(cp -> cp.getPrerequisiteCourse().getCode(), Collectors.toList())
                        ));

        // Initialize standard 4-year undergraduate structure with 1st & 2nd Semesters
        Map<Integer, Map<String, List<CurriculumCourse>>> grouped = new TreeMap<>();
        for (int y = 1; y <= 4; y++) {
            Map<String, List<CurriculumCourse>> semMap = new TreeMap<>();
            semMap.put("1ST_SEM", new ArrayList<>());
            semMap.put("2ND_SEM", new ArrayList<>());
            grouped.put(y, semMap);
        }

        // Merge existing assigned courses
        for (CurriculumCourse cc : courses) {
            grouped.computeIfAbsent(cc.getYearLevel(), k -> new TreeMap<>())
                    .computeIfAbsent(cc.getSemester(), k -> new ArrayList<>())
                    .add(cc);
        }

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
                    List<String> prereqs = prereqMap.getOrDefault(c.getId(), Collections.emptyList());

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

        if (prerequisiteRepository.existsByCourseIdAndPrerequisiteCourseId(request.courseId(), request.prerequisiteCourseId())) {
            throw new IllegalArgumentException("Prerequisite relationship already exists between these courses.");
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

    public CurriculumSummaryResponse createCurriculum(CreateCurriculumRequest request) {
        if (curriculumRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Curriculum code already exists: " + request.code());
        }

        Program program = programRepository.findById(request.programId())
                .orElseThrow(() -> new IllegalArgumentException("Program not found with ID: " + request.programId()));

        Curriculum curriculum = Curriculum.builder()
                .program(program)
                .code(request.code())
                .name(request.name())
                .effectiveAcademicYear(request.effectiveAcademicYear())
                .status(Curriculum.Status.DRAFT)
                .versionNumber(1)
                .isActive(true)
                .build();

        Curriculum saved = curriculumRepository.save(curriculum);

        return new CurriculumSummaryResponse(
                saved.getId(),
                saved.getCode(),
                saved.getName(),
                program.getCode(),
                saved.getEffectiveAcademicYear(),
                saved.getStatus().name(),
                saved.getVersionNumber()
        );
    }

    public void addCourseToCurriculum(Long curriculumId, AddCourseToCurriculumRequest request) {
        Curriculum curriculum = getCurriculum(curriculumId);
        assertEditable(curriculum);

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found with ID: " + request.courseId()));

        if (curriculumCourseRepository.existsByCurriculumIdAndCourseId(curriculumId, request.courseId())) {
            throw new IllegalArgumentException("Course is already assigned to this curriculum: " + course.getCode());
        }

        int seqOrder;
        if (request.sequenceOrder() != null) {
            seqOrder = request.sequenceOrder();
        } else {
            int maxSeq = curriculumCourseRepository
                    .findByCurriculumIdAndYearLevelAndSemester(curriculumId, request.yearLevel(), request.semester())
                    .stream()
                    .mapToInt(CurriculumCourse::getSequenceOrder)
                    .max()
                    .orElse(0);
            seqOrder = maxSeq + 1;
        }

        String category = (request.category() != null && !request.category().isBlank())
                ? request.category()
                : "PROFESSIONAL_MAJOR";

        CurriculumCourse curriculumCourse = CurriculumCourse.builder()
                .curriculum(curriculum)
                .course(course)
                .yearLevel(request.yearLevel())
                .semester(request.semester())
                .category(category)
                .sequenceOrder(seqOrder)
                .build();

        curriculumCourseRepository.save(curriculumCourse);
    }

    public void removeCourseFromCurriculum(Long curriculumId, Long curriculumCourseId) {
        Curriculum curriculum = getCurriculum(curriculumId);
        assertEditable(curriculum);

        CurriculumCourse curriculumCourse = curriculumCourseRepository.findById(curriculumCourseId)
                .orElseThrow(() -> new IllegalArgumentException("CurriculumCourse not found with ID: " + curriculumCourseId));

        if (!curriculumCourse.getCurriculum().getId().equals(curriculumId)) {
            throw new IllegalArgumentException("CurriculumCourse " + curriculumCourseId + " does not belong to curriculum " + curriculumId);
        }

        curriculumCourseRepository.delete(curriculumCourse);
    }

    public CurriculumSummaryResponse cloneCurriculumAsNewRevision(Long sourceCurriculumId, CloneCurriculumRequest request) {
        Curriculum source = getCurriculum(sourceCurriculumId);

        if (curriculumRepository.existsByCode(request.newCode())) {
            throw new IllegalArgumentException("Curriculum code already exists: " + request.newCode());
        }

        Curriculum cloned = Curriculum.builder()
                .program(source.getProgram())
                .code(request.newCode())
                .name(request.newName())
                .effectiveAcademicYear(request.effectiveAcademicYear())
                .status(Curriculum.Status.DRAFT)
                .versionNumber(source.getVersionNumber() + 1)
                .isActive(true)
                .build();

        Curriculum savedCloned = curriculumRepository.save(cloned);

        List<CurriculumCourse> sourceCourses = curriculumCourseRepository.findByCurriculumId(sourceCurriculumId);
        for (CurriculumCourse cc : sourceCourses) {
            CurriculumCourse clonedCc = CurriculumCourse.builder()
                    .curriculum(savedCloned)
                    .course(cc.getCourse())
                    .yearLevel(cc.getYearLevel())
                    .semester(cc.getSemester())
                    .category(cc.getCategory())
                    .sequenceOrder(cc.getSequenceOrder())
                    .build();
            curriculumCourseRepository.save(clonedCc);
        }

        return new CurriculumSummaryResponse(
                savedCloned.getId(),
                savedCloned.getCode(),
                savedCloned.getName(),
                source.getProgram().getCode(),
                savedCloned.getEffectiveAcademicYear(),
                savedCloned.getStatus().name(),
                savedCloned.getVersionNumber()
        );
    }

    @Transactional(readOnly = true)
    public List<AvailableCourseDto> getAvailableCourses(Long curriculumId, String search) {
        return getAvailableCourses(curriculumId, search, PageRequest.of(0, 50));
    }

    @Transactional(readOnly = true)
    public List<AvailableCourseDto> getAvailableCourses(Long curriculumId, String search, Pageable pageable) {
        List<CurriculumCourse> assigned = curriculumCourseRepository.findByCurriculumId(curriculumId);
        Set<Long> assignedCourseIds = assigned.stream()
                .map(cc -> cc.getCourse().getId())
                .collect(Collectors.toSet());

        Page<Course> paged;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        if (cleanSearch == null) {
            if (assignedCourseIds.isEmpty()) {
                paged = courseRepository.findByIsActiveTrue(pageable);
            } else {
                paged = courseRepository.findByIsActiveTrueAndIdNotIn(assignedCourseIds, pageable);
            }
        } else {
            if (assignedCourseIds.isEmpty()) {
                paged = courseRepository.findAvailableCoursesAll(cleanSearch, pageable);
            } else {
                paged = courseRepository.findAvailableCoursesExcluding(assignedCourseIds, cleanSearch, pageable);
            }
        }

        return paged.getContent().stream()
                .map(c -> new AvailableCourseDto(
                        c.getId(),
                        c.getCode(),
                        c.getTitle(),
                        c.getLectureUnits(),
                        c.getLabUnits(),
                        c.getCreditUnits(),
                        c.getContactHoursLec(),
                        c.getContactHoursLab()
                ))
                .toList();
    }

    public void batchRelocatePositions(Long curriculumId, List<RelocateCourseRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        Curriculum curriculum = getCurriculum(curriculumId);
        assertEditable(curriculum);
        List<Long> targetIds = requests.stream()
                .map(RelocateCourseRequest::curriculumCourseId)
                .toList();

        Map<Long, CurriculumCourse> courseMap = curriculumCourseRepository.findAllById(targetIds).stream()
                .collect(Collectors.toMap(CurriculumCourse::getId, cc -> cc));

        for (RelocateCourseRequest req : requests) {
            CurriculumCourse cc = courseMap.get(req.curriculumCourseId());
            if (cc == null) {
                throw new IllegalArgumentException("CurriculumCourse not found with ID: " + req.curriculumCourseId());
            }
            if (!cc.getCurriculum().getId().equals(curriculumId)) {
                throw new IllegalArgumentException("Course ID " + req.curriculumCourseId() + " does not belong to curriculum ID: " + curriculumId);
            }

            cc.relocatePosition(req.targetYearLevel(), req.targetSemester(), req.targetSequenceOrder());
        }

    }

    public void removePrerequisite(Long curriculumId, Long prerequisiteId) {
        Curriculum curriculum = getCurriculum(curriculumId);
        assertEditable(curriculum);

        CoursePrerequisite rule = prerequisiteRepository.findById(prerequisiteId)
                .orElseThrow(() -> new IllegalArgumentException("CoursePrerequisite rule not found with ID: " + prerequisiteId));

        prerequisiteRepository.delete(rule);
    }

    @Transactional(readOnly = true)
    public List<CurriculumSummaryResponse> getCurriculaByProgram(Long programId) {
        return curriculumRepository.findByProgramId(programId).stream()
                .map(this::toSummaryResponse)
                .toList();
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

    private CurriculumSummaryResponse toSummaryResponse(Curriculum c) {
        return new CurriculumSummaryResponse(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getProgram().getCode(),
                c.getEffectiveAcademicYear(),
                c.getStatus().name(),
                c.getVersionNumber()
        );
    }
}