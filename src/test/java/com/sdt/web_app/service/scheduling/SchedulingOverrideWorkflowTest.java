package com.sdt.web_app.service.scheduling;

import com.sdt.web_app.dto.scheduling.SchedulingOverrideDtos.*;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.faculty.FacultyProfile;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.scheduling.*;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.faculty.FacultyProfileRepository;
import com.sdt.web_app.repositories.institution.DepartmentRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import com.sdt.web_app.repositories.scheduling.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulingOverrideWorkflowTest {

    @Mock
    private SchedulingOverrideRequestRepository requestRepository;
    @Mock
    private SchedulingOverrideApprovalRepository approvalRepository;
    @Mock
    private ClassSectionRepository sectionRepository;
    @Mock
    private ClassScheduleRepository scheduleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FacultyProfileRepository facultyProfileRepository;
    @Mock
    private ProgramRepository programRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private FacultyWorkloadRepository workloadRepository;
    @Mock
    private com.sdt.web_app.config.WebSocketBroadcastService broadcastService;

    @InjectMocks
    private SchedulingOverrideService overrideService;

    private Department ccsCollege;
    private Department coeCollege;
    private Program bsitProgram;
    private Program bscsProgram;
    private Program bsedProgram;
    private User facultyUser;
    private FacultyProfile facultyProfile;
    private User chairUser;
    private User deanUser;
    private User registrarUser;
    private ClassSection bsitSection;
    private ClassSection bscsSection;
    private ClassSection bsedSection;
    private ClassSchedule bscsSchedule;

    @BeforeEach
    void setUp() {
        ccsCollege = Department.builder().id(10L).code("CCS").name("College of Computer Studies").deanUserId(102L).build();
        coeCollege = Department.builder().id(20L).code("COE").name("College of Education").deanUserId(106L).build();

        chairUser = User.builder().id(101L).username("bsit_chair").roles(Set.of(Roles.CHAIRPERSON)).build();
        deanUser = User.builder().id(102L).username("ccs_dean").roles(Set.of(Roles.DEAN)).build();
        registrarUser = User.builder().id(103L).username("reg_user").roles(Set.of(Roles.REGISTRAR)).build();

        bsitProgram = Program.builder().id(1L).code("BSIT").name("Information Technology").college(ccsCollege).chairpersonUserId(101L).build();
        bscsProgram = Program.builder().id(2L).code("BSCS").name("Computer Science").college(ccsCollege).chairpersonUserId(104L).build();
        bsedProgram = Program.builder().id(3L).code("BSEd").name("Secondary Education").college(coeCollege).chairpersonUserId(105L).build();

        facultyUser = User.builder().id(201L).username("prof_smith").email("smith@chmsu.edu.ph").roles(Set.of(Roles.FACULTY)).build();
        facultyProfile = FacultyProfile.builder()
                .id(301L)
                .user(facultyUser)
                .program(bsitProgram)
                .college(ccsCollege)
                .firstName("John")
                .lastName("Smith")
                .facultyIdNumber("F-2024-001")
                .build();

        Curriculum bsitCurr = Curriculum.builder().id(11L).code("CURR-BSIT").program(bsitProgram).build();
        Curriculum bscsCurr = Curriculum.builder().id(12L).code("CURR-BSCS").program(bscsProgram).build();
        Curriculum bsedCurr = Curriculum.builder().id(13L).code("CURR-BSED").program(bsedProgram).build();

        Course itCourse = Course.builder().id(51L).code("IT101").title("Intro to IT").lectureUnits(BigDecimal.valueOf(3)).labUnits(BigDecimal.ZERO).build();
        Course csCourse = Course.builder().id(52L).code("CS101").title("Algorithms").lectureUnits(BigDecimal.valueOf(3)).labUnits(BigDecimal.ZERO).build();
        Course edCourse = Course.builder().id(53L).code("ED101").title("Teaching Principles").lectureUnits(BigDecimal.valueOf(3)).labUnits(BigDecimal.ZERO).build();

        Term term = Term.builder().id(99L).build();

        bsitSection = ClassSection.builder().id(501L).sectionCode("BSIT-1A").curriculum(bsitCurr).course(itCourse).term(term).build();
        bscsSection = ClassSection.builder().id(502L).sectionCode("BSCS-1A").curriculum(bscsCurr).course(csCourse).term(term).build();
        bsedSection = ClassSection.builder().id(503L).sectionCode("BSED-1A").curriculum(bsedCurr).course(edCourse).term(term).build();

        Room room = Room.builder().id(801L).code("CL-1").name("Computer Lab 1").capacity(40).build();
        bscsSchedule = ClassSchedule.builder()
                .id(901L)
                .section(bscsSection)
                .room(room)
                .instructor(facultyUser)
                .dayOfWeek("MONDAY")
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .isLocked(false)
                .build();
    }

    @Test
    @DisplayName("Scenario 1: Same Program Assignment requires NO override")
    void testSameProgramNoOverrideRequired() {
        when(sectionRepository.findById(501L)).thenReturn(Optional.of(bsitSection));
        when(userRepository.findById(201L)).thenReturn(Optional.of(facultyUser));
        when(facultyProfileRepository.findByUserId(201L)).thenReturn(Optional.of(facultyProfile));

        OverrideValidationResponse validation = overrideService.validateAssignment(501L, 201L);

        assertFalse(validation.isMismatch());
        assertEquals("NONE", validation.overrideType());
        assertTrue(validation.requiredApprovalSteps().isEmpty());
    }

    @Test
    @DisplayName("Scenario 2: Cross-Program Assignment (BSIT -> BSCS within CCS) requires Chairperson + Faculty approval")
    void testCrossProgramOverrideRequirements() {
        when(sectionRepository.findById(502L)).thenReturn(Optional.of(bscsSection));
        when(userRepository.findById(201L)).thenReturn(Optional.of(facultyUser));
        when(facultyProfileRepository.findByUserId(201L)).thenReturn(Optional.of(facultyProfile));
        when(userRepository.findById(101L)).thenReturn(Optional.of(chairUser));

        OverrideValidationResponse validation = overrideService.validateAssignment(502L, 201L);

        assertTrue(validation.isMismatch());
        assertEquals("CROSS_PROGRAM", validation.overrideType());
        assertEquals(101L, validation.assignedChairpersonId());
        assertNull(validation.assignedDeanId()); // Dean approval not required for cross-program within same college
        assertEquals(2, validation.requiredApprovalSteps().size());
        assertTrue(validation.requiredApprovalSteps().get(0).contains("Chairperson"));
        assertTrue(validation.requiredApprovalSteps().get(1).contains("Faculty Acceptance"));
    }

    @Test
    @DisplayName("Scenario 3: Cross-College Assignment (BSIT -> BSEd) requires Chairperson + Dean + Faculty approval")
    void testCrossCollegeOverrideRequirements() {
        when(sectionRepository.findById(503L)).thenReturn(Optional.of(bsedSection));
        when(userRepository.findById(201L)).thenReturn(Optional.of(facultyUser));
        when(facultyProfileRepository.findByUserId(201L)).thenReturn(Optional.of(facultyProfile));
        when(userRepository.findById(101L)).thenReturn(Optional.of(chairUser));
        when(userRepository.findById(102L)).thenReturn(Optional.of(deanUser));

        OverrideValidationResponse validation = overrideService.validateAssignment(503L, 201L);

        assertTrue(validation.isMismatch());
        assertEquals("CROSS_COLLEGE", validation.overrideType());
        assertEquals(101L, validation.assignedChairpersonId());
        assertEquals(102L, validation.assignedDeanId());
        assertEquals(3, validation.requiredApprovalSteps().size());
    }

    @Test
    @DisplayName("Locking: Creating an override locks the class schedule and creates pending approval steps")
    void testCreateOverrideLocksSchedule() {
        when(sectionRepository.findById(502L)).thenReturn(Optional.of(bscsSection));
        when(scheduleRepository.findById(901L)).thenReturn(Optional.of(bscsSchedule));
        when(userRepository.findById(201L)).thenReturn(Optional.of(facultyUser));
        when(userRepository.findById(103L)).thenReturn(Optional.of(registrarUser));
        when(facultyProfileRepository.findByUserId(201L)).thenReturn(Optional.of(facultyProfile));
        when(userRepository.findById(101L)).thenReturn(Optional.of(chairUser));
        when(userRepository.findById(102L)).thenReturn(Optional.of(deanUser));
        when(programRepository.findById(1L)).thenReturn(Optional.of(bsitProgram));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(ccsCollege));
        when(requestRepository.findBySectionIdAndStatus(502L, OverrideRequestStatus.PENDING_APPROVAL)).thenReturn(List.of());
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SchedulingOverrideResponse response = overrideService.createOverrideRequest(
                502L, 901L, 201L, "Shortage of CS instructors", 103L);

        assertNotNull(response);
        assertEquals("CROSS_PROGRAM", response.overrideType());
        assertEquals("PENDING_APPROVAL", response.status());
        assertTrue(bscsSchedule.isLocked());
        assertEquals("CROSS_PROGRAM", bscsSchedule.getLockReason());
        verify(scheduleRepository).save(bscsSchedule);
    }

    @Test
    @DisplayName("Decision: Unauthorized user cannot approve a step")
    void testUnauthorizedUserCannotApprove() {
        SchedulingOverrideRequest req = SchedulingOverrideRequest.builder()
                .id(1001L)
                .section(bscsSection)
                .schedule(bscsSchedule)
                .facultyUser(facultyUser)
                .requestedByUser(registrarUser)
                .overrideType(OverrideType.CROSS_PROGRAM)
                .targetProgram(bscsProgram)
                .targetCollege(ccsCollege)
                .status(OverrideRequestStatus.PENDING_APPROVAL)
                .justification("Cross-teaching")
                .build();

        SchedulingOverrideApproval step = SchedulingOverrideApproval.builder()
                .id(2001L)
                .request(req)
                .approvalType(ApprovalStepType.CHAIRPERSON_APPROVAL)
                .stepOrder(1)
                .requiredRole("ROLE_CHAIRPERSON")
                .assignedApproverUser(chairUser)
                .status(ApprovalStepStatus.PENDING)
                .build();
        req.addApproval(step);

        when(requestRepository.findByIdWithDetails(1001L)).thenReturn(Optional.of(req));
        User randomFaculty = User.builder().id(999L).username("other_faculty").roles(Set.of(Roles.FACULTY)).build();
        when(userRepository.findById(999L)).thenReturn(Optional.of(randomFaculty));

        assertThrows(AccessDeniedException.class, () ->
                overrideService.approveStep(1001L, 2001L, 999L, "I approve this"));
    }

    @Test
    @DisplayName("Workflow Completion: Final step unlocks schedule after conflict verification")
    void testFinalApprovalUnlocksSchedule() {
        bscsSchedule.lock("CROSS_PROGRAM");
        SchedulingOverrideRequest req = SchedulingOverrideRequest.builder()
                .id(1001L)
                .section(bscsSection)
                .schedule(bscsSchedule)
                .facultyUser(facultyUser)
                .requestedByUser(registrarUser)
                .overrideType(OverrideType.CROSS_PROGRAM)
                .targetProgram(bscsProgram)
                .targetCollege(ccsCollege)
                .status(OverrideRequestStatus.PENDING_APPROVAL)
                .justification("Cross-teaching")
                .build();

        SchedulingOverrideApproval chairStep = SchedulingOverrideApproval.builder()
                .id(2001L)
                .request(req)
                .approvalType(ApprovalStepType.CHAIRPERSON_APPROVAL)
                .stepOrder(1)
                .requiredRole("ROLE_CHAIRPERSON")
                .assignedApproverUser(chairUser)
                .status(ApprovalStepStatus.APPROVED)
                .build();
        req.addApproval(chairStep);

        SchedulingOverrideApproval facultyStep = SchedulingOverrideApproval.builder()
                .id(2002L)
                .request(req)
                .approvalType(ApprovalStepType.FACULTY_ACCEPTANCE)
                .stepOrder(2)
                .requiredRole("ROLE_FACULTY")
                .assignedApproverUser(facultyUser)
                .status(ApprovalStepStatus.PENDING)
                .build();
        req.addApproval(facultyStep);

        when(requestRepository.findByIdWithDetails(1001L)).thenReturn(Optional.of(req));
        when(userRepository.findById(201L)).thenReturn(Optional.of(facultyUser));
        when(scheduleRepository.findActiveByRoomAndDay(any(), any(), any())).thenReturn(List.of());
        when(scheduleRepository.findActiveByInstructorAndDay(any(), any(), any())).thenReturn(List.of());
        when(requestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SchedulingOverrideResponse response = overrideService.approveStep(
                1001L, 2002L, 201L, "I accept this teaching assignment.");

        assertEquals("APPROVED", response.status());
        assertFalse(bscsSchedule.isLocked());
        assertEquals("NONE", bscsSchedule.getLockReason());
        verify(scheduleRepository).save(bscsSchedule);
    }
}
