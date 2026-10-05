package com.sdt.web_app.service.enrollment;

import com.sdt.web_app.dto.enrollment.EnrollmentDtos.*;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.CoursePrerequisite;
import com.sdt.web_app.entities.institution.CurriculumCourse;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.enrollment.*;
import com.sdt.web_app.entities.scheduling.ClassSchedule;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.institution.CoursePrerequisiteRepository;
import com.sdt.web_app.repositories.institution.CurriculumCourseRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.repositories.enrollment.*;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import com.sdt.web_app.entities.compliance.ClearanceRequest;
import com.sdt.web_app.repositories.compliance.ClearanceRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sdt.web_app.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final StudentProfileRepository studentProfileRepository;
    private final StudentService studentService;
    private final StudentCourseGradeRepository studentCourseGradeRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final EnrollmentCourseItemRepository enrollmentItemRepository;
    private final ClassSectionRepository sectionRepository;
    private final CurriculumCourseRepository curriculumCourseRepository;
    private final CoursePrerequisiteRepository prerequisiteRepository;
    private final com.sdt.web_app.service.institution.TermService termService;
    private final ClearanceRequestRepository clearanceRequestRepository;

    // -------------------------------------------------------------------------
    // Gate 3: Student Advising & Eligibility Evaluation
    // -------------------------------------------------------------------------
    @Transactional
    public AdvisingEligibilityResponse getAdvisingEligibility(Long studentId, Long termId) {
        return getAdvisingEligibility(studentId, termId, null, null, false);
    }

    @Transactional
    public AdvisingEligibilityResponse getAdvisingEligibility(
            Long studentId, Long termId, Integer targetYearLevel, String targetSemester) {
        return getAdvisingEligibility(studentId, termId, targetYearLevel, targetSemester, false);
    }

    @Transactional
    public AdvisingEligibilityResponse getAdvisingEligibility(
            Long studentId, Long termId, Integer targetYearLevel, String targetSemester, Boolean allCourses) {
        final Long resolvedStudentId;
        if (studentId != null && studentId < 0) {
            resolvedStudentId = studentService.createStudentFromAdmissionAppId(-studentId).id();
        } else {
            resolvedStudentId = studentId;
        }

        StudentProfile student = studentProfileRepository.findByIdWithProgramAndCurriculum(resolvedStudentId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found with id: " + resolvedStudentId));

        Term term = termService.getTermById(termId);

        // 1. Fetch student's historical passed grades
        List<StudentCourseGrade> passedGrades = studentCourseGradeRepository.findPassedGradesByStudentId(student.getId());
        Map<Long, StudentCourseGrade> passedCourseMap = passedGrades.stream()
                .collect(Collectors.toMap(g -> g.getCourse().getId(), g -> g, (g1, g2) -> g1));

        // 2. Fetch current enrollment items for term (if existing)
        Optional<StudentEnrollment> existingEnrollmentOpt = studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(student.getId(), term.getId());
        Set<Long> currentlyEnrolledCourseIds = existingEnrollmentOpt.map(se -> se.getItems().stream()
                .map(item -> item.getSection().getCourse().getId())
                .collect(Collectors.toSet())).orElse(Collections.emptySet());

        BigDecimal currentEnrolledUnits = existingEnrollmentOpt
                .map(StudentEnrollment::getTotalCreditUnits)
                .orElse(BigDecimal.ZERO);

        // 3. Fetch prescribed curriculum courses
        List<CurriculumCourse> curriculumCourses = curriculumCourseRepository.findByCurriculumId(student.getCurriculum().getId());

        // 4. Determine student's target academic period (next year level & semester)
        AcademicPeriod targetPeriod = determineTargetPeriod(
                student, term, curriculumCourses, passedCourseMap.keySet(), targetYearLevel, targetSemester);
        boolean hasPeriodMatches = curriculumCourses.stream().anyMatch(cc -> matchesPeriod(cc, targetPeriod));

        // 5. Batch fetch prerequisites for all curriculum courses
        List<Long> allCourseIds = curriculumCourses.stream().map(cc -> cc.getCourse().getId()).distinct().toList();
        List<CoursePrerequisite> allPrerequisites = prerequisiteRepository.findPrerequisitesForCourseIds(allCourseIds);
        Map<Long, List<CoursePrerequisite>> prereqMap = allPrerequisites.stream()
                .collect(Collectors.groupingBy(cp -> cp.getCourse().getId()));

        // 6. Fetch all OPEN sections for this term
        List<ClassSection> termSections = sectionRepository.findAllWithSchedulesByTermId(term.getId());
        Map<Long, List<ClassSection>> sectionsByCourseId = termSections.stream()
                .filter(s -> s.getStatus() == ClassSection.Status.OPEN)
                .collect(Collectors.groupingBy(s -> s.getCourse().getId()));

        // 7. Evaluate eligibility and filter courses for the next academic period & passed history
        List<CourseEligibilityItemDto> courseEligibilityList = new ArrayList<>();
        for (CurriculumCourse cc : curriculumCourses) {
            Course course = cc.getCourse();
            boolean isPassed = passedCourseMap.containsKey(course.getId());
            boolean isEnrolled = currentlyEnrolledCourseIds.contains(course.getId());
            boolean isInTargetPeriod = Boolean.TRUE.equals(allCourses) || !hasPeriodMatches || matchesPeriod(cc, targetPeriod);

            if (!isPassed && !isEnrolled && !isInTargetPeriod) {
                continue;
            }

            List<CoursePrerequisite> prerequisites = prereqMap.getOrDefault(course.getId(), Collections.emptyList());

            List<PrerequisiteDetailDto> prereqDetails = new ArrayList<>();
            boolean allPrereqsSatisfied = true;
            StringBuilder failureReason = new StringBuilder();

            for (CoursePrerequisite cp : prerequisites) {
                Course requiredCourse = cp.getPrerequisiteCourse();
                StudentCourseGrade grade = passedCourseMap.get(requiredCourse.getId());
                boolean isSatisfied = grade != null && grade.isPassed();
                boolean isHard = !"CO_REQUISITE".equalsIgnoreCase(cp.getRuleType());

                if (isHard && !isSatisfied) {
                    allPrereqsSatisfied = false;
                    if (!failureReason.isEmpty()) failureReason.append("; ");
                    failureReason.append("Missing prerequisite: ")
                            .append(requiredCourse.getCode())
                            .append(" (")
                            .append(requiredCourse.getTitle())
                            .append(")");
                }

                prereqDetails.add(new PrerequisiteDetailDto(
                        requiredCourse.getId(),
                        requiredCourse.getCode(),
                        requiredCourse.getTitle(),
                        cp.getMinGradeRequired() != null ? new BigDecimal(cp.getMinGradeRequired()) : new BigDecimal("3.00"),
                        isSatisfied,
                        grade != null ? grade.getNumericalGrade() : null
                ));
            }

            String status;
            if (isPassed) {
                status = "ALREADY_PASSED";
            } else if (isEnrolled) {
                status = "CURRENTLY_ENROLLED";
            } else if (!allPrereqsSatisfied) {
                status = "LOCKED_PREREQUISITE";
            } else {
                status = "ELIGIBLE";
            }

            List<ClassSection> availableSections = sectionsByCourseId.getOrDefault(course.getId(), Collections.emptyList());
            List<AvailableSectionOptionDto> sectionDtos = availableSections.stream()
                    .map(sec -> new AvailableSectionOptionDto(
                            sec.getId(),
                            sec.getSectionCode(),
                            sec.getMaxCapacity(),
                            sec.getEnrolledCount(),
                            sec.getStatus().name(),
                            sec.getSchedules().stream()
                                    .map(s -> s.getDayOfWeek() + " " + s.getStartTime() + "-" + s.getEndTime() + " (" + s.getRoom().getCode() + ")")
                                    .reduce((a, b) -> a + "; " + b).orElse("No schedule")
                    ))
                    .toList();

            courseEligibilityList.add(new CourseEligibilityItemDto(
                    course.getId(),
                    course.getCode(),
                    course.getTitle(),
                    course.getLectureUnits(),
                    course.getLabUnits(),
                    course.getCreditUnits(),
                    cc.getYearLevel(),
                    cc.getSemester(),
                    status,
                    failureReason.toString(),
                    prereqDetails,
                    sectionDtos
            ));
        }

        // Determine max allowed units
        boolean isSummer = "SUMMER".equalsIgnoreCase(term.getTermType().name()) || "MIDYEAR".equalsIgnoreCase(term.getTermType().name());
        BigDecimal maxAllowedUnits;
        if (student.isGraduating() && existingEnrollmentOpt.map(StudentEnrollment::isOverloadApproved).orElse(false)) {
            maxAllowedUnits = isSummer ? new BigDecimal("12.00") : new BigDecimal("27.00");
        } else {
            maxAllowedUnits = isSummer ? new BigDecimal("9.00") : new BigDecimal("24.00");
        }

        return new AdvisingEligibilityResponse(
                student.getId(),
                student.getStudentNumber(),
                student.getUser().getUsername(),
                student.getProgram().getCode(),
                student.getProgram().getName(),
                student.getCurriculum().getCode(),
                student.getYearLevel(),
                student.getEnrollmentStatus().name(),
                student.isGraduating(),
                student.getTotalUnitsEarned(),
                student.getCumulativeGpa(),
                maxAllowedUnits,
                currentEnrolledUnits,
                student.getFinancialClearance() != null ? student.getFinancialClearance().name() : "CLEARED",
                student.getDepartmentalClearance() != null ? student.getDepartmentalClearance().name() : "CLEARED",
                checkMultiDepartmentClearanceStatus(student, term.getId()),
                courseEligibilityList
        );
    }

    // -------------------------------------------------------------------------
    // Gate 3: Course Enlistment & Atomic Capacity Check
    // -------------------------------------------------------------------------
    @Transactional
    public StudentEnrollmentResponse enlistSection(Long studentId, EnlistSectionRequest request) {
        final Long resolvedStudentId;
        if (studentId != null && studentId < 0) {
            resolvedStudentId = studentService.createStudentFromAdmissionAppId(-studentId).id();
        } else {
            resolvedStudentId = studentId;
        }

        StudentProfile student = studentProfileRepository.findByIdWithProgramAndCurriculum(resolvedStudentId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found with id: " + resolvedStudentId));

        Term term = termService.getTermById(request.termId());
        if (!term.isEnrollmentOpen()) {
            throw new IllegalStateException("Enrollment period for term '" + term.getName() + "' is currently closed.");
        }

        ClassSection section = sectionRepository.findByIdWithSchedules(request.sectionId())
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with id: " + request.sectionId()));

        verifyMultiDepartmentClearanceGate(student, term.getId());

        if (section.getStatus() != ClassSection.Status.OPEN) {
            throw new IllegalStateException(String.format(
                    "Gate 3 Violation: Section '%s' is %s and cannot accept new enrollments.",
                    section.getSectionCode(), section.getStatus()));
        }

        Course course = section.getCourse();

        // 1. Verify student has passed all prerequisites
        List<CoursePrerequisite> prerequisites = prerequisiteRepository.findByCourseId(course.getId());
        List<StudentCourseGrade> studentGrades = studentCourseGradeRepository.findPassedGradesByStudentId(student.getId());
        Map<Long, StudentCourseGrade> gradeMap = studentGrades.stream()
                .collect(Collectors.toMap(g -> g.getCourse().getId(), g -> g, (a, b) -> a));
        for (CoursePrerequisite cp : prerequisites) {
            if ("CO_REQUISITE".equalsIgnoreCase(cp.getRuleType())) {
                continue;
            }
            StudentCourseGrade grade = gradeMap.get(cp.getPrerequisiteCourse().getId());
            if (grade == null || !grade.isPassed()) {
                throw new IllegalStateException(String.format(
                        "Gate 3 Violation: Missing prerequisite '%s' (%s) required for course '%s'.",
                        cp.getPrerequisiteCourse().getCode(), cp.getPrerequisiteCourse().getTitle(), course.getCode()));
            }

            BigDecimal minThreshold = cp.getMinGradeRequired() != null
                    ? new BigDecimal(cp.getMinGradeRequired())
                    : new BigDecimal("3.00");

            if (grade.getNumericalGrade() != null && grade.getNumericalGrade().compareTo(minThreshold) > 0) {
                throw new IllegalStateException(String.format(
                        "Gate 3 Violation: Prerequisite '%s' requires a minimum grade of %.2f, but student achieved %.2f.",
                        cp.getPrerequisiteCourse().getCode(), minThreshold, grade.getNumericalGrade()));
            }
        }

        // 2. Fetch or create StudentEnrollment for term
        StudentEnrollment enrollment = studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(student.getId(), term.getId())
                .orElseGet(() -> {
                    StudentEnrollment newEnrollment = StudentEnrollment.builder()
                            .student(student)
                            .term(term)
                            .status(StudentEnrollment.Status.DRAFT)
                            .totalCreditUnits(BigDecimal.ZERO)
                            .isOverloadApproved(false)
                            .build();
                    return studentEnrollmentRepository.save(newEnrollment);
                });

        boolean alreadyEnrolledInCourse = enrollment.getItems().stream()
                .anyMatch(item -> item.getSection().getCourse().getId().equals(course.getId()));
        if (alreadyEnrolledInCourse) {
            throw new IllegalStateException(String.format(
                    "Student is already enrolled in a section for course '%s'.", course.getCode()));
        }
        for (ClassSchedule newSlot : section.getSchedules()) {
            for (EnrollmentCourseItem existingItem : enrollment.getItems()) {
                for (ClassSchedule existingSlot : existingItem.getSection().getSchedules()) {
                    if (existingSlot.getDayOfWeek().equalsIgnoreCase(newSlot.getDayOfWeek())) {
                        boolean overlaps = newSlot.getStartTime().isBefore(existingSlot.getEndTime())
                                && existingSlot.getStartTime().isBefore(newSlot.getEndTime());
                        if (overlaps) {
                            throw new IllegalStateException(String.format(
                                    "Gate 3 Violation: Schedule collision detected. Course '%s' (%s %s-%s) conflicts with already enlisted course '%s' (%s %s-%s).",
                                    course.getCode(), newSlot.getDayOfWeek(), newSlot.getStartTime(), newSlot.getEndTime(),
                                    existingItem.getSection().getCourse().getCode(), existingSlot.getDayOfWeek(), existingSlot.getStartTime(), existingSlot.getEndTime()));
                        }
                    }
                }
            }
        }
        // 3. Unit Cap Verification
        boolean isSummer = "SUMMER".equalsIgnoreCase(term.getTermType().name()) || "MIDYEAR".equalsIgnoreCase(term.getTermType().name());
        BigDecimal maxUnits = isSummer ? new BigDecimal("9.00") : new BigDecimal("24.00");
        if (student.isGraduating() && enrollment.isOverloadApproved()) {
            maxUnits = isSummer ? new BigDecimal("12.00") : new BigDecimal("27.00");
        }

        BigDecimal newTotalUnits = enrollment.getTotalCreditUnits().add(course.getCreditUnits());
        if (newTotalUnits.compareTo(maxUnits) > 0) {
            throw new IllegalStateException(String.format(
                    "Gate 3 Violation: Enlisting course '%s' (%.2f units) exceeds the term credit ceiling of %.2f units (running: %.2f units).",
                    course.getCode(), course.getCreditUnits(), maxUnits, newTotalUnits));
        }

        // 4. Atomic capacity increment using conditional JPQL
        int updatedRows = sectionRepository.incrementEnrolledCountIfOpen(section.getId());
        if (updatedRows == 0) {
            throw new IllegalStateException(String.format(
                    "Gate 3 Violation: Section '%s' reached maximum capacity (%d) or closed concurrently during enrollment.",
                    section.getSectionCode(), section.getMaxCapacity()));
        }

        // 5. Create EnrollmentCourseItem
        EnrollmentCourseItem item = EnrollmentCourseItem.builder()
                .enrollment(enrollment)
                .section(section)
                .completionStatus(EnrollmentCourseItem.CompletionStatus.ENROLLED)
                .build();

        enrollment.addItem(item);
        enrollment.recalculateUnits();
        enrollment.updateStatus(StudentEnrollment.Status.ENLISTED);

        StudentEnrollment saved = studentEnrollmentRepository.save(enrollment);
        return mapToEnrollmentResponse(saved);
    }

    @Transactional
    public StudentEnrollmentResponse removeEnlistedSection(Long studentId, Long termId, Long sectionId) {
        final Long resolvedStudentId = studentId != null && studentId < 0 ? studentService.createStudentFromAdmissionAppId(-studentId).id() : studentId;
        StudentEnrollment enrollment = studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(resolvedStudentId, termId)
                .orElseThrow(() -> new EntityNotFoundException("Enrollment record not found for student " + resolvedStudentId + " in term " + termId));

        Term term = termService.getTermById(termId);
        if (!term.isEnrollmentOpen() && !term.isAddDropOpen()) {
            throw new IllegalStateException("Enrollment and add/drop period for term '" + term.getName() + "' is currently closed.");
        }

        EnrollmentCourseItem itemToRemove = enrollment.getItems().stream()
                .filter(item -> item.getSection().getId().equals(sectionId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Section " + sectionId + " is not enlisted in current enrollment"));

        enrollment.removeItem(itemToRemove);
        enrollmentItemRepository.delete(itemToRemove);
        enrollment.recalculateUnits();

        sectionRepository.decrementEnrolledCount(sectionId);

        StudentEnrollment saved = studentEnrollmentRepository.save(enrollment);
        return mapToEnrollmentResponse(saved);
    }

    @Transactional
    @CacheEvict(value = {CacheConfig.CACHE_PROGRAMS, CacheConfig.CACHE_EQUITY_PROFILES}, allEntries = true)
    public EnrollmentConfirmationDto confirmEnrollment(Long studentId, ConfirmEnrollmentRequest request) {
        final Long resolvedStudentId = studentId != null && studentId < 0 ? studentService.createStudentFromAdmissionAppId(-studentId).id() : studentId;
        StudentProfile student = studentProfileRepository.findById(resolvedStudentId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found with id: " + resolvedStudentId));

        verifyMultiDepartmentClearanceGate(student, request.termId());

        Term term = termService.getTermById(request.termId());
        if (!term.isEnrollmentOpen()) {
            throw new IllegalStateException("Enrollment period for term '" + term.getName() + "' is currently closed.");
        }

        StudentEnrollment enrollment = studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(resolvedStudentId, request.termId())
                .orElseThrow(() -> new EntityNotFoundException("Enrollment record not found for student " + resolvedStudentId + " in term " + request.termId()));

        if (enrollment.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot confirm enrollment: no courses have been enlisted.");
        }

        // Gate 3: Enforce co-requisite validation for all enlisted courses
        Set<Long> enlistedCourseIds = enrollment.getItems().stream()
                .map(item -> item.getSection().getCourse().getId())
                .collect(Collectors.toSet());

        List<StudentCourseGrade> studentGrades = studentCourseGradeRepository.findPassedGradesByStudentId(student.getId());
        Set<Long> passedCourseIds = studentGrades.stream()
                .filter(StudentCourseGrade::isPassed)
                .map(g -> g.getCourse().getId())
                .collect(Collectors.toSet());

        for (EnrollmentCourseItem item : enrollment.getItems()) {
            Course currentCourse = item.getSection().getCourse();
            List<CoursePrerequisite> prereqs = prerequisiteRepository.findByCourseId(currentCourse.getId());
            for (CoursePrerequisite cp : prereqs) {
                if ("CO_REQUISITE".equalsIgnoreCase(cp.getRuleType())) {
                    Long coreqCourseId = cp.getPrerequisiteCourse().getId();
                    boolean isSatisfied = passedCourseIds.contains(coreqCourseId) || enlistedCourseIds.contains(coreqCourseId);
                    if (!isSatisfied) {
                        throw new IllegalStateException(String.format(
                                "Gate 3 Violation: Missing mandatory co-requisite '%s' (%s) required for course '%s'. Co-requisites must be either previously passed or concurrently enlisted in the same term.",
                                cp.getPrerequisiteCourse().getCode(), cp.getPrerequisiteCourse().getTitle(), currentCourse.getCode()));
                    }
                }
            }
        }

        enrollment.updateStatus(StudentEnrollment.Status.ENROLLED);
        studentEnrollmentRepository.save(enrollment);

        return new EnrollmentConfirmationDto(
                enrollment.getId(),
                enrollment.getStatus().name(),
                enrollment.getTotalCreditUnits(),
                "Enrollment successfully confirmed for term: " + enrollment.getTerm().getTermType().name()
        );
    }

    @Transactional(readOnly = true)
    public StudentEnrollmentResponse getEnrollment(Long studentId, Long termId) {
        if (studentId == null) {
            return new StudentEnrollmentResponse(
                    null, null, null, termId, "UNENROLLED", null, "NOT_ENROLLED",
                    BigDecimal.ZERO, false, Collections.emptyList()
            );
        }

        final Long resolvedStudentId;
        if (studentId < 0) {
            Optional<Long> existingIdOpt = studentService.findExistingStudentProfileIdFromAdmissionAppId(-studentId);
            if (existingIdOpt.isPresent()) {
                resolvedStudentId = existingIdOpt.get();
            } else {
                return new StudentEnrollmentResponse(
                        null,
                        studentId,
                        null,
                        termId,
                        "UNENROLLED",
                        null,
                        "NOT_ENROLLED",
                        BigDecimal.ZERO,
                        false,
                        Collections.emptyList()
                );
            }
        } else {
            resolvedStudentId = studentId;
        }

        return studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(resolvedStudentId, termId)
                .map(this::mapToEnrollmentResponse)
                .orElseGet(() -> new StudentEnrollmentResponse(
                        null,
                        resolvedStudentId,
                        null,
                        termId,
                        "UNENROLLED",
                        null,
                        "NOT_ENROLLED",
                        BigDecimal.ZERO,
                        false,
                        Collections.emptyList()
                ));
    }

    // -------------------------------------------------------------------------
    // Registrar / Administrative Enrollment Oversight
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<StudentEnrollmentResponse> getEnrollmentsByTerm(Long termId) {
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByTermIdWithDetails(termId);
        return enrollments.stream().map(this::mapToEnrollmentResponse).toList();
    }

    @Transactional
    public StudentEnrollmentResponse updateEnrollmentStatus(Long enrollmentId, UpdateEnrollmentStatusRequest request) {
        StudentEnrollment enrollment = studentEnrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException("Student enrollment record not found with id: " + enrollmentId));

        if (request.status() != null && !request.status().isBlank()) {
            try {
                StudentEnrollment.Status newStatus = StudentEnrollment.Status.valueOf(request.status().toUpperCase());
                enrollment.updateStatus(newStatus);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid enrollment status specified: " + request.status());
            }
        }

        if (request.isOverloadApproved() != null) {
            enrollment.setOverloadApproved(request.isOverloadApproved());
        }

        StudentEnrollment saved = studentEnrollmentRepository.save(enrollment);
        return mapToEnrollmentResponse(saved);
    }

    // -------------------------------------------------------------------------
    // Mappers
    // -------------------------------------------------------------------------
    private StudentEnrollmentResponse mapToEnrollmentResponse(StudentEnrollment se) {
        List<EnrollmentItemResponse> itemResponses = se.getItems().stream()
                .map(item -> new EnrollmentItemResponse(
                        item.getId(),
                        item.getSection().getId(),
                        item.getSection().getSectionCode(),
                        item.getSection().getCourse().getCode(),
                        item.getSection().getCourse().getTitle(),
                        item.getSection().getCourse().getCreditUnits(),
                        item.getSection().getSchedules().stream()
                                .map(s -> s.getDayOfWeek() + " " + s.getStartTime() + "-" + s.getEndTime() + " (" + s.getRoom().getCode() + ")")
                                .reduce((a, b) -> a + "; " + b).orElse(""),
                        item.getCompletionStatus().name(),
                        item.getFinalNumericalGrade()
                ))
                .toList();

        return new StudentEnrollmentResponse(
                se.getId(),
                se.getStudent().getId(),
                se.getStudent().getStudentNumber(),
                se.getTerm().getId(),
                se.getTerm().getTermType().name(),
                se.getEnrollmentDate(),
                se.getStatus().name(),
                se.getTotalCreditUnits(),
                se.isOverloadApproved(),
                itemResponses
        );
    }

    // -------------------------------------------------------------------------
    // Advising Academic Period Progression Helpers
    // -------------------------------------------------------------------------
    public record AcademicPeriod(int yearLevel, String semester) {
    }

    private AcademicPeriod determineTargetPeriod(
            StudentProfile student,
            Term term,
            List<CurriculumCourse> curriculumCourses,
            Set<Long> passedCourseIds,
            Integer requestedYearLevel,
            String requestedSemester) {

        if (requestedYearLevel != null && requestedYearLevel > 0 && requestedSemester != null && !requestedSemester.isBlank()) {
            return new AcademicPeriod(requestedYearLevel, normalizeSemester(requestedSemester));
        }

        int maxPassedRank = 0;
        for (CurriculumCourse cc : curriculumCourses) {
            if (passedCourseIds.contains(cc.getCourse().getId())) {
                int rank = getTermRank(cc.getYearLevel(), cc.getSemester());
                if (rank > maxPassedRank) {
                    maxPassedRank = rank;
                }
            }
        }

        if (maxPassedRank > 0) {
            int nextRank = Math.min(maxPassedRank + 1, 8);
            return getPeriodFromRank(nextRank);
        }

        int year = student.getYearLevel() > 0 ? student.getYearLevel() : 1;
        String sem = "1ST_SEM";
        if (term != null && term.getTermType() != null) {
            sem = switch (term.getTermType()) {
                case SECOND_SEM, SECOND_SEMESTER -> "2ND_SEM";
                case SUMMER -> "SUMMER";
                default -> "1ST_SEM";
            };
        }

        return new AcademicPeriod(year, sem);
    }

    private int getTermRank(int yearLevel, String semester) {
        int semIndex = isSecondSem(semester) ? 2 : 1;
        return (yearLevel - 1) * 2 + semIndex;
    }

    private AcademicPeriod getPeriodFromRank(int rank) {
        int r = Math.max(1, Math.min(rank, 8));
        int yearLevel = ((r - 1) / 2) + 1;
        String semester = ((r - 1) % 2 == 0) ? "1ST_SEM" : "2ND_SEM";
        return new AcademicPeriod(yearLevel, semester);
    }

    private String normalizeSemester(String semester) {
        if (isSecondSem(semester)) return "2ND_SEM";
        if (isSummer(semester)) return "SUMMER";
        return "1ST_SEM";
    }

    private boolean isFirstSem(String semester) {
        if (semester == null) return false;
        String s = semester.toUpperCase();
        return s.contains("1ST") || s.contains("FIRST") || s.equals("1");
    }

    private boolean isSecondSem(String semester) {
        if (semester == null) return false;
        String s = semester.toUpperCase();
        return s.contains("2ND") || s.contains("SECOND") || s.equals("2");
    }

    private boolean isSummer(String semester) {
        if (semester == null) return false;
        String s = semester.toUpperCase();
        return s.contains("SUMMER") || s.contains("MIDYEAR");
    }

    private boolean matchesPeriod(CurriculumCourse cc, AcademicPeriod targetPeriod) {
        if (cc.getYearLevel() != targetPeriod.yearLevel()) {
            return false;
        }
        return isSemesterMatch(cc.getSemester(), targetPeriod.semester());
    }

    private boolean isSemesterMatch(String semA, String semB) {
        if (semA == null || semB == null) return false;
        if (semA.equalsIgnoreCase(semB)) return true;
        if (isFirstSem(semA) && isFirstSem(semB)) return true;
        if (isSecondSem(semA) && isSecondSem(semB)) return true;
        if (isSummer(semA) && isSummer(semB)) return true;
        return false;
    }

    // -------------------------------------------------------------------------
    // Gate 3: Multi-Department Clearance Verification
    // -------------------------------------------------------------------------
    public boolean checkMultiDepartmentClearanceStatus(StudentProfile student, Long termId) {
        if (!student.isClearedForEnrollment()) {
            return false;
        }
        Optional<ClearanceRequest> clearanceOpt = clearanceRequestRepository.findByStudentProfileIdAndTermId(student.getId(), termId);
        if (clearanceOpt.isEmpty()) {
            List<ClearanceRequest> list = clearanceRequestRepository.findByStudentProfileId(student.getId());
            if (!list.isEmpty()) {
                clearanceOpt = Optional.of(list.get(0));
            }
        }
        if (clearanceOpt.isPresent()) {
            ClearanceRequest cr = clearanceOpt.get();
            if (!"CLEARED".equalsIgnoreCase(cr.getOverallStatus())) {
                return false;
            }
            if (cr.getSignoffs() == null || cr.getSignoffs().isEmpty()) {
                return false;
            }
            return cr.getSignoffs().stream()
                    .allMatch(s -> "APPROVED".equalsIgnoreCase(s.getSignoffStatus()));
        }
        return true;
    }

    public void verifyMultiDepartmentClearanceGate(StudentProfile student, Long termId) {
        if (!student.isClearedForEnrollment()) {
            throw new IllegalStateException(String.format(
                    "Clearance Violation: Multi-Department Clearance Gate Blocked: Student '%s' has uncleared profile status (Financial: %s, Departmental: %s).",
                    student.getStudentNumber(), student.getFinancialClearance(), student.getDepartmentalClearance()));
        }
        Optional<ClearanceRequest> clearanceOpt = clearanceRequestRepository.findByStudentProfileIdAndTermId(student.getId(), termId);
        if (clearanceOpt.isEmpty()) {
            List<ClearanceRequest> list = clearanceRequestRepository.findByStudentProfileId(student.getId());
            if (!list.isEmpty()) {
                clearanceOpt = Optional.of(list.get(0));
            }
        }
        if (clearanceOpt.isPresent()) {
            ClearanceRequest cr = clearanceOpt.get();
            if (!"CLEARED".equalsIgnoreCase(cr.getOverallStatus())) {
                throw new IllegalStateException(String.format(
                        "Clearance Violation: Multi-Department Clearance Gate Blocked: Student '%s' clearance request status is '%s' (Required: CLEARED for all departments).",
                        student.getStudentNumber(), cr.getOverallStatus()));
            }
            boolean allApproved = cr.getSignoffs() != null && !cr.getSignoffs().isEmpty() &&
                    cr.getSignoffs().stream().allMatch(s -> "APPROVED".equalsIgnoreCase(s.getSignoffStatus()));
            if (!allApproved) {
                throw new IllegalStateException(String.format(
                        "Clearance Violation: Multi-Department Clearance Gate Blocked: Student '%s' has pending or rejected department sign-offs.",
                        student.getStudentNumber()));
            }
        }
    }
}
