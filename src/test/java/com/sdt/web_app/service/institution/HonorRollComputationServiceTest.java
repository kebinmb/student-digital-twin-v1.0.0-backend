package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.HonorRollDtos.TermHonorRollReportDto;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.enrollment.StudentEnrollment;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.enrollment.StudentEnrollmentRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.service.lms.StudentNotificationPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HonorRollComputationServiceTest {

    @Mock
    private StudentEnrollmentRepository enrollmentRepository;

    @Mock
    private TermRepository termRepository;

    @Mock
    private StudentNotificationPublisherService studentNotificationPublisherService;

    @Mock
    private com.sdt.web_app.service.webhook.InstitutionalWebhookService institutionalWebhookService;

    @InjectMocks
    private HonorRollComputationService honorRollService;

    private Term term;
    private Program program;
    private StudentProfile studentA;
    private StudentProfile studentB;

    @BeforeEach
    void setUp() {
        term = Term.builder().id(10L).termType(com.sdt.web_app.entities.institution.TermType.FIRST_SEMESTER).build();
        program = Program.builder().id(1L).code("BSCS").name("BS Computer Science").build();

        User userA = User.builder().id(101L).username("studentA").build();
        User userB = User.builder().id(102L).username("studentB").build();

        studentA = StudentProfile.builder()
                .id(1L)
                .user(userA)
                .studentNumber("2026-0001")
                .firstName("Maria")
                .lastName("Santos")
                .program(program)
                .build();

        studentB = StudentProfile.builder()
                .id(2L)
                .user(userB)
                .studentNumber("2026-0002")
                .firstName("Juan")
                .lastName("Dela Cruz")
                .program(program)
                .build();
    }

    @Test
    @DisplayName("computeTermHonorRoll classifies President's List and Dean's List candidates accurately")
    void computeTermHonorRoll_ClassifiesHonorsCorrectly() {
        Course course1 = Course.builder().id(1L).code("CS101").creditUnits(BigDecimal.valueOf(3.0)).build();
        Course course2 = Course.builder().id(2L).code("CS102").creditUnits(BigDecimal.valueOf(3.0)).build();
        Course course3 = Course.builder().id(3L).code("CS103").creditUnits(BigDecimal.valueOf(3.0)).build();
        Course course4 = Course.builder().id(4L).code("CS104").creditUnits(BigDecimal.valueOf(3.0)).build();
        Course course5 = Course.builder().id(5L).code("CS105").creditUnits(BigDecimal.valueOf(3.0)).build();

        ClassSection s1 = ClassSection.builder().id(1L).course(course1).build();
        ClassSection s2 = ClassSection.builder().id(2L).course(course2).build();
        ClassSection s3 = ClassSection.builder().id(3L).course(course3).build();
        ClassSection s4 = ClassSection.builder().id(4L).course(course4).build();
        ClassSection s5 = ClassSection.builder().id(5L).course(course5).build();

        // Student A: 15 units, all 1.25 -> President's List
        StudentEnrollment enrA = StudentEnrollment.builder().id(1L).student(studentA).term(term).items(new LinkedHashSet<>()).build();
        enrA.getItems().add(EnrollmentCourseItem.builder().id(1L).enrollment(enrA).section(s1).finalNumericalGrade(BigDecimal.valueOf(1.25)).completionStatus(EnrollmentCourseItem.CompletionStatus.PASSED).build());
        enrA.getItems().add(EnrollmentCourseItem.builder().id(2L).enrollment(enrA).section(s2).finalNumericalGrade(BigDecimal.valueOf(1.25)).completionStatus(EnrollmentCourseItem.CompletionStatus.PASSED).build());
        enrA.getItems().add(EnrollmentCourseItem.builder().id(3L).enrollment(enrA).section(s3).finalNumericalGrade(BigDecimal.valueOf(1.00)).completionStatus(EnrollmentCourseItem.CompletionStatus.PASSED).build());
        enrA.getItems().add(EnrollmentCourseItem.builder().id(4L).enrollment(enrA).section(s4).finalNumericalGrade(BigDecimal.valueOf(1.25)).completionStatus(EnrollmentCourseItem.CompletionStatus.PASSED).build());
        enrA.getItems().add(EnrollmentCourseItem.builder().id(5L).enrollment(enrA).section(s5).finalNumericalGrade(BigDecimal.valueOf(1.25)).completionStatus(EnrollmentCourseItem.CompletionStatus.PASSED).build());

        // Student B: 15 units, all 1.75 -> Dean's List
        StudentEnrollment enrB = StudentEnrollment.builder().id(2L).student(studentB).term(term).items(new LinkedHashSet<>()).build();
        enrB.getItems().add(EnrollmentCourseItem.builder().id(6L).enrollment(enrB).section(s1).finalNumericalGrade(BigDecimal.valueOf(1.75)).completionStatus(EnrollmentCourseItem.CompletionStatus.PASSED).build());
        enrB.getItems().add(EnrollmentCourseItem.builder().id(7L).enrollment(enrB).section(s2).finalNumericalGrade(BigDecimal.valueOf(1.75)).completionStatus(EnrollmentCourseItem.CompletionStatus.PASSED).build());
        enrB.getItems().add(EnrollmentCourseItem.builder().id(8L).enrollment(enrB).section(s3).finalNumericalGrade(BigDecimal.valueOf(1.50)).completionStatus(EnrollmentCourseItem.CompletionStatus.PASSED).build());
        enrB.getItems().add(EnrollmentCourseItem.builder().id(9L).enrollment(enrB).section(s4).finalNumericalGrade(BigDecimal.valueOf(1.75)).completionStatus(EnrollmentCourseItem.CompletionStatus.PASSED).build());
        enrB.getItems().add(EnrollmentCourseItem.builder().id(10L).enrollment(enrB).section(s5).finalNumericalGrade(BigDecimal.valueOf(1.75)).completionStatus(EnrollmentCourseItem.CompletionStatus.PASSED).build());

        given(termRepository.findById(10L)).willReturn(Optional.of(term));
        given(enrollmentRepository.findByTermIdWithDetails(10L)).willReturn(List.of(enrA, enrB));

        TermHonorRollReportDto report = honorRollService.computeTermHonorRoll(10L, null);

        assertThat(report.totalEvaluated()).isEqualTo(2);
        assertThat(report.totalQualified()).isEqualTo(2);
        assertThat(report.honorees()).hasSize(2);

        // Verify rank 1 is Student A (President's List)
        assertThat(report.honorees().get(0).studentId()).isEqualTo(1L);
        assertThat(report.honorees().get(0).honorCategory()).isEqualTo("PRESIDENTS_LIST");
        assertThat(report.honorees().get(0).rank()).isEqualTo(1);

        // Verify rank 2 is Student B (Dean's List)
        assertThat(report.honorees().get(1).studentId()).isEqualTo(2L);
        assertThat(report.honorees().get(1).honorCategory()).isEqualTo("DEANS_LIST");
        assertThat(report.honorees().get(1).rank()).isEqualTo(2);

        // Verify student notification dispatched
        verify(studentNotificationPublisherService).publishStandingUpdatedEvent(eq(1L), eq(1.2), eq(BigDecimal.valueOf(15.0)), eq("PRESIDENTS_LIST"));
    }

    @Test
    @DisplayName("Students with failing grades or underloaded units are excluded from honor roll")
    void computeTermHonorRoll_FailingOrUnderloaded_Excluded() {
        Course course1 = Course.builder().id(1L).code("CS101").creditUnits(BigDecimal.valueOf(3.0)).build();
        ClassSection s1 = ClassSection.builder().id(1L).course(course1).build();

        // Underloaded: only 3 units
        StudentEnrollment enrA = StudentEnrollment.builder().id(1L).student(studentA).term(term).items(new LinkedHashSet<>()).build();
        enrA.getItems().add(EnrollmentCourseItem.builder().id(1L).enrollment(enrA).section(s1).finalNumericalGrade(BigDecimal.valueOf(1.00)).completionStatus(EnrollmentCourseItem.CompletionStatus.PASSED).build());

        given(termRepository.findById(10L)).willReturn(Optional.of(term));
        given(enrollmentRepository.findByTermIdWithDetails(10L)).willReturn(List.of(enrA));

        TermHonorRollReportDto report = honorRollService.computeTermHonorRoll(10L, null);

        assertThat(report.totalEvaluated()).isEqualTo(1);
        assertThat(report.totalQualified()).isEqualTo(0);
        assertThat(report.honorees()).isEmpty();
    }
}
