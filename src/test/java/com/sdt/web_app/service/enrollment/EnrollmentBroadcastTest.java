package com.sdt.web_app.service.enrollment;

import com.sdt.web_app.config.WebSocketBroadcastService;
import com.sdt.web_app.config.WebSocketTopics;
import com.sdt.web_app.dto.enrollment.EnrollmentDtos.ConfirmEnrollmentRequest;
import com.sdt.web_app.dto.enrollment.EnrollmentDtos.EnlistSectionRequest;
import com.sdt.web_app.dto.enrollment.EnrollmentDtos.UpdateEnrollmentStatusRequest;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
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
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import com.sdt.web_app.repositories.compliance.ClearanceRequestRepository;
import com.sdt.web_app.websocket.dto.EnrollmentStatusMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnrollmentBroadcastTest {

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
    private WebSocketBroadcastService broadcastService;

    @InjectMocks
    private EnrollmentService enrollmentService;

    private StudentProfile student;
    private Term term;
    private Course course;
    private ClassSection section;
    private StudentEnrollment enrollment;

    @BeforeEach
    void setUp() {
        Program program = Program.builder().code("BSIT").name("BS Information Technology").build();
        ReflectionTestUtils.setField(program, "id", 1L);

        Curriculum curriculum = Curriculum.builder().code("BSIT-2026").status(Curriculum.Status.ACTIVE).build();
        ReflectionTestUtils.setField(curriculum, "id", 10L);

        User user = User.builder().username("student_1").email("student1@chmsu.edu.ph").build();
        ReflectionTestUtils.setField(user, "id", 100L);

        student = StudentProfile.builder()
                .user(user)
                .studentNumber("2026-IT-0001")
                .program(program)
                .curriculum(curriculum)
                .yearLevel(1)
                .enrollmentStatus(StudentProfile.EnrollmentStatus.REGULAR)
                .isGraduating(false)
                .totalUnitsEarned(BigDecimal.ZERO)
                .build();
        ReflectionTestUtils.setField(student, "id", 42L);

        AcademicYear ay = AcademicYear.builder().code("AY 2026-2027").build();
        term = Term.builder()
                .academicYear(ay)
                .termType(TermType.FIRST_SEM)
                .enrollmentOpen(true)
                .addDropOpen(true)
                .build();
        ReflectionTestUtils.setField(term, "id", 11L);

        course = Course.builder()
                .code("IT 101")
                .title("Introduction to Computing")
                .lectureUnits(new BigDecimal("2.00"))
                .labUnits(new BigDecimal("1.00"))
                .creditUnits(new BigDecimal("3.00"))
                .build();
        ReflectionTestUtils.setField(course, "id", 101L);

        section = ClassSection.builder()
                .term(term)
                .curriculum(curriculum)
                .course(course)
                .sectionCode("BSIT-1A")
                .maxCapacity(40)
                .enrolledCount(10)
                .status(ClassSection.Status.OPEN)
                .build();
        ReflectionTestUtils.setField(section, "id", 201L);

        enrollment = StudentEnrollment.builder()
                .student(student)
                .term(term)
                .totalCreditUnits(new BigDecimal("3.00"))
                .status(StudentEnrollment.Status.ENLISTED)
                .items(new LinkedHashSet<>())
                .build();
        ReflectionTestUtils.setField(enrollment, "id", 301L);
    }

    @Test
    @DisplayName("Should broadcast to student and admin topics upon enlisting a section")
    void shouldBroadcastUponEnlistSection() {
        given(studentProfileRepository.existsById(42L)).willReturn(true);
        given(studentProfileRepository.findByIdWithProgramAndCurriculum(42L)).willReturn(Optional.of(student));
        given(termService.getTermById(11L)).willReturn(term);
        given(sectionRepository.findByIdWithSchedules(201L)).willReturn(Optional.of(section));
        given(prerequisiteRepository.findByCourseId(101L)).willReturn(Collections.emptyList());

        StudentEnrollment existing = StudentEnrollment.builder()
                .student(student)
                .term(term)
                .status(StudentEnrollment.Status.DRAFT)
                .totalCreditUnits(BigDecimal.ZERO)
                .isOverloadApproved(false)
                .items(new LinkedHashSet<>())
                .build();
        ReflectionTestUtils.setField(existing, "id", 301L);

        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(42L, 11L))
                .willReturn(Optional.of(existing));
        given(sectionRepository.incrementEnrolledCountIfOpen(201L)).willReturn(1);
        given(studentEnrollmentRepository.save(any(StudentEnrollment.class))).willAnswer(inv -> inv.getArgument(0));

        EnlistSectionRequest request = new EnlistSectionRequest(11L, 201L);
        enrollmentService.enlistSection(42L, request);

        ArgumentCaptor<EnrollmentStatusMessage> captor = ArgumentCaptor.forClass(EnrollmentStatusMessage.class);
        verify(broadcastService).broadcast(eq(WebSocketTopics.enrollment(42L)), captor.capture());
        verify(broadcastService).broadcast(eq(WebSocketTopics.ADMIN_ENROLLMENTS), captor.capture());

        EnrollmentStatusMessage sentMessage = captor.getValue();
        assertThat(sentMessage.studentProfileId()).isEqualTo(42L);
        assertThat(sentMessage.termId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("Should broadcast to student and admin topics upon removing an enlisted section")
    void shouldBroadcastUponRemoveEnlistedSection() {
        EnrollmentCourseItem item = EnrollmentCourseItem.builder()
                .enrollment(enrollment)
                .section(section)
                .build();
        ReflectionTestUtils.setField(item, "id", 501L);
        enrollment.addItem(item);

        given(studentProfileRepository.existsById(42L)).willReturn(true);
        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(42L, 11L)).willReturn(Optional.of(enrollment));
        given(termService.getTermById(11L)).willReturn(term);
        given(studentEnrollmentRepository.save(any(StudentEnrollment.class))).willAnswer(inv -> inv.getArgument(0));

        enrollmentService.removeEnlistedSection(42L, 11L, 201L);

        ArgumentCaptor<EnrollmentStatusMessage> captor = ArgumentCaptor.forClass(EnrollmentStatusMessage.class);
        verify(broadcastService).broadcast(eq(WebSocketTopics.enrollment(42L)), captor.capture());
        verify(broadcastService).broadcast(eq(WebSocketTopics.ADMIN_ENROLLMENTS), captor.capture());

        EnrollmentStatusMessage sentMessage = captor.getValue();
        assertThat(sentMessage.studentProfileId()).isEqualTo(42L);
        assertThat(sentMessage.termId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("Should broadcast to student and admin topics upon confirming enrollment")
    void shouldBroadcastUponConfirmEnrollment() {
        EnrollmentCourseItem item = EnrollmentCourseItem.builder()
                .enrollment(enrollment)
                .section(section)
                .build();
        ReflectionTestUtils.setField(item, "id", 501L);
        enrollment.addItem(item);

        given(studentProfileRepository.existsById(42L)).willReturn(true);
        given(studentProfileRepository.findById(42L)).willReturn(Optional.of(student));
        given(termService.getTermById(11L)).willReturn(term);
        given(studentEnrollmentRepository.findByStudentIdAndTermIdWithItems(42L, 11L)).willReturn(Optional.of(enrollment));
        given(studentEnrollmentRepository.save(any(StudentEnrollment.class))).willAnswer(inv -> inv.getArgument(0));

        ConfirmEnrollmentRequest request = new ConfirmEnrollmentRequest(11L);
        enrollmentService.confirmEnrollment(42L, request);

        ArgumentCaptor<EnrollmentStatusMessage> captor = ArgumentCaptor.forClass(EnrollmentStatusMessage.class);
        verify(broadcastService).broadcast(eq(WebSocketTopics.enrollment(42L)), captor.capture());
        verify(broadcastService).broadcast(eq(WebSocketTopics.ADMIN_ENROLLMENTS), captor.capture());

        EnrollmentStatusMessage sentMessage = captor.getValue();
        assertThat(sentMessage.enrollmentStatus()).isEqualTo(StudentEnrollment.Status.ENROLLED.name());
    }

    @Test
    @DisplayName("Should broadcast to student and admin topics upon updating enrollment status")
    void shouldBroadcastUponUpdateEnrollmentStatus() {
        given(studentEnrollmentRepository.findById(301L)).willReturn(Optional.of(enrollment));
        given(studentEnrollmentRepository.save(any(StudentEnrollment.class))).willAnswer(inv -> inv.getArgument(0));

        UpdateEnrollmentStatusRequest request = new UpdateEnrollmentStatusRequest("ENROLLED", true);
        enrollmentService.updateEnrollmentStatus(301L, request);

        ArgumentCaptor<EnrollmentStatusMessage> captor = ArgumentCaptor.forClass(EnrollmentStatusMessage.class);
        verify(broadcastService).broadcast(eq(WebSocketTopics.enrollment(42L)), captor.capture());
        verify(broadcastService).broadcast(eq(WebSocketTopics.ADMIN_ENROLLMENTS), captor.capture());

        EnrollmentStatusMessage sentMessage = captor.getValue();
        assertThat(sentMessage.enrollmentId()).isEqualTo(301L);
        assertThat(sentMessage.studentProfileId()).isEqualTo(42L);
    }
}
