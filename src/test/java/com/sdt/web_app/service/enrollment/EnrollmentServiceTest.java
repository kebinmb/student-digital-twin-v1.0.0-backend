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
    private TermRepository termRepository;

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
        given(termRepository.findById(20L)).willReturn(Optional.of(term));

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
    @DisplayName("Gate 3: Should block enlistment in closed or full section")
    void shouldBlockEnlistmentInClosedSection() {
        given(studentProfileRepository.findByIdWithProgramAndCurriculum(50L)).willReturn(Optional.of(student));
        given(termRepository.findById(20L)).willReturn(Optional.of(term));
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
        given(termRepository.findById(20L)).willReturn(Optional.of(term));
        given(sectionRepository.findByIdWithSchedules(200L)).willReturn(Optional.of(openSection));

        CoursePrerequisite prereq = CoursePrerequisite.builder()
                .course(course2)
                .prerequisiteCourse(course1)
                .minGradeRequired("3.00")
                .build();
        given(prerequisiteRepository.findByCourseId(102L)).willReturn(List.of(prereq));

        // Student has NOT passed course1
        given(studentCourseGradeRepository.isCoursePassedByStudent(50L, 101L)).willReturn(false);

        EnlistSectionRequest request = new EnlistSectionRequest(20L, 200L);

        assertThatThrownBy(() -> enrollmentService.enlistSection(50L, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing prerequisite 'IT 101'");
    }

    @Test
    @DisplayName("Gate 3: Should block enlistment when total units would exceed term cap (24.0 regular)")
    void shouldBlockEnlistmentWhenUnitCapExceeded() {
        given(studentProfileRepository.findByIdWithProgramAndCurriculum(50L)).willReturn(Optional.of(student));
        given(termRepository.findById(20L)).willReturn(Optional.of(term));
        given(sectionRepository.findByIdWithSchedules(200L)).willReturn(Optional.of(openSection));
        given(prerequisiteRepository.findByCourseId(102L)).willReturn(Collections.emptyList());

        // Existing enrollment already has 22.0 units; adding 3.0 units would reach 25.0 (> 24.0 cap)
        StudentEnrollment existingEnrollment = StudentEnrollment.builder()
                .student(student)
                .term(term)
                .status(StudentEnrollment.Status.ENLISTED)
                .totalCreditUnits(new BigDecimal("22.00"))
                .isOverloadApproved(false)
                .items(new ArrayList<>())
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
        given(termRepository.findById(20L)).willReturn(Optional.of(term));
        given(sectionRepository.findByIdWithSchedules(200L)).willReturn(Optional.of(openSection));
        given(prerequisiteRepository.findByCourseId(102L)).willReturn(Collections.emptyList());

        StudentEnrollment existingEnrollment = StudentEnrollment.builder()
                .student(student)
                .term(term)
                .status(StudentEnrollment.Status.DRAFT)
                .totalCreditUnits(BigDecimal.ZERO)
                .isOverloadApproved(false)
                .items(new ArrayList<>())
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
        given(termRepository.findById(20L)).willReturn(Optional.of(term));
        given(sectionRepository.findByIdWithSchedules(200L)).willReturn(Optional.of(openSection));
        given(prerequisiteRepository.findByCourseId(102L)).willReturn(Collections.emptyList());

        StudentEnrollment existingEnrollment = StudentEnrollment.builder()
                .student(student)
                .term(term)
                .status(StudentEnrollment.Status.DRAFT)
                .totalCreditUnits(BigDecimal.ZERO)
                .isOverloadApproved(false)
                .items(new ArrayList<>())
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
                .items(new ArrayList<>())
                .build();
        ReflectionTestUtils.setField(enrollment, "id", 800L);

        EnrollmentCourseItem item = EnrollmentCourseItem.builder()
                .enrollment(enrollment)
                .section(openSection)
                .build();
        enrollment.addItem(item);

        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(50L, 20L))
                .willReturn(Optional.of(enrollment));

        ConfirmEnrollmentRequest request = new ConfirmEnrollmentRequest(20L);
        EnrollmentConfirmationDto confirmation = enrollmentService.confirmEnrollment(50L, request);

        assertThat(confirmation).isNotNull();
        assertThat(confirmation.status()).isEqualTo("ENROLLED");
        assertThat(confirmation.totalCreditUnits()).isEqualTo(new BigDecimal("18.00"));
        assertThat(enrollment.getStatus()).isEqualTo(StudentEnrollment.Status.ENROLLED);
    }
}
