package com.sdt.web_app.service.scheduling;

import com.sdt.web_app.dto.scheduling.SchedulingDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.institution.AcademicYear;
import com.sdt.web_app.entities.institution.Campus;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.institution.TermType;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SchedulingServiceTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private ClassSectionRepository sectionRepository;
    @Mock
    private ClassScheduleRepository scheduleRepository;
    @Mock
    private FacultyWorkloadRepository workloadRepository;
    @Mock
    private CampusRepository campusRepository;
    @Mock
    private TermRepository termRepository;
    @Mock
    private com.sdt.web_app.service.institution.TermService termService;
    @Mock
    private CurriculumRepository curriculumRepository;
    @Mock
    private CurriculumCourseRepository curriculumCourseRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private com.sdt.web_app.service.security.AcademicScopeAssertionService academicScopeAssertionService;

    @InjectMocks
    private SchedulingService schedulingService;

    private Curriculum activeCurriculum;
    private Curriculum draftCurriculum;
    private Term term;
    private Course course;
    private Room room;
    private User instructor;

    @BeforeEach
    void setUp() {
        activeCurriculum = Curriculum.builder()
                .code("BSIT-2026")
                .name("BSIT Curriculum")
                .status(Curriculum.Status.ACTIVE)
                .effectiveAcademicYear("2026-2027")
                .build();
        ReflectionTestUtils.setField(activeCurriculum, "id", 1L);

        draftCurriculum = Curriculum.builder()
                .code("BSIT-DRAFT")
                .name("BSIT Draft Curriculum")
                .status(Curriculum.Status.DRAFT)
                .effectiveAcademicYear("2026-2027")
                .build();
        ReflectionTestUtils.setField(draftCurriculum, "id", 2L);

        AcademicYear ay = AcademicYear.builder().code("AY 2026-2027").build();
        term = Term.builder()
                .academicYear(ay)
                .termType(TermType.FIRST_SEM)
                .build();
        ReflectionTestUtils.setField(term, "id", 10L);

        course = Course.builder()
                .code("IT 101")
                .title("Introduction to Computing")
                .lectureUnits(new BigDecimal("2.00"))
                .labUnits(new BigDecimal("1.00"))
                .creditUnits(new BigDecimal("3.00"))
                .contactHoursLec(2)
                .contactHoursLab(3)
                .build();
        ReflectionTestUtils.setField(course, "id", 100L);

        Campus campus = Campus.builder().code("TAL").name("Talisay Main").build();
        ReflectionTestUtils.setField(campus, "id", 5L);

        room = Room.builder()
                .campus(campus)
                .code("LAB-1")
                .name("Computer Lab 1")
                .building("Tech Wing")
                .capacity(40)
                .roomType(Room.RoomType.LABORATORY)
                .build();
        ReflectionTestUtils.setField(room, "id", 20L);

        instructor = User.builder()
                .username("faculty_alice")
                .email("alice@chmsu.edu.ph")
                .build();
        ReflectionTestUtils.setField(instructor, "id", 30L);
        org.mockito.Mockito.lenient().when(termService.getTermById(any())).thenReturn(term);
    }

    @Test
    @DisplayName("Gate 1: Should block section creation if curriculum is not in ACTIVE status")
    void shouldBlockSectionCreationForNonActiveCurriculum() {
        given(curriculumRepository.findById(2L)).willReturn(Optional.of(draftCurriculum));

        CreateSectionRequest request = new CreateSectionRequest(
                10L, 2L, 100L, "BSIT-1A", 40,
                List.of(new ScheduleSlotDto(20L, 30L, "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0), "LECTURE"))
        );

        assertThatThrownBy(() -> schedulingService.createSection(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Gate 1 Violation");
    }

    @Test
    @DisplayName("Gate 2: Should reject section if course is not prescribed in active curriculum")
    void shouldRejectOffCurriculumCourse() {
        given(curriculumRepository.findById(1L)).willReturn(Optional.of(activeCurriculum));
        given(courseRepository.findById(100L)).willReturn(Optional.of(course));
        given(curriculumCourseRepository.existsByCurriculumIdAndCourseId(1L, 100L)).willReturn(false);

        CreateSectionRequest request = new CreateSectionRequest(
                10L, 1L, 100L, "BSIT-1A", 40,
                List.of(new ScheduleSlotDto(20L, 30L, "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0), "LECTURE"))
        );

        assertThatThrownBy(() -> schedulingService.createSection(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Gate 2 Violation: Course 'IT 101' is not prescribed");
    }

    @Test
    @DisplayName("Gate 2: Should reject section if scheduled minutes mismatch CHED requirements")
    void shouldRejectWhenScheduledMinutesMismatch() {
        // Course has 2.00 lec (120 min) and 1.00 lab (180 min)
        given(curriculumRepository.findById(1L)).willReturn(Optional.of(activeCurriculum));
        given(courseRepository.findById(100L)).willReturn(Optional.of(course));
        given(curriculumCourseRepository.existsByCurriculumIdAndCourseId(1L, 100L)).willReturn(true);
        given(sectionRepository.existsByTermIdAndCourseIdAndSectionCode(10L, 100L, "BSIT-1A")).willReturn(false);

        // Schedule only 60 min lecture instead of 120 min
        CreateSectionRequest request = new CreateSectionRequest(
                10L, 1L, 100L, "BSIT-1A", 40,
                List.of(
                        new ScheduleSlotDto(20L, 30L, "MONDAY", LocalTime.of(8, 0), LocalTime.of(9, 0), "LECTURE"),
                        new ScheduleSlotDto(20L, 30L, "WEDNESDAY", LocalTime.of(8, 0), LocalTime.of(11, 0), "LABORATORY")
                )
        );

        assertThatThrownBy(() -> schedulingService.createSection(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Scheduled minutes mismatch");
    }

    @Test
    @DisplayName("Gate 2: Should detect and block room schedule collision")
    void shouldDetectRoomCollision() {
        given(curriculumRepository.findById(1L)).willReturn(Optional.of(activeCurriculum));
        given(courseRepository.findById(100L)).willReturn(Optional.of(course));
        given(curriculumCourseRepository.existsByCurriculumIdAndCourseId(1L, 100L)).willReturn(true);
        given(sectionRepository.existsByTermIdAndCourseIdAndSectionCode(10L, 100L, "BSIT-1A")).willReturn(false);

        // Room overlap is true
        given(scheduleRepository.existsOverlappingRoomSchedule(10L, 20L, "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0)))
                .willReturn(true);
        given(roomRepository.findById(20L)).willReturn(Optional.of(room));

        CreateSectionRequest request = new CreateSectionRequest(
                10L, 1L, 100L, "BSIT-1A", 40,
                List.of(
                        new ScheduleSlotDto(20L, 30L, "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0), "LECTURE"),
                        new ScheduleSlotDto(20L, 30L, "WEDNESDAY", LocalTime.of(8, 0), LocalTime.of(11, 0), "LABORATORY")
                )
        );

        assertThatThrownBy(() -> schedulingService.createSection(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Room collision detected");
    }

    @Test
    @DisplayName("Gate 2: Should detect and block faculty schedule collision")
    void shouldDetectFacultyCollision() {
        given(curriculumRepository.findById(1L)).willReturn(Optional.of(activeCurriculum));
        given(courseRepository.findById(100L)).willReturn(Optional.of(course));
        given(curriculumCourseRepository.existsByCurriculumIdAndCourseId(1L, 100L)).willReturn(true);
        given(sectionRepository.existsByTermIdAndCourseIdAndSectionCode(10L, 100L, "BSIT-1A")).willReturn(false);

        // Room overlap false, but faculty overlap true
        given(scheduleRepository.existsOverlappingRoomSchedule(10L, 20L, "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0)))
                .willReturn(false);
        given(scheduleRepository.existsOverlappingFacultySchedule(10L, 30L, "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0)))
                .willReturn(true);
        given(userRepository.findById(30L)).willReturn(Optional.of(instructor));

        CreateSectionRequest request = new CreateSectionRequest(
                10L, 1L, 100L, "BSIT-1A", 40,
                List.of(
                        new ScheduleSlotDto(20L, 30L, "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0), "LECTURE"),
                        new ScheduleSlotDto(20L, 30L, "WEDNESDAY", LocalTime.of(8, 0), LocalTime.of(11, 0), "LABORATORY")
                )
        );

        assertThatThrownBy(() -> schedulingService.createSection(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Faculty collision detected");
    }

    @Test
    @DisplayName("Gate 2: Should block faculty assignment when workload cap (>24 hrs/wk) is exceeded")
    void shouldBlockWorkloadCapExceeded() {
        given(curriculumRepository.findById(1L)).willReturn(Optional.of(activeCurriculum));
        given(courseRepository.findById(100L)).willReturn(Optional.of(course));
        given(curriculumCourseRepository.existsByCurriculumIdAndCourseId(1L, 100L)).willReturn(true);
        given(sectionRepository.existsByTermIdAndCourseIdAndSectionCode(10L, 100L, "BSIT-1A")).willReturn(false);

        given(scheduleRepository.existsOverlappingRoomSchedule(any(), any(), any(), any(), any())).willReturn(false);
        given(scheduleRepository.existsOverlappingFacultySchedule(any(), any(), any(), any(), any())).willReturn(false);
        given(roomRepository.findById(20L)).willReturn(Optional.of(room));
        given(userRepository.findById(30L)).willReturn(Optional.of(instructor));

        // Existing workload already at 21.0 hrs, course adds 2 lec + 3 lab = 5 hrs -> 26.0 hrs (> 24.0 hard cap)
        FacultyWorkload existingWorkload = FacultyWorkload.builder()
                .term(term)
                .faculty(instructor)
                .totalContactHours(new BigDecimal("21.00"))
                .regularUnits(new BigDecimal("21.00"))
                .overloadUnits(BigDecimal.ZERO)
                .build();
        given(workloadRepository.findByTermIdAndFacultyId(10L, 30L)).willReturn(Optional.of(existingWorkload));

        CreateSectionRequest request = new CreateSectionRequest(
                10L, 1L, 100L, "BSIT-1A", 40,
                List.of(
                        new ScheduleSlotDto(20L, 30L, "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0), "LECTURE"),
                        new ScheduleSlotDto(20L, 30L, "WEDNESDAY", LocalTime.of(8, 0), LocalTime.of(11, 0), "LABORATORY")
                )
        );

        assertThatThrownBy(() -> schedulingService.createSection(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Faculty load limit exceeded");
    }

    @Test
    @DisplayName("Gate 2: Should successfully create section and schedules when all constraints are met")
    void shouldCreateSectionSuccessfully() {
        given(curriculumRepository.findById(1L)).willReturn(Optional.of(activeCurriculum));
        given(courseRepository.findById(100L)).willReturn(Optional.of(course));
        given(curriculumCourseRepository.existsByCurriculumIdAndCourseId(1L, 100L)).willReturn(true);
        given(sectionRepository.existsByTermIdAndCourseIdAndSectionCode(10L, 100L, "BSIT-1A")).willReturn(false);

        given(scheduleRepository.existsOverlappingRoomSchedule(any(), any(), any(), any(), any())).willReturn(false);
        given(scheduleRepository.existsOverlappingFacultySchedule(any(), any(), any(), any(), any())).willReturn(false);
        given(roomRepository.findById(20L)).willReturn(Optional.of(room));
        given(userRepository.findById(30L)).willReturn(Optional.of(instructor));

        given(workloadRepository.findByTermIdAndFacultyId(10L, 30L)).willReturn(Optional.empty());
        given(sectionRepository.save(any(ClassSection.class))).willAnswer(invocation -> {
            ClassSection sec = invocation.getArgument(0);
            ReflectionTestUtils.setField(sec, "id", 500L);
            return sec;
        });

        CreateSectionRequest request = new CreateSectionRequest(
                10L, 1L, 100L, "BSIT-1A", 40,
                List.of(
                        new ScheduleSlotDto(20L, 30L, "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0), "LECTURE"),
                        new ScheduleSlotDto(20L, 30L, "WEDNESDAY", LocalTime.of(8, 0), LocalTime.of(11, 0), "LABORATORY")
                )
        );

        SectionDetailResponse response = schedulingService.createSection(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.sectionCode()).isEqualTo("BSIT-1A");
        assertThat(response.status()).isEqualTo("OPEN");
        assertThat(response.schedules()).hasSize(2);
    }

    @Test
    @DisplayName("Gate 2: Should reject schedule slot exceeding term max class hours")
    void shouldRejectScheduleSlotExceedingMaxHoursPerClass() {
        given(curriculumRepository.findById(1L)).willReturn(Optional.of(activeCurriculum));
        given(courseRepository.findById(100L)).willReturn(Optional.of(course));
        given(curriculumCourseRepository.existsByCurriculumIdAndCourseId(1L, 100L)).willReturn(true);
        given(sectionRepository.existsByTermIdAndCourseIdAndSectionCode(10L, 100L, "BSIT-1A")).willReturn(false);

        // 4 hours slot (8:00 to 12:00) exceeds term's default 3.0 hours limit
        CreateSectionRequest request = new CreateSectionRequest(
                10L, 1L, 100L, "BSIT-1A", 40,
                List.of(
                        new ScheduleSlotDto(20L, 30L, "MONDAY", LocalTime.of(8, 0), LocalTime.of(12, 0), "LECTURE")
                )
        );

        assertThatThrownBy(() -> schedulingService.createSection(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds the maximum allowed class duration");
    }

    @Test
    @DisplayName("Gate 2: Should support multi-day recurring schedule slots")
    void shouldCreateSectionWithMultiDaySlot() {
        given(curriculumRepository.findById(1L)).willReturn(Optional.of(activeCurriculum));
        given(courseRepository.findById(100L)).willReturn(Optional.of(course));
        given(curriculumCourseRepository.existsByCurriculumIdAndCourseId(1L, 100L)).willReturn(true);
        given(sectionRepository.existsByTermIdAndCourseIdAndSectionCode(10L, 100L, "BSIT-1A")).willReturn(false);

        given(scheduleRepository.existsOverlappingRoomSchedule(any(), any(), any(), any(), any())).willReturn(false);
        given(scheduleRepository.existsOverlappingFacultySchedule(any(), any(), any(), any(), any())).willReturn(false);
        given(roomRepository.findById(20L)).willReturn(Optional.of(room));
        given(userRepository.findById(30L)).willReturn(Optional.of(instructor));

        given(workloadRepository.findByTermIdAndFacultyId(10L, 30L)).willReturn(Optional.empty());
        given(sectionRepository.save(any(ClassSection.class))).willAnswer(invocation -> {
            ClassSection sec = invocation.getArgument(0);
            ReflectionTestUtils.setField(sec, "id", 600L);
            return sec;
        });

        // 2.00 lec units = 120 min -> MONDAY & WEDNESDAY 8:00-9:00 (60 min x 2 = 120 min)
        // 1.00 lab units = 180 min -> FRIDAY 8:00-11:00 (180 min)
        CreateSectionRequest request = new CreateSectionRequest(
                10L, 1L, 100L, "BSIT-1A", 40,
                List.of(
                        new ScheduleSlotDto(20L, 30L, null, List.of("MONDAY", "WEDNESDAY"), LocalTime.of(8, 0), LocalTime.of(9, 0), "LECTURE"),
                        new ScheduleSlotDto(20L, 30L, "FRIDAY", null, LocalTime.of(8, 0), LocalTime.of(11, 0), "LABORATORY")
                )
        );

        SectionDetailResponse response = schedulingService.createSection(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(600L);
        // Total slots created should be 3: 2 for MONDAY+WEDNESDAY and 1 for FRIDAY
        assertThat(response.schedules()).hasSize(3);
    }

    @Test
    @DisplayName("Gate 2: Should enforce 18.0 units cap when instructor preparations exceed 2")
    void shouldEnforce18UnitsLimitWhenPreparationsExceedTwo() {
        given(curriculumRepository.findById(1L)).willReturn(Optional.of(activeCurriculum));
        given(courseRepository.findById(100L)).willReturn(Optional.of(course));
        given(curriculumCourseRepository.existsByCurriculumIdAndCourseId(1L, 100L)).willReturn(true);
        given(sectionRepository.existsByTermIdAndCourseIdAndSectionCode(10L, 100L, "BSIT-1A")).willReturn(false);

        given(scheduleRepository.existsOverlappingRoomSchedule(any(), any(), any(), any(), any())).willReturn(false);
        given(scheduleRepository.existsOverlappingFacultySchedule(any(), any(), any(), any(), any())).willReturn(false);
        given(roomRepository.findById(20L)).willReturn(Optional.of(room));
        given(userRepository.findById(30L)).willReturn(Optional.of(instructor));

        // Existing workload at 15.0 hrs, distinct preps = 3 (> 2, so cap is 18.00)
        // New course adds 2 lec + 3 lab = 5 hrs -> 15 + 5 = 20 hrs (> 18.0 cap)
        FacultyWorkload existingWorkload = FacultyWorkload.builder()
                .term(term)
                .faculty(instructor)
                .totalContactHours(new BigDecimal("15.00"))
                .regularUnits(new BigDecimal("15.00"))
                .overloadUnits(BigDecimal.ZERO)
                .numberOfPreparations(3)
                .isOverloadApproved(false)
                .build();
        given(workloadRepository.findByTermIdAndFacultyId(10L, 30L)).willReturn(Optional.of(existingWorkload));
        given(scheduleRepository.countDistinctCoursesByFacultyAndTerm(30L, 10L)).willReturn(3L);

        CreateSectionRequest request = new CreateSectionRequest(
                10L, 1L, 100L, "BSIT-1A", 40,
                List.of(
                        new ScheduleSlotDto(20L, 30L, "MONDAY", LocalTime.of(8, 0), LocalTime.of(10, 0), "LECTURE"),
                        new ScheduleSlotDto(20L, 30L, "WEDNESDAY", LocalTime.of(8, 0), LocalTime.of(11, 0), "LABORATORY")
                )
        );

        assertThatThrownBy(() -> schedulingService.createSection(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("3 preparations")
                .hasMessageContaining("18.00 hrs/week");
    }

    @Test
    @DisplayName("Admin: Should update faculty custom load limit override")
    void shouldAllowAdminToOverrideFacultyLoadLimit() {
        User admin = User.builder().username("admin_john").build();
        ReflectionTestUtils.setField(admin, "id", 1L);

        given(userRepository.findById(30L)).willReturn(Optional.of(instructor));
        given(userRepository.findById(1L)).willReturn(Optional.of(admin));

        FacultyWorkload workload = FacultyWorkload.builder()
                .term(term)
                .faculty(instructor)
                .totalContactHours(BigDecimal.ZERO)
                .regularUnits(BigDecimal.ZERO)
                .overloadUnits(BigDecimal.ZERO)
                .build();
        given(workloadRepository.findByTermIdAndFacultyId(10L, 30L)).willReturn(Optional.of(workload));
        given(scheduleRepository.findByInstructorIdAndSectionTermId(30L, 10L)).willReturn(List.of());

        FacultyLoadSummaryResponse response = schedulingService.updateFacultyWorkloadLimit(
                30L, 10L, new BigDecimal("25.00"), "Research coordinator override", 1L);

        assertThat(response).isNotNull();
        assertThat(response.customMaxLoadUnits()).isEqualTo(new BigDecimal("25.00"));
        assertThat(response.effectiveMaxUnits()).isEqualTo(new BigDecimal("25.00"));
        assertThat(response.overrideReason()).isEqualTo("Research coordinator override");
        assertThat(response.overriddenByName()).isEqualTo("admin_john");
    }

    @Test
    @DisplayName("Gate 2: Should not duplicate faculty contact hours when adding slots to an already assigned section")
    void shouldNotDuplicateWorkloadWhenAddingSlotToAlreadyAssignedSection() {
        ClassSection section = ClassSection.builder()
                .term(term)
                .curriculum(activeCurriculum)
                .course(course)
                .sectionCode("BSIT-1A")
                .maxCapacity(40)
                .build();
        ReflectionTestUtils.setField(section, "id", 500L);

        ClassSchedule existingSchedule = ClassSchedule.builder()
                .room(room)
                .instructor(instructor)
                .dayOfWeek("MONDAY")
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .scheduleType("LECTURE")
                .build();
        section.addSchedule(existingSchedule);

        given(sectionRepository.findByIdWithSchedules(500L)).willReturn(Optional.of(section));
        given(roomRepository.findById(20L)).willReturn(Optional.of(room));
        given(userRepository.findById(30L)).willReturn(Optional.of(instructor));
        given(scheduleRepository.existsOverlappingRoomSchedule(any(), any(), any(), any(), any())).willReturn(false);
        given(scheduleRepository.existsOverlappingFacultySchedule(any(), any(), any(), any(), any())).willReturn(false);
        given(sectionRepository.save(any(ClassSection.class))).willReturn(section);

        CreateScheduleSlotRequest request = new CreateScheduleSlotRequest(
                500L, 100L, 30L, 20L, List.of("WEDNESDAY"), LocalTime.of(8, 0), LocalTime.of(11, 0), true
        );

        SectionDetailResponse response = schedulingService.addScheduleSlots(request);

        assertThat(response).isNotNull();
        org.mockito.Mockito.verify(workloadRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Query: Should get sections by term with batched schedules and zero Cartesian explosion")
    void shouldGetSectionsByTermWithBatchedSchedules() {
        ClassSection section1 = ClassSection.builder()
                .term(term)
                .curriculum(activeCurriculum)
                .course(course)
                .sectionCode("BSIT-1A")
                .maxCapacity(40)
                .build();
        ReflectionTestUtils.setField(section1, "id", 101L);

        ClassSchedule schedule1 = ClassSchedule.builder()
                .section(section1)
                .room(room)
                .instructor(instructor)
                .dayOfWeek("MONDAY")
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .scheduleType("LECTURE")
                .build();
        ReflectionTestUtils.setField(schedule1, "id", 201L);

        given(sectionRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .willReturn(List.of(section1));
        given(scheduleRepository.findBySectionIdInWithRoomAndInstructor(List.of(101L)))
                .willReturn(List.of(schedule1));

        List<SectionDetailResponse> result = schedulingService.getSectionsByTerm(10L);

        assertThat(result).hasSize(1);
        SectionDetailResponse dto = result.get(0);
        assertThat(dto.id()).isEqualTo(101L);
        assertThat(dto.sectionCode()).isEqualTo("BSIT-1A");
        assertThat(dto.schedules()).hasSize(1);
        assertThat(dto.schedules().get(0).roomCode()).isEqualTo("LAB-1");
        assertThat(dto.schedules().get(0).instructorName()).isEqualTo("faculty_alice");
    }

    @Test
    @DisplayName("Query: Should get section by ID with eagerly fetched details and schedules")
    void shouldGetSectionByIdWithDetailsAndSchedules() {
        ClassSection section1 = ClassSection.builder()
                .term(term)
                .curriculum(activeCurriculum)
                .course(course)
                .sectionCode("BSIT-1B")
                .maxCapacity(35)
                .build();
        ReflectionTestUtils.setField(section1, "id", 102L);

        ClassSchedule schedule1 = ClassSchedule.builder()
                .section(section1)
                .room(room)
                .instructor(instructor)
                .dayOfWeek("TUESDAY")
                .startTime(LocalTime.of(13, 0))
                .endTime(LocalTime.of(16, 0))
                .scheduleType("LABORATORY")
                .build();
        ReflectionTestUtils.setField(schedule1, "id", 202L);

        given(sectionRepository.findByIdWithDetails(102L)).willReturn(Optional.of(section1));
        given(scheduleRepository.findBySectionIdInWithRoomAndInstructor(List.of(102L)))
                .willReturn(List.of(schedule1));

        SectionDetailResponse dto = schedulingService.getSectionById(102L);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(102L);
        assertThat(dto.sectionCode()).isEqualTo("BSIT-1B");
        assertThat(dto.schedules()).hasSize(1);
        assertThat(dto.schedules().get(0).scheduleType()).isEqualTo("LABORATORY");
    }
}
