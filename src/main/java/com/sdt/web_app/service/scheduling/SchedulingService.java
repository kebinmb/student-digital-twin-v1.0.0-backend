package com.sdt.web_app.service.scheduling;

import com.sdt.web_app.dto.scheduling.SchedulingDtos.*;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.institution.Campus;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.scheduling.ClassSchedule;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.entities.scheduling.FacultyWorkload;
import com.sdt.web_app.entities.scheduling.Room;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.institution.CampusRepository;
import com.sdt.web_app.repositories.institution.CourseRepository;
import com.sdt.web_app.repositories.institution.CurriculumCourseRepository;
import com.sdt.web_app.repositories.institution.CurriculumRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.repositories.scheduling.ClassScheduleRepository;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import com.sdt.web_app.repositories.scheduling.FacultyWorkloadRepository;
import com.sdt.web_app.repositories.scheduling.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulingService {

    private final RoomRepository roomRepository;
    private final ClassSectionRepository sectionRepository;
    private final ClassScheduleRepository scheduleRepository;
    private final FacultyWorkloadRepository workloadRepository;
    private final CampusRepository campusRepository;
    private final TermRepository termRepository;
    private final CurriculumRepository curriculumRepository;
    private final CurriculumCourseRepository curriculumCourseRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    // -------------------------------------------------------------------------
    // Room Management
    // -------------------------------------------------------------------------
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        Campus campus = campusRepository.findById(request.campusId())
                .orElseThrow(() -> new EntityNotFoundException("Campus not found with id: " + request.campusId()));

        roomRepository.findByCampusIdAndCode(campus.getId(), request.code().trim()).ifPresent(r -> {
            throw new IllegalStateException("Room with code '" + request.code() + "' already exists in campus " + campus.getName());
        });

        Room room = Room.builder()
                .campus(campus)
                .code(request.code().trim())
                .name(request.name().trim())
                .building(request.building().trim())
                .floor(request.floor())
                .capacity(request.capacity())
                .roomType(Room.RoomType.valueOf(request.roomType().toUpperCase()))
                .isActive(true)
                .build();

        Room saved = roomRepository.save(room);
        return mapToRoomResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findByIsActiveTrue().stream()
                .map(this::mapToRoomResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByCampus(Long campusId) {
        return roomRepository.findByCampusIdAndIsActiveTrue(campusId).stream()
                .map(this::mapToRoomResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Section & Timetable Management (Gate 1 & Gate 2)
    // -------------------------------------------------------------------------
    @Transactional
    public SectionDetailResponse createSection(CreateSectionRequest request) {
        // --- GATE 1: Active Curriculum Lock ---
        Curriculum curriculum = curriculumRepository.findById(request.curriculumId())
                .orElseThrow(() -> new EntityNotFoundException("Curriculum not found with id: " + request.curriculumId()));

        if (curriculum.getStatus() != Curriculum.Status.ACTIVE) {
            throw new IllegalStateException(String.format(
                    "Gate 1 Violation: Cannot schedule section. Curriculum '%s' is in %s status. Only ACTIVE curricula can be scheduled.",
                    curriculum.getCode(), curriculum.getStatus()));
        }

        Term term = termRepository.findById(request.termId())
                .orElseThrow(() -> new EntityNotFoundException("Term not found with id: " + request.termId()));

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + request.courseId()));

        // Verify that course is mapped to the curriculum
        boolean isPrescribed = curriculumCourseRepository.existsByCurriculumIdAndCourseId(curriculum.getId(), course.getId());
        if (!isPrescribed) {
            throw new IllegalArgumentException(String.format(
                    "Gate 2 Violation: Course '%s' is not prescribed in curriculum '%s'.",
                    course.getCode(), curriculum.getCode()));
        }

        // Verify section code uniqueness within (term, course)
        if (sectionRepository.existsByTermIdAndCourseIdAndSectionCode(term.getId(), course.getId(), request.sectionCode().trim())) {
            throw new IllegalStateException(String.format(
                    "Section '%s' already exists for course '%s' in term '%s'.",
                    request.sectionCode(), course.getCode(), term.getTermType().name()));
        }

        // --- GATE 2: Contact Hour Calculation & Schedule Overlap Check ---
        int requiredLecMinutes = (int) (course.getLectureUnits().doubleValue() * 60);
        int requiredLabMinutes = (int) (course.getLabUnits().doubleValue() * 180);
        int scheduledLecMinutes = 0;
        int scheduledLabMinutes = 0;

        BigDecimal maxHours = term.getMaxHoursPerClass() != null ? term.getMaxHoursPerClass() : new BigDecimal("3.0");
        long maxSessionMinutes = maxHours.multiply(BigDecimal.valueOf(60)).longValue();

        List<ScheduleSlotDto> slots = request.scheduleSlots();
        for (ScheduleSlotDto slot : slots) {
            if (!slot.endTime().isAfter(slot.startTime())) {
                throw new IllegalArgumentException("Schedule slot end time must be strictly after start time.");
            }

            int durationMinutes = (int) Duration.between(slot.startTime(), slot.endTime()).toMinutes();
            if (durationMinutes > maxSessionMinutes) {
                throw new IllegalArgumentException(String.format(
                        "Schedule slot duration (%d minutes) exceeds the maximum allowed class duration of %.1f hours (%d minutes) for term '%s'.",
                        durationMinutes, maxHours.doubleValue(), maxSessionMinutes, term.getTermType().name()));
            }

            List<String> effectiveDays = slot.getEffectiveDays();
            if (effectiveDays.isEmpty()) {
                throw new IllegalArgumentException("Schedule slot must have at least one day specified.");
            }

            for (String day : effectiveDays) {
                String dayUpper = day.toUpperCase();
                if ("LABORATORY".equalsIgnoreCase(slot.scheduleType())) {
                    scheduledLabMinutes += durationMinutes;
                } else {
                    scheduledLecMinutes += durationMinutes;
                }

                // Room collision check
                boolean roomOverlap = scheduleRepository.existsOverlappingRoomSchedule(
                        term.getId(), slot.roomId(), dayUpper, slot.startTime(), slot.endTime());
                if (roomOverlap) {
                    Room room = roomRepository.findById(slot.roomId()).orElse(null);
                    String roomCode = room != null ? room.getCode() : String.valueOf(slot.roomId());
                    throw new IllegalStateException(String.format(
                            "Gate 2 Violation: Room collision detected. Room '%s' is already occupied on %s between %s and %s.",
                            roomCode, dayUpper, slot.startTime(), slot.endTime()));
                }

                // Faculty collision check
                if (slot.instructorUserId() != null) {
                    boolean facultyOverlap = scheduleRepository.existsOverlappingFacultySchedule(
                            term.getId(), slot.instructorUserId(), dayUpper, slot.startTime(), slot.endTime());
                    if (facultyOverlap) {
                        User instructor = userRepository.findById(slot.instructorUserId()).orElse(null);
                        String facultyName = instructor != null ? instructor.getUsername() : String.valueOf(slot.instructorUserId());
                        throw new IllegalStateException(String.format(
                            "Gate 2 Violation: Faculty collision detected. Instructor '%s' is already scheduled on %s between %s and %s.",
                            facultyName, dayUpper, slot.startTime(), slot.endTime()));
                    }
                }
            }
        }

        // Verify total contact minutes match CHED requirements
        if (scheduledLecMinutes != requiredLecMinutes || scheduledLabMinutes != requiredLabMinutes) {
            throw new IllegalArgumentException(String.format(
                    "Gate 2 Violation: Scheduled minutes mismatch for course '%s'. Required: Lecture=%dm, Lab=%dm. Scheduled: Lecture=%dm, Lab=%dm.",
                    course.getCode(), requiredLecMinutes, requiredLabMinutes, scheduledLecMinutes, scheduledLabMinutes));
        }

        // Build ClassSection
        ClassSection section = ClassSection.builder()
                .term(term)
                .curriculum(curriculum)
                .course(course)
                .sectionCode(request.sectionCode().trim())
                .maxCapacity(request.maxCapacity())
                .enrolledCount(0)
                .status(ClassSection.Status.OPEN)
                .build();

        // Build & attach schedules
        Set<User> instructorsToUpdate = new LinkedHashSet<>();
        for (ScheduleSlotDto slot : slots) {
            Room room = roomRepository.findById(slot.roomId())
                    .orElseThrow(() -> new EntityNotFoundException("Room not found with id: " + slot.roomId()));

            User instructor = null;
            if (slot.instructorUserId() != null) {
                instructor = userRepository.findById(slot.instructorUserId())
                        .orElseThrow(() -> new EntityNotFoundException("Instructor not found with id: " + slot.instructorUserId()));
                instructorsToUpdate.add(instructor);
            }

            for (String day : slot.getEffectiveDays()) {
                ClassSchedule schedule = ClassSchedule.builder()
                        .room(room)
                        .instructor(instructor)
                        .dayOfWeek(day.toUpperCase())
                        .startTime(slot.startTime())
                        .endTime(slot.endTime())
                        .scheduleType(slot.scheduleType().toUpperCase())
                        .build();

                section.addSchedule(schedule);
            }
        }

        ClassSection saved = sectionRepository.save(section);

        for (User instructor : instructorsToUpdate) {
            updateFacultyWorkload(term, instructor, course);
        }

        return mapToSectionDetail(saved);
    }

    private void updateFacultyWorkload(Term term, User instructor, Course course) {
        FacultyWorkload workload = workloadRepository.findByTermIdAndFacultyId(term.getId(), instructor.getId())
                .orElseGet(() -> FacultyWorkload.builder()
                        .term(term)
                        .faculty(instructor)
                        .regularUnits(BigDecimal.ZERO)
                        .overloadUnits(BigDecimal.ZERO)
                        .totalContactHours(BigDecimal.ZERO)
                        .isOverloadApproved(false)
                        .numberOfPreparations(0)
                        .build());

        long distinctPreps = scheduleRepository.countDistinctCoursesByFacultyAndTerm(instructor.getId(), term.getId());
        int preps = Math.max(1, (int) distinctPreps);
        workload.updatePreparations(preps);

        BigDecimal courseContactHours = course.getLectureUnits()
                .add(course.getLabUnits().multiply(new BigDecimal("3.00")));
        BigDecimal newTotalHours = workload.getTotalContactHours().add(courseContactHours);

        BigDecimal effectiveCap = workload.getEffectiveMaxLoad();
        BigDecimal hardCap = new BigDecimal("24.00");

        if (workload.getCustomMaxLoadUnits() != null) {
            BigDecimal allowableCap = workload.isOverloadApproved()
                    ? workload.getCustomMaxLoadUnits().max(hardCap)
                    : workload.getCustomMaxLoadUnits();
            if (newTotalHours.compareTo(allowableCap) > 0) {
                throw new IllegalStateException(String.format(
                        "Gate 2 Violation: Faculty load limit exceeded for %s. Total contact hours (%.2f hrs) would exceed the custom limit of %.2f hrs/week.",
                        instructor.getUsername(), newTotalHours, allowableCap));
            }
        } else {
            if (workload.isOverloadApproved()) {
                if (newTotalHours.compareTo(hardCap) > 0) {
                    throw new IllegalStateException(String.format(
                            "Gate 2 Violation: Faculty load limit exceeded for %s. Total contact hours (%.2f hrs) would exceed the maximum allowable cap of 24.00 hrs/week.",
                            instructor.getUsername(), newTotalHours));
                }
            } else {
                if (newTotalHours.compareTo(effectiveCap) > 0) {
                    throw new IllegalStateException(String.format(
                            "Gate 2 Violation: Faculty load limit exceeded for %s. Total contact hours (%.2f hrs) would exceed the allowable cap of %.2f hrs/week (%d preparations). Overload approval required.",
                            instructor.getUsername(), newTotalHours, effectiveCap, preps));
                }
            }
        }

        if (newTotalHours.compareTo(effectiveCap) > 0) {
            BigDecimal overload = newTotalHours.subtract(effectiveCap);
            workload.updateWorkload(effectiveCap, overload, newTotalHours);
        } else {
            workload.updateWorkload(newTotalHours, BigDecimal.ZERO, newTotalHours);
        }

        workloadRepository.save(workload);
    }

    @Transactional(readOnly = true)
    public List<SectionDetailResponse> getSectionsByTerm(Long termId) {
        return sectionRepository.findAllWithSchedulesByTermId(termId).stream()
                .map(this::mapToSectionDetail)
                .toList();
    }

    @Transactional(readOnly = true)
    public SectionDetailResponse getSectionById(Long id) {
        ClassSection section = sectionRepository.findByIdWithSchedules(id)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with id: " + id));
        return mapToSectionDetail(section);
    }

    @Transactional(readOnly = true)
    public FacultyLoadSummaryResponse getFacultyWorkload(Long termId, Long facultyUserId) {
        User faculty = userRepository.findById(facultyUserId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty not found with id: " + facultyUserId));
        Term term = termRepository.findById(termId)
                .orElseThrow(() -> new EntityNotFoundException("Term not found with id: " + termId));

        FacultyWorkload workload = workloadRepository.findByTermIdAndFacultyId(termId, facultyUserId).orElse(null);
        List<ClassSchedule> schedules = scheduleRepository.findByInstructorIdAndSectionTermId(facultyUserId, termId);

        List<AssignedSectionDto> assignedSections = schedules.stream()
                .map(ClassSchedule::getSection)
                .distinct()
                .map(sec -> new AssignedSectionDto(
                        sec.getId(),
                        sec.getSectionCode(),
                        sec.getCourse().getCode(),
                        sec.getCourse().getTitle(),
                        sec.getCourse().getCreditUnits(),
                        (int) (sec.getCourse().getLectureUnits().doubleValue() + sec.getCourse().getLabUnits().doubleValue() * 3),
                        sec.getSchedules().stream()
                                .map(s -> s.getDayOfWeek() + " " + s.getStartTime() + "-" + s.getEndTime() + " (" + s.getRoom().getCode() + ")")
                                .reduce((a, b) -> a + "; " + b).orElse("")
                ))
                .toList();

        int preps = workload != null ? workload.getNumberOfPreparations() : (int) scheduleRepository.countDistinctCoursesByFacultyAndTerm(facultyUserId, termId);
        BigDecimal effectiveMax = workload != null ? workload.getEffectiveMaxLoad() : (preps <= 2 ? new BigDecimal("21.00") : new BigDecimal("18.00"));

        return new FacultyLoadSummaryResponse(
                faculty.getId(),
                faculty.getUsername(),
                faculty.getEmail(),
                term.getId(),
                term.getTermType().name(),
                workload != null ? workload.getRegularUnits() : BigDecimal.ZERO,
                workload != null ? workload.getOverloadUnits() : BigDecimal.ZERO,
                workload != null ? workload.getTotalContactHours() : BigDecimal.ZERO,
                workload != null && workload.isOverloadApproved(),
                workload != null && workload.getApprovedBy() != null ? workload.getApprovedBy().getUsername() : null,
                preps,
                workload != null ? workload.getCustomMaxLoadUnits() : null,
                effectiveMax,
                workload != null ? workload.getOverrideReason() : null,
                workload != null && workload.getOverriddenBy() != null ? workload.getOverriddenBy().getUsername() : null,
                assignedSections
        );
    }

    @Transactional
    public void approveFacultyOverload(Long termId, Long facultyUserId, Long approverUserId) {
        User approver = userRepository.findById(approverUserId)
                .orElseThrow(() -> new EntityNotFoundException("Approver user not found: " + approverUserId));
        FacultyWorkload workload = workloadRepository.findByTermIdAndFacultyId(termId, facultyUserId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty workload record not found for faculty " + facultyUserId + " in term " + termId));

        workload.approveOverload(approver);
        workloadRepository.save(workload);
    }

    @Transactional
    public FacultyLoadSummaryResponse updateFacultyWorkloadLimit(
            Long facultyUserId, Long termId, BigDecimal customMaxUnits, String reason, Long adminUserId) {
        User faculty = userRepository.findById(facultyUserId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty not found with id: " + facultyUserId));
        Term term = termRepository.findById(termId)
                .orElseThrow(() -> new EntityNotFoundException("Term not found with id: " + termId));
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new EntityNotFoundException("Admin user not found with id: " + adminUserId));

        FacultyWorkload workload = workloadRepository.findByTermIdAndFacultyId(termId, facultyUserId)
                .orElseGet(() -> FacultyWorkload.builder()
                        .term(term)
                        .faculty(faculty)
                        .regularUnits(BigDecimal.ZERO)
                        .overloadUnits(BigDecimal.ZERO)
                        .totalContactHours(BigDecimal.ZERO)
                        .isOverloadApproved(false)
                        .build());

        int preps = (int) scheduleRepository.countDistinctCoursesByFacultyAndTerm(facultyUserId, termId);
        workload.updatePreparations(preps);
        workload.overrideLoadLimit(customMaxUnits, reason, adminUser);
        workloadRepository.save(workload);

        return getFacultyWorkload(termId, facultyUserId);
    }

    @Transactional
    public SchedulingTermDto updateTermMaxHoursPerClass(Long termId, BigDecimal maxHoursPerClass) {
        Term term = termRepository.findById(termId)
                .orElseThrow(() -> new EntityNotFoundException("Term not found with id: " + termId));
        term.updateMaxHoursPerClass(maxHoursPerClass);
        Term saved = termRepository.save(term);
        return new SchedulingTermDto(
                saved.getId(),
                saved.getAcademicYear().getId(),
                saved.getAcademicYear().getCode(),
                saved.getTermType().name(),
                saved.getAcademicYear().getCode() + " - " + formatTermType(saved.getTermType().name()),
                saved.getAcademicYear().isCurrent(),
                saved.isActive(),
                saved.isEnrollmentOpen(),
                saved.getMaxHoursPerClass() != null ? saved.getMaxHoursPerClass() : new BigDecimal("3.0")
        );
    }

    @Transactional
    public SectionDetailResponse addScheduleSlots(CreateScheduleSlotRequest request) {
        ClassSection section = sectionRepository.findByIdWithSchedules(request.sectionId())
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with id: " + request.sectionId()));

        Term term = section.getTerm();
        BigDecimal maxHours = term.getMaxHoursPerClass() != null ? term.getMaxHoursPerClass() : new BigDecimal("3.0");
        long maxSessionMinutes = maxHours.multiply(BigDecimal.valueOf(60)).longValue();

        long durationMinutes = Duration.between(request.startTime(), request.endTime()).toMinutes();
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("Schedule slot end time must be strictly after start time.");
        }
        if (durationMinutes > maxSessionMinutes) {
            throw new IllegalArgumentException(String.format(
                    "Schedule slot duration (%d minutes) exceeds the maximum allowed class duration of %.1f hours (%d minutes) for term '%s'.",
                    durationMinutes, maxHours.doubleValue(), maxSessionMinutes, term.getTermType().name()));
        }

        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new EntityNotFoundException("Room not found with id: " + request.roomId()));

        User instructor = null;
        if (request.facultyUserId() != null) {
            instructor = userRepository.findById(request.facultyUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Faculty not found with id: " + request.facultyUserId()));
        }

        List<String> days = request.daysOfWeek();
        if (days == null || days.isEmpty()) {
            throw new IllegalArgumentException("At least one day is required.");
        }

        boolean alreadyAssignedToSection = false;
        if (instructor != null) {
            final Long instructorId = instructor.getId();
            alreadyAssignedToSection = section.getSchedules().stream()
                    .anyMatch(s -> s.getInstructor() != null && instructorId.equals(s.getInstructor().getId()));
        }

        for (String day : days) {
            String dayUpper = day.toUpperCase();
            boolean roomOverlap = scheduleRepository.existsOverlappingRoomSchedule(
                    term.getId(), room.getId(), dayUpper, request.startTime(), request.endTime());
            if (roomOverlap) {
                throw new IllegalStateException(String.format(
                        "Gate 2 Violation: Room collision detected. Room '%s' is already occupied on %s between %s and %s.",
                        room.getCode(), dayUpper, request.startTime(), request.endTime()));
            }

            if (instructor != null) {
                boolean facultyOverlap = scheduleRepository.existsOverlappingFacultySchedule(
                        term.getId(), instructor.getId(), dayUpper, request.startTime(), request.endTime());
                if (facultyOverlap) {
                    throw new IllegalStateException(String.format(
                            "Gate 2 Violation: Faculty collision detected. Instructor '%s' is already scheduled on %s between %s and %s.",
                            instructor.getUsername(), dayUpper, request.startTime(), request.endTime()));
                }
            }

            ClassSchedule schedule = ClassSchedule.builder()
                    .room(room)
                    .instructor(instructor)
                    .dayOfWeek(dayUpper)
                    .startTime(request.startTime())
                    .endTime(request.endTime())
                    .scheduleType(request.isLaboratory() ? "LABORATORY" : "LECTURE")
                    .build();

            section.addSchedule(schedule);
        }

        ClassSection saved = sectionRepository.save(section);
        if (instructor != null && !alreadyAssignedToSection) {
            updateFacultyWorkload(term, instructor, section.getCourse());
        }

        return mapToSectionDetail(saved);
    }

    @Transactional(readOnly = true)
    public List<SchedulingTermDto> getSchedulingTerms() {
        return termRepository.findAll().stream()
                .map(t -> new SchedulingTermDto(
                        t.getId(),
                        t.getAcademicYear().getId(),
                        t.getAcademicYear().getCode(),
                        t.getTermType().name(),
                        t.getAcademicYear().getCode() + " - " + formatTermType(t.getTermType().name()),
                        t.getAcademicYear().isCurrent(),
                        t.isActive(),
                        t.isEnrollmentOpen(),
                        t.getMaxHoursPerClass() != null ? t.getMaxHoursPerClass() : new BigDecimal("3.0")
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstructorOptionDto> getAvailableInstructors() {
        return userRepository.findAll().stream()
                .filter(u -> u.isEnabled() && u.getRoles().stream()
                        .anyMatch(r -> r == Roles.FACULTY || r == Roles.CHAIRPERSON || r == Roles.DEAN || r == Roles.ADMIN))
                .map(u -> new InstructorOptionDto(u.getId(), u.getUsername(), u.getEmail()))
                .toList();
    }

    private String formatTermType(String termType) {
        if ("FIRST_SEM".equalsIgnoreCase(termType) || "1ST_SEM".equalsIgnoreCase(termType)) {
            return "1st Semester";
        }
        if ("SECOND_SEM".equalsIgnoreCase(termType) || "2ND_SEM".equalsIgnoreCase(termType)) {
            return "2nd Semester";
        }
        if ("SUMMER".equalsIgnoreCase(termType)) {
            return "Summer Term";
        }
        return termType;
    }

    // -------------------------------------------------------------------------
    // Mappers
    // -------------------------------------------------------------------------
    private RoomResponse mapToRoomResponse(Room r) {
        return new RoomResponse(
                r.getId(),
                r.getCampus().getId(),
                r.getCampus().getName(),
                r.getCode(),
                r.getName(),
                r.getBuilding(),
                r.getFloor(),
                r.getCapacity(),
                r.getRoomType().name(),
                r.isActive()
        );
    }

    private SectionDetailResponse mapToSectionDetail(ClassSection sec) {
        List<ScheduleSlotResponse> slotResponses = sec.getSchedules().stream()
                .map(s -> new ScheduleSlotResponse(
                        s.getId(),
                        s.getRoom().getId(),
                        s.getRoom().getCode(),
                        s.getRoom().getName(),
                        s.getInstructor() != null ? s.getInstructor().getId() : null,
                        s.getInstructor() != null ? s.getInstructor().getUsername() : "Unassigned",
                        s.getDayOfWeek(),
                        s.getStartTime(),
                        s.getEndTime(),
                        s.getScheduleType()
                ))
                .toList();

        return new SectionDetailResponse(
                sec.getId(),
                sec.getTerm().getId(),
                sec.getTerm().getTermType().name(),
                sec.getCurriculum().getId(),
                sec.getCurriculum().getCode(),
                sec.getCurriculum().getName(),
                sec.getCourse().getId(),
                sec.getCourse().getCode(),
                sec.getCourse().getTitle(),
                sec.getCourse().getLectureUnits(),
                sec.getCourse().getLabUnits(),
                sec.getCourse().getCreditUnits(),
                sec.getSectionCode(),
                sec.getMaxCapacity(),
                sec.getEnrolledCount(),
                sec.getStatus().name(),
                slotResponses
        );
    }
}
