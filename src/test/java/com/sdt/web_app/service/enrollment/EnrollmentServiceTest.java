package com.sdt.web_app.service.enrollment;

import com.sdt.web_app.dto.enrollment.EnrollmentDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.enrollment.StudentCourseGrade;
import com.sdt.web_app.entities.enrollment.StudentEnrollment;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.*;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.enrollment.EnrollmentCourseItemRepository;
import com.sdt.web_app.repositories.enrollment.StudentCourseGradeRepository;
import com.sdt.web_app.repositories.enrollment.StudentEnrollmentRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.CoursePrerequisiteRepository;
import com.sdt.web_app.repositories.institution.CurriculumCourseRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import com.sdt.web_app.repositories.compliance.ClearanceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private StudentService studentService;
    @Mock
    private StudentCourseGradeRepository studentCourseGradeRepository;
    @Mock
    private StudentEnrollmentRepository studentEnrollmentRepository;
    @Mock
    private EnrollmentCourseItemRepository enrollmentItemRepository;
    @Mock
    private ClassSectionRepository sectionRepository;
    @Mock
    private CurriculumCourseRepository curriculumCourseRepository;
    @Mock
    private CoursePrerequisiteRepository prerequisiteRepository;
    @Mock
    private com.sdt.web_app.service.institution.TermService termService;
    @Mock
    private ClearanceRequestRepository clearanceRequestRepository;
    @Mock
    private com.sdt.web_app.service.scheduling.SectionEventPublisherService sectionEventPublisherService;
    @Mock
    private com.sdt.web_app.service.lms.StudentNotificationPublisherService studentNotificationPublisherService;
    @Mock
    private com.sdt.web_app.config.WebSocketBroadcastService broadcastService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private StudentProfile student;
    private Term term;
    private Course course1;
    private Course course2;
    private ClassSection openSection;
    private ClassSection closedSection;

    @BeforeEach
    void setUp() {
        Program program = Program.builder().code("BSIT").name("BS Information Technology").build();
        ReflectionTestUtils.setField(program, "id", 1L);

        Curriculum curriculum = Curriculum.builder().code("BSIT-2026").status(Curriculum.Status.ACTIVE).build();
        ReflectionTestUtils.setField(curriculum, "id", 10L);

        User studentUser = User.builder().username("student_john").email("john@chmsu.edu.ph").build();
        ReflectionTestUtils.setField(studentUser, "id", 100L);

        student = StudentProfile.builder()
                .user(studentUser)
                .studentNumber("2026-IT-0001")
                .program(program)
                .curriculum(curriculum)
                .yearLevel(2)
                .enrollmentStatus(StudentProfile.EnrollmentStatus.REGULAR)
                .isGraduating(false)
                .totalUnitsEarned(new BigDecimal("26.00"))
                .build();
        ReflectionTestUtils.setField(student, "id", 50L);

        AcademicYear ay = AcademicYear.builder().code("AY 2026-2027").build();
        term = Term.builder()
                .academicYear(ay)
                .termType(TermType.FIRST_SEM)
                .enrollmentOpen(true)
                .addDropOpen(true)
                .build();
        ReflectionTestUtils.setField(term, "id", 20L);

        course1 = Course.builder()
                .code("IT 101")
                .title("Introduction to Computing")
                .lectureUnits(new BigDecimal("2.00"))
                .labUnits(new BigDecimal("1.00"))
                .creditUnits(new BigDecimal("3.00"))
                .build();
        ReflectionTestUtils.setField(course1, "id", 101L);

        course2 = Course.builder()
                .code("IT 102")
                .title("Computer Programming 1")
                .lectureUnits(new BigDecimal("2.00"))
                .labUnits(new BigDecimal("1.00"))
                .creditUnits(new BigDecimal("3.00"))
                .build();
        ReflectionTestUtils.setField(course2, "id", 102L);

        openSection = ClassSection.builder()
                .term(term)
                .curriculum(curriculum)
                .course(course2)
                .sectionCode("BSIT-1A")
                .maxCapacity(40)
                .enrolledCount(10)
                .status(ClassSection.Status.OPEN)
                .build();
        ReflectionTestUtils.setField(openSection, "id", 200L);

        closedSection = ClassSection.builder()
                .term(term)
                .curriculum(curriculum)
                .course(course2)
                .sectionCode("BSIT-1B")
                .maxCapacity(40)
                .enrolledCount(40)
                .status(ClassSection.Status.CLOSED)
                .build();
        ReflectionTestUtils.setField(closedSection, "id", 201L);
    }

    @Test
    @DisplayName("Gate 3: Advising should lock course with unfulfilled prerequisite")
    void shouldLockCourseWithUnfulfilledPrerequisite() {
        given(studentProfileRepository.findByIdWithProgramAndCurriculum(50L)).willReturn(Optional.of(student));
        given(termService.getTermById(20L)).willReturn(term);

        // Student has NOT passed course1 (IT 101)
        given(studentCourseGradeRepository.findPassedGradesByStudentId(50L)).willReturn(Collections.emptyList());
        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(50L, 20L)).willReturn(Optional.empty());

        CurriculumCourse cc = CurriculumCourse.builder()
                .curriculum(student.getCurriculum())
                .course(course2)
                .yearLevel(1)
                .semester("2ND_SEM")
                .sequenceOrder(1)
                .build();
        given(curriculumCourseRepository.findByCurriculumId(10L))
                .willReturn(List.of(cc));

        // course2 requires course1 as prerequisite
        CoursePrerequisite prereq = CoursePrerequisite.builder()
                .course(course2)
                .prerequisiteCourse(course1)
                .minGradeRequired("3.00")
                .build();
        given(prerequisiteRepository.findPrerequisitesForCourseIds(List.of(102L)))
                .willReturn(List.of(prereq));
        given(sectionRepository.findAllWithSchedulesByTermId(20L)).willReturn(Collections.emptyList());

        AdvisingEligibilityResponse response = enrollmentService.getAdvisingEligibility(50L, 20L);

        assertThat(response).isNotNull();
        assertThat(response.courses()).hasSize(1);
        CourseEligibilityItemDto courseDto = response.courses().get(0);
        assertThat(courseDto.eligibilityStatus()).isEqualTo("LOCKED_PREREQUISITE");
        assertThat(courseDto.failureReason()).contains("Missing prerequisite: IT 101");
    }

    @Test
    @DisplayName("Gate 3: Advising should filter eligible courses to next semester (Year 1 - 2nd Sem) and retain passed courses")
    void shouldFilterEligibleCoursesToNextSemesterAndRetainPassed() {
        given(studentProfileRepository.findByIdWithProgramAndCurriculum(50L)).willReturn(Optional.of(student));
        given(termService.getTermById(20L)).willReturn(term);

        // Student has PASSED course1 (IT 101 - Year 1, 1ST_SEM)
        StudentCourseGrade passedGrade = StudentCourseGrade.builder()
                .student(student)
                .course(course1)
                .numericalGrade(new BigDecimal("1.50"))
                .completionStatus("PASSED")
                .isCredited(true)
                .build();
        given(studentCourseGradeRepository.findPassedGradesByStudentId(50L)).willReturn(List.of(passedGrade));
        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(50L, 20L)).willReturn(Optional.empty());

        // Curriculum has 3 courses:
        // 1) course1: Year 1, 1ST_SEM (PASSED)
        // 2) course2: Year 1, 2ND_SEM (ELIGIBLE)
        // 3) course3: Year 2, 1ST_SEM (Future year - should be FILTERED OUT)
        Course course3 = Course.builder()
                .code("IT 201")
                .title("Advanced Database Systems")
                .lectureUnits(new BigDecimal("2.00"))
                .labUnits(new BigDecimal("1.00"))
                .creditUnits(new BigDecimal("3.00"))
                .build();
        ReflectionTestUtils.setField(course3, "id", 103L);

        CurriculumCourse cc1 = CurriculumCourse.builder()
                .curriculum(student.getCurriculum()).course(course1).yearLevel(1).semester("1ST_SEM").build();
        CurriculumCourse cc2 = CurriculumCourse.builder()
                .curriculum(student.getCurriculum()).course(course2).yearLevel(1).semester("2ND_SEM").build();
        CurriculumCourse cc3 = CurriculumCourse.builder()
                .curriculum(student.getCurriculum()).course(course3).yearLevel(2).semester("1ST_SEM").build();

        given(curriculumCourseRepository.findByCurriculumId(10L)).willReturn(List.of(cc1, cc2, cc3));
        given(prerequisiteRepository.findPrerequisitesForCourseIds(any())).willReturn(Collections.emptyList());
        given(sectionRepository.findAllWithSchedulesByTermId(20L)).willReturn(Collections.emptyList());

        AdvisingEligibilityResponse response = enrollmentService.getAdvisingEligibility(50L, 20L);

        assertThat(response).isNotNull();
        // course3 from Year 2 must be filtered out! Only course1 (ALREADY_PASSED) and course2 (ELIGIBLE) are retained.
        assertThat(response.courses()).hasSize(2);

        List<String> statuses = response.courses().stream().map(CourseEligibilityItemDto::eligibilityStatus).toList();
        assertThat(statuses).containsExactlyInAnyOrder("ALREADY_PASSED", "ELIGIBLE");

        CourseEligibilityItemDto eligibleCourse = response.courses().stream()
                .filter(c -> c.eligibilityStatus().equals("ELIGIBLE"))
                .findFirst().orElseThrow();
        assertThat(eligibleCourse.yearLevel()).isEqualTo(1);
        assertThat(eligibleCourse.semester()).isEqualTo("2ND_SEM");
        assertThat(eligibleCourse.code()).isEqualTo("IT 102");
    }

    @Test
    @DisplayName("Gate 3: Advising with allCourses=true should retain all curriculum courses across all years")
    void shouldRetainAllCurriculumCoursesWhenAllCoursesIsTrue() {
        given(studentProfileRepository.findByIdWithProgramAndCurriculum(50L)).willReturn(Optional.of(student));
        given(termService.getTermById(20L)).willReturn(term);

        StudentCourseGrade passedGrade = StudentCourseGrade.builder()
                .student(student)
                .course(course1)
                .numericalGrade(new BigDecimal("1.50"))
                .completionStatus("PASSED")
                .isCredited(true)
                .build();
        given(studentCourseGradeRepository.findPassedGradesByStudentId(50L)).willReturn(List.of(passedGrade));
        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(50L, 20L)).willReturn(Optional.empty());

        Course course3 = Course.builder()
                .code("IT 201")
                .title("Advanced Database Systems")
                .lectureUnits(new BigDecimal("2.00"))
                .labUnits(new BigDecimal("1.00"))
                .creditUnits(new BigDecimal("3.00"))
                .build();
        ReflectionTestUtils.setField(course3, "id", 103L);

        CurriculumCourse cc1 = CurriculumCourse.builder()
                .curriculum(student.getCurriculum()).course(course1).yearLevel(1).semester("1ST_SEM").build();
        CurriculumCourse cc2 = CurriculumCourse.builder()
                .curriculum(student.getCurriculum()).course(course2).yearLevel(1).semester("2ND_SEM").build();
        CurriculumCourse cc3 = CurriculumCourse.builder()
                .curriculum(student.getCurriculum()).course(course3).yearLevel(2).semester("1ST_SEM").build();

        given(curriculumCourseRepository.findByCurriculumId(10L)).willReturn(List.of(cc1, cc2, cc3));
        given(prerequisiteRepository.findPrerequisitesForCourseIds(any())).willReturn(Collections.emptyList());
        given(sectionRepository.findAllWithSchedulesByTermId(20L)).willReturn(Collections.emptyList());

        AdvisingEligibilityResponse response = enrollmentService.getAdvisingEligibility(50L, 20L, null, null, true);

        assertThat(response).isNotNull();
        // With allCourses=true, all 3 courses (including Year 2 course3) are retained!
        assertThat(response.courses()).hasSize(3);
        List<String> codes = response.courses().stream().map(CourseEligibilityItemDto::code).toList();
        assertThat(codes).containsExactlyInAnyOrder("IT 101", "IT 102", "IT 201");
    }

    @Test
    @DisplayName("Gate 3: Should block enlistment in closed or full section")
    void shouldBlockEnlistmentInClosedSection() {
        given(studentProfileRepository.findByIdWithProgramAndCurriculum(50L)).willReturn(Optional.of(student));
        given(termService.getTermById(20L)).willReturn(term);
        given(sectionRepository.findByIdWithSchedules(201L)).willReturn(Optional.of(closedSection));

        EnlistSectionRequest request = new EnlistSectionRequest(20L, 201L);

        assertThatThrownBy(() -> enrollmentService.enlistSection(50L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Gate 3 Violation: Section 'BSIT-1B' is CLOSED");
    }

    @Test
    @DisplayName("Gate 3: Should block enlistment when prerequisite is not passed")
    void shouldBlockEnlistmentWhenPrerequisiteMissing() {
        given(studentProfileRepository.findByIdWithProgramAndCurriculum(50L)).willReturn(Optional.of(student));
        given(termService.getTermById(20L)).willReturn(term);
        given(sectionRepository.findByIdWithSchedules(200L)).willReturn(Optional.of(openSection));

        CoursePrerequisite prereq = CoursePrerequisite.builder()
                .course(course2)
                .prerequisiteCourse(course1)
                .minGradeRequired("3.00")
                .build();
        given(prerequisiteRepository.findByCourseId(102L)).willReturn(List.of(prereq));

        // Student has NOT passed course1 (findPassedGradesByStudentId returns empty list by default)

        EnlistSectionRequest request = new EnlistSectionRequest(20L, 200L);

        assertThatThrownBy(() -> enrollmentService.enlistSection(50L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing prerequisite 'IT 101'");
    }

    @Test
    @DisplayName("Gate 3: Should block enlistment when total units would exceed term cap (24.0 regular)")
    void shouldBlockEnlistmentWhenUnitCapExceeded() {
        given(studentProfileRepository.findByIdWithProgramAndCurriculum(50L)).willReturn(Optional.of(student));
        given(termService.getTermById(20L)).willReturn(term);
        given(sectionRepository.findByIdWithSchedules(200L)).willReturn(Optional.of(openSection));
        given(prerequisiteRepository.findByCourseId(102L)).willReturn(Collections.emptyList());

        // Existing enrollment already has 22.0 units; adding 3.0 units would reach 25.0 (> 24.0 cap)
        StudentEnrollment existingEnrollment = StudentEnrollment.builder()
                .student(student)
                .term(term)
                .status(StudentEnrollment.Status.ENLISTED)
                .totalCreditUnits(new BigDecimal("22.00"))
                .isOverloadApproved(false)
                .items(new LinkedHashSet<>())
                .build();
        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(50L, 20L))
                .willReturn(Optional.of(existingEnrollment));

        EnlistSectionRequest request = new EnlistSectionRequest(20L, 200L);

        assertThatThrownBy(() -> enrollmentService.enlistSection(50L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exceeds the term credit ceiling of 24.00 units");
    }

    @Test
    @DisplayName("Gate 3: Should fail enlistment atomically if section capacity reached during checkout")
    void shouldFailEnlistmentWhenCapacityDecrementsToZero() {
        given(studentProfileRepository.findByIdWithProgramAndCurriculum(50L)).willReturn(Optional.of(student));
        given(termService.getTermById(20L)).willReturn(term);
        given(sectionRepository.findByIdWithSchedules(200L)).willReturn(Optional.of(openSection));
        given(prerequisiteRepository.findByCourseId(102L)).willReturn(Collections.emptyList());

        StudentEnrollment existingEnrollment = StudentEnrollment.builder()
                .student(student)
                .term(term)
                .status(StudentEnrollment.Status.DRAFT)
                .totalCreditUnits(BigDecimal.ZERO)
                .isOverloadApproved(false)
                .items(new LinkedHashSet<>())
                .build();
        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(50L, 20L))
                .willReturn(Optional.of(existingEnrollment));

        // Atomic conditional update returns 0 (section filled concurrently)
        given(sectionRepository.incrementEnrolledCountIfOpen(200L)).willReturn(0);

        EnlistSectionRequest request = new EnlistSectionRequest(20L, 200L);

        assertThatThrownBy(() -> enrollmentService.enlistSection(50L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reached maximum capacity (40) or closed concurrently");
    }

    @Test
    @DisplayName("Gate 3: Should successfully enlist section and increment units when valid")
    void shouldEnlistSectionSuccessfully() {
        given(studentProfileRepository.findByIdWithProgramAndCurriculum(50L)).willReturn(Optional.of(student));
        given(termService.getTermById(20L)).willReturn(term);
        given(sectionRepository.findByIdWithSchedules(200L)).willReturn(Optional.of(openSection));
        given(prerequisiteRepository.findByCourseId(102L)).willReturn(Collections.emptyList());

        StudentEnrollment existingEnrollment = StudentEnrollment.builder()
                .student(student)
                .term(term)
                .status(StudentEnrollment.Status.DRAFT)
                .totalCreditUnits(BigDecimal.ZERO)
                .isOverloadApproved(false)
                .items(new LinkedHashSet<>())
                .build();
        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(50L, 20L))
                .willReturn(Optional.of(existingEnrollment));

        // Atomic update succeeds (1 row affected)
        given(sectionRepository.incrementEnrolledCountIfOpen(200L)).willReturn(1);
        given(studentEnrollmentRepository.save(any(StudentEnrollment.class))).willAnswer(inv -> inv.getArgument(0));

        EnlistSectionRequest request = new EnlistSectionRequest(20L, 200L);
        StudentEnrollmentResponse response = enrollmentService.enlistSection(50L, request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("ENLISTED");
        assertThat(response.totalCreditUnits()).isEqualTo(new BigDecimal("3.00"));
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).courseCode()).isEqualTo("IT 102");
    }

    @Test
    @DisplayName("Gate 3: Should confirm enrollment and transition status to ENROLLED")
    void shouldConfirmEnrollmentSuccessfully() {
        StudentEnrollment enrollment = StudentEnrollment.builder()
                .student(student)
                .term(term)
                .status(StudentEnrollment.Status.ENLISTED)
                .totalCreditUnits(new BigDecimal("18.00"))
                .items(new LinkedHashSet<>())
                .build();
        ReflectionTestUtils.setField(enrollment, "id", 800L);

        EnrollmentCourseItem item = EnrollmentCourseItem.builder()
                .enrollment(enrollment)
                .section(openSection)
                .build();
        enrollment.addItem(item);

        given(studentProfileRepository.findById(50L)).willReturn(Optional.of(student));
        given(termService.getTermById(20L)).willReturn(term);
        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(50L, 20L))
                .willReturn(Optional.of(enrollment));

        ConfirmEnrollmentRequest request = new ConfirmEnrollmentRequest(20L);
        EnrollmentConfirmationDto confirmation = enrollmentService.confirmEnrollment(50L, request);

        assertThat(confirmation).isNotNull();
        assertThat(confirmation.status()).isEqualTo("ENROLLED");
        assertThat(confirmation.totalCreditUnits()).isEqualTo(new BigDecimal("18.00"));
        assertThat(enrollment.getStatus()).isEqualTo(StudentEnrollment.Status.ENROLLED);
    }

    @Test
    @DisplayName("Gate 3: Should reject enlistment when student financial clearance is pending")
    void shouldRejectEnlistmentWhenFinancialClearanceNotCleared() {
        student.updateClearance(StudentProfile.ClearanceStatus.PENDING, StudentProfile.ClearanceStatus.CLEARED);
        given(studentProfileRepository.findByIdWithProgramAndCurriculum(50L)).willReturn(Optional.of(student));
        given(termService.getTermById(20L)).willReturn(term);
        given(sectionRepository.findByIdWithSchedules(301L)).willReturn(Optional.of(openSection));

        EnlistSectionRequest request = new EnlistSectionRequest(20L, 301L);

        assertThatThrownBy(() -> enrollmentService.enlistSection(50L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Clearance Violation");
    }

    @Test
    @DisplayName("Gate 3: Should reject confirm enrollment when student departmental clearance is blocked")
    void shouldRejectConfirmEnrollmentWhenDepartmentalClearanceBlocked() {
        student.updateClearance(StudentProfile.ClearanceStatus.CLEARED, StudentProfile.ClearanceStatus.BLOCKED);
        given(studentProfileRepository.findById(50L)).willReturn(Optional.of(student));

        ConfirmEnrollmentRequest request = new ConfirmEnrollmentRequest(20L);

        assertThatThrownBy(() -> enrollmentService.confirmEnrollment(50L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Clearance Violation");
    }

    @Test
    @DisplayName("getEnrollment: Returns UNENROLLED response for unprovisioned negative student ID without creating entities")
    void getEnrollment_NegativeId_Unprovisioned_ReturnsUnenrolledWithoutWrites() {
        given(studentService.findExistingStudentProfileIdFromAdmissionAppId(1L)).willReturn(Optional.empty());

        StudentEnrollmentResponse response = enrollmentService.getEnrollment(-1L, 10L);

        assertThat(response).isNotNull();
        assertThat(response.studentId()).isEqualTo(-1L);
        assertThat(response.termName()).isEqualTo("UNENROLLED");
        assertThat(response.status()).isEqualTo("NOT_ENROLLED");
        assertThat(response.items()).isEmpty();
    }

    @Test
    @DisplayName("getEnrollment: Resolves provisioned student profile for negative student ID without writes")
    void getEnrollment_NegativeId_Provisioned_ReturnsEnrollment() {
        given(studentService.findExistingStudentProfileIdFromAdmissionAppId(1L)).willReturn(Optional.of(50L));
        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(50L, 10L)).willReturn(Optional.empty());

        StudentEnrollmentResponse response = enrollmentService.getEnrollment(-1L, 10L);

        assertThat(response).isNotNull();
        assertThat(response.studentId()).isEqualTo(50L);
        assertThat(response.status()).isEqualTo("NOT_ENROLLED");
    }

    @Test
    @DisplayName("getEnrollment: Positive student ID with existing enrollment returns mapped items")
    void getEnrollment_PositiveId_ReturnsEnrollment() {
        StudentEnrollment mockEnrollment = StudentEnrollment.builder()
                .student(student)
                .term(term)
                .status(StudentEnrollment.Status.ENROLLED)
                .totalCreditUnits(new BigDecimal("3.00"))
                .isOverloadApproved(false)
                .build();
        ReflectionTestUtils.setField(mockEnrollment, "id", 200L);
        ReflectionTestUtils.setField(mockEnrollment, "items", new HashSet<>());

        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(50L, 20L)).willReturn(Optional.of(mockEnrollment));

        StudentEnrollmentResponse response = enrollmentService.getEnrollment(50L, 20L);

        assertThat(response).isNotNull();
        assertThat(response.enrollmentId()).isEqualTo(200L);
        assertThat(response.status()).isEqualTo("ENROLLED");
        assertThat(response.totalCreditUnits()).isEqualByComparingTo(new BigDecimal("3.00"));
    }

    @Test
    @DisplayName("Gate 3: Should allow provisional enlistment when prerequisite is a CO_REQUISITE")
    void shouldAllowProvisionalEnlistmentForCoRequisite() {
        given(studentProfileRepository.findByIdWithProgramAndCurriculum(50L)).willReturn(Optional.of(student));
        given(termService.getTermById(20L)).willReturn(term);
        given(sectionRepository.findByIdWithSchedules(200L)).willReturn(Optional.of(openSection));

        CoursePrerequisite coreq = CoursePrerequisite.builder()
                .course(course2)
                .prerequisiteCourse(course1)
                .ruleType("CO_REQUISITE")
                .minGradeRequired("3.00")
                .build();
        given(prerequisiteRepository.findByCourseId(102L)).willReturn(List.of(coreq));
        given(sectionRepository.incrementEnrolledCountIfOpen(200L)).willReturn(1);
        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(50L, 20L)).willReturn(Optional.empty());
        given(studentEnrollmentRepository.save(any(StudentEnrollment.class))).willAnswer(inv -> inv.getArgument(0));

        EnlistSectionRequest request = new EnlistSectionRequest(20L, 200L);
        StudentEnrollmentResponse response = enrollmentService.enlistSection(50L, request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("ENLISTED");
    }

    @Test
    @DisplayName("Gate 3: Should block confirm enrollment when mandatory CO_REQUISITE is missing")
    void shouldBlockConfirmEnrollmentWhenMandatoryCoRequisiteMissing() {
        StudentEnrollment enrollment = StudentEnrollment.builder()
                .student(student)
                .term(term)
                .status(StudentEnrollment.Status.ENLISTED)
                .totalCreditUnits(new BigDecimal("3.00"))
                .isOverloadApproved(false)
                .items(new LinkedHashSet<>())
                .build();
        ReflectionTestUtils.setField(enrollment, "id", 300L);

        EnrollmentCourseItem item = EnrollmentCourseItem.builder()
                .enrollment(enrollment)
                .section(openSection) // course2
                .build();
        enrollment.addItem(item);

        given(studentProfileRepository.findById(50L)).willReturn(Optional.of(student));
        given(termService.getTermById(20L)).willReturn(term);
        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(50L, 20L)).willReturn(Optional.of(enrollment));

        CoursePrerequisite coreq = CoursePrerequisite.builder()
                .course(course2)
                .prerequisiteCourse(course1)
                .ruleType("CO_REQUISITE")
                .minGradeRequired("3.00")
                .build();
        given(prerequisiteRepository.findByCourseId(102L)).willReturn(List.of(coreq));
        given(studentCourseGradeRepository.findPassedGradesByStudentId(50L)).willReturn(Collections.emptyList());

        ConfirmEnrollmentRequest request = new ConfirmEnrollmentRequest(20L);

        assertThatThrownBy(() -> enrollmentService.confirmEnrollment(50L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Gate 3 Violation: Missing mandatory co-requisite 'IT 101'");
    }

    @Test
    @DisplayName("Gate 3: Should allow confirm enrollment when mandatory CO_REQUISITE is enlisted concurrently")
    void shouldAllowConfirmEnrollmentWhenCoRequisiteEnlistedConcurrently() {
        ClassSection section1 = ClassSection.builder()
                .term(term)
                .course(course1)
                .sectionCode("BSIT-1A-C1")
                .maxCapacity(40)
                .status(ClassSection.Status.OPEN)
                .build();
        ReflectionTestUtils.setField(section1, "id", 201L);

        StudentEnrollment enrollment = StudentEnrollment.builder()
                .student(student)
                .term(term)
                .status(StudentEnrollment.Status.ENLISTED)
                .totalCreditUnits(new BigDecimal("6.00"))
                .isOverloadApproved(false)
                .items(new LinkedHashSet<>())
                .build();
        ReflectionTestUtils.setField(enrollment, "id", 300L);

        EnrollmentCourseItem item2 = EnrollmentCourseItem.builder()
                .enrollment(enrollment)
                .section(openSection) // course2
                .build();
        EnrollmentCourseItem item1 = EnrollmentCourseItem.builder()
                .enrollment(enrollment)
                .section(section1) // course1 (the co-requisite)
                .build();
        enrollment.addItem(item2);
        enrollment.addItem(item1);

        given(studentProfileRepository.findById(50L)).willReturn(Optional.of(student));
        given(termService.getTermById(20L)).willReturn(term);
        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(50L, 20L)).willReturn(Optional.of(enrollment));

        CoursePrerequisite coreq = CoursePrerequisite.builder()
                .course(course2)
                .prerequisiteCourse(course1)
                .ruleType("CO_REQUISITE")
                .minGradeRequired("3.00")
                .build();
        given(prerequisiteRepository.findByCourseId(102L)).willReturn(List.of(coreq));
        given(prerequisiteRepository.findByCourseId(101L)).willReturn(Collections.emptyList());
        given(studentCourseGradeRepository.findPassedGradesByStudentId(50L)).willReturn(Collections.emptyList());

        ConfirmEnrollmentRequest request = new ConfirmEnrollmentRequest(20L);
        EnrollmentConfirmationDto confirmation = enrollmentService.confirmEnrollment(50L, request);

        assertThat(confirmation).isNotNull();
        assertThat(confirmation.status()).isEqualTo("ENROLLED");
        assertThat(enrollment.getStatus()).isEqualTo(StudentEnrollment.Status.ENROLLED);
    }

    @Test
    @DisplayName("Gate 3: Should reject enlistment when term enrollment is closed")
    void shouldThrowExceptionWhenEnlistingInClosedTerm() {
        term.closeEnrollment();
        given(studentProfileRepository.findByIdWithProgramAndCurriculum(50L)).willReturn(Optional.of(student));
        given(termService.getTermById(20L)).willReturn(term);

        EnlistSectionRequest request = new EnlistSectionRequest(20L, 200L);

        assertThatThrownBy(() -> enrollmentService.enlistSection(50L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("currently closed");
    }

    @Test
    @DisplayName("Gate 4: Should reject enrollment confirmation when term enrollment is closed")
    void shouldThrowExceptionWhenConfirmingInClosedTerm() {
        term.closeEnrollment();
        given(studentProfileRepository.findById(50L)).willReturn(Optional.of(student));
        given(termService.getTermById(20L)).willReturn(term);

        ConfirmEnrollmentRequest request = new ConfirmEnrollmentRequest(20L);

        assertThatThrownBy(() -> enrollmentService.confirmEnrollment(50L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("currently closed");
    }
}

