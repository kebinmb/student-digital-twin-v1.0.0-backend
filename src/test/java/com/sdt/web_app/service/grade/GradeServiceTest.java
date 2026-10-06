package com.sdt.web_app.service.grade;

import com.sdt.web_app.dto.grade.GradeDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.enrollment.StudentCourseGrade;
import com.sdt.web_app.entities.enrollment.StudentEnrollment;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.EnrollmentCourseItemRepository;
import com.sdt.web_app.repositories.enrollment.StudentCourseGradeRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.grade.GradeSealingAuditRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private ClassSectionRepository sectionRepository;
    @Mock
    private EnrollmentCourseItemRepository itemRepository;
    @Mock
    private StudentCourseGradeRepository gradeRepository;
    @Mock
    private StudentProfileRepository profileRepository;
    @Mock
    private com.sdt.web_app.service.security.AcademicScopeAssertionService academicScopeAssertionService;
    @Mock
    private GradeSealingAuditRepository sealingAuditRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private com.sdt.web_app.service.scheduling.SectionEventPublisherService sectionEventPublisherService;
    @Mock
    private com.sdt.web_app.service.lms.StudentNotificationPublisherService studentNotificationPublisherService;
    @Mock
    private com.sdt.web_app.service.compliance.ClearanceWorkflowService clearanceWorkflowService;

    @InjectMocks
    private GradeService gradeService;

    private ClassSection section;
    private Course course;
    private Term term;
    private StudentProfile studentProfile;
    private EnrollmentCourseItem item;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .code("IT-201")
                .title("Data Structures and Algorithms")
                .creditUnits(new BigDecimal("3.00"))
                .lectureUnits(new BigDecimal("2.00"))
                .labUnits(new BigDecimal("1.00"))
                .build();
        ReflectionTestUtils.setField(course, "id", 101L);

        term = Term.builder().termType(com.sdt.web_app.entities.institution.TermType.FIRST_SEM).build();
        ReflectionTestUtils.setField(term, "id", 10L);

        User instructor = User.builder().username("faculty_bob").build();
        ReflectionTestUtils.setField(instructor, "id", 5L);

        section = ClassSection.builder()
                .sectionCode("BSIT-2A")
                .course(course)
                .term(term)
                .curriculum(Curriculum.builder().code("BSIT-2026").build())
                .primaryInstructor(instructor)
                .gradeStatus(ClassSection.GradeStatus.DRAFT)
                .status(ClassSection.Status.OPEN)
                .maxCapacity(40)
                .enrolledCount(1)
                .build();
        ReflectionTestUtils.setField(section, "id", 201L);

        User studentUser = User.builder().username("student_john").email("john@example.com").build();
        studentProfile = StudentProfile.builder()
                .user(studentUser)
                .studentNumber("2026-IT-0001")
                .program(Program.builder().code("BSIT").build())
                .totalUnitsEarned(BigDecimal.ZERO)
                .build();
        ReflectionTestUtils.setField(studentProfile, "id", 301L);

        StudentEnrollment enrollment = StudentEnrollment.builder()
                .student(studentProfile)
                .term(term)
                .build();

        item = EnrollmentCourseItem.builder()
                .enrollment(enrollment)
                .section(section)
                .completionStatus(EnrollmentCourseItem.CompletionStatus.ENROLLED)
                .build();
        ReflectionTestUtils.setField(item, "id", 401L);
    }

    @Test
    @DisplayName("Should return section roster with enrolled students")
    void getSectionRoster_Success() {
        given(sectionRepository.findByIdWithSchedules(201L)).willReturn(Optional.of(section));
        given(itemRepository.findBySectionIdWithStudentDetails(201L)).willReturn(List.of(item));

        SectionRosterResponse response = gradeService.getSectionRoster(201L);

        assertThat(response).isNotNull();
        assertThat(response.sectionId()).isEqualTo(201L);
        assertThat(response.sectionCode()).isEqualTo("BSIT-2A");
        assertThat(response.gradeStatus()).isEqualTo("DRAFT");
        assertThat(response.primaryInstructorName()).isEqualTo("faculty_bob");
        assertThat(response.students()).hasSize(1);
        assertThat(response.students().get(0).studentNumber()).isEqualTo("2026-IT-0001");
    }

    @Test
    @DisplayName("Should save grades as draft without transitioning to SUBMITTED")
    void saveGrades_Draft_Success() {
        SaveSectionGradesRequest request = new SaveSectionGradesRequest(
                List.of(new GradeEntryDto(401L, new BigDecimal("1.75"), "PASSED")),
                false
        );

        given(sectionRepository.findById(201L)).willReturn(Optional.of(section));
        given(itemRepository.findById(401L)).willReturn(Optional.of(item));

        GradeActionResponse response = gradeService.saveGrades(201L, request, 5L);

        assertThat(response).isNotNull();
        assertThat(response.gradeStatus()).isEqualTo("DRAFT");
        assertThat(response.updatedCount()).isEqualTo(1);
        assertThat(item.getFinalNumericalGrade()).isEqualByComparingTo("1.75");
        assertThat(item.getCompletionStatus()).isEqualTo(EnrollmentCourseItem.CompletionStatus.PASSED);
    }

    @Test
    @DisplayName("Should submit grades for verification and transition status to SUBMITTED")
    void saveGrades_SubmitForVerification_Success() {
        SaveSectionGradesRequest request = new SaveSectionGradesRequest(
                List.of(new GradeEntryDto(401L, new BigDecimal("1.75"), "PASSED")),
                true
        );

        given(sectionRepository.findById(201L)).willReturn(Optional.of(section));
        given(itemRepository.findById(401L)).willReturn(Optional.of(item));
        given(itemRepository.findBySectionIdWithStudentDetails(201L)).willReturn(List.of(item));

        GradeActionResponse response = gradeService.saveGrades(201L, request, 5L);

        assertThat(response.gradeStatus()).isEqualTo("SUBMITTED");
        assertThat(section.getGradeStatus()).isEqualTo(ClassSection.GradeStatus.SUBMITTED);
        verify(sectionRepository).save(section);
    }

    @Test
    @DisplayName("Should verify grades and transition status from SUBMITTED to VERIFIED")
    void verifyGrades_Success() {
        section.updateGradeStatus(ClassSection.GradeStatus.SUBMITTED);
        given(sectionRepository.findById(201L)).willReturn(Optional.of(section));

        GradeActionResponse response = gradeService.verifyGrades(201L, 2L);

        assertThat(response.gradeStatus()).isEqualTo("VERIFIED");
        assertThat(section.getGradeStatus()).isEqualTo(ClassSection.GradeStatus.VERIFIED);
        verify(sectionRepository).save(section);
    }

    @Test
    @DisplayName("Should reject grades and return status from SUBMITTED to DRAFT")
    void rejectGrades_Success() {
        section.updateGradeStatus(ClassSection.GradeStatus.SUBMITTED);
        given(sectionRepository.findById(201L)).willReturn(Optional.of(section));

        GradeActionResponse response = gradeService.rejectGrades(201L, "Scores inaccurate", 2L);

        assertThat(response.gradeStatus()).isEqualTo("DRAFT");
        assertThat(section.getGradeStatus()).isEqualTo(ClassSection.GradeStatus.DRAFT);
        verify(sectionRepository).save(section);
    }

    @Test
    @DisplayName("Should permanently seal verified grades into transcripts and recalculate GPA")
    void sealGrades_Success() {
        section.updateGradeStatus(ClassSection.GradeStatus.VERIFIED);
        item.updateGrade(new BigDecimal("1.75"), EnrollmentCourseItem.CompletionStatus.PASSED);

        given(sectionRepository.findById(201L)).willReturn(Optional.of(section));
        given(itemRepository.findBySectionIdWithStudentDetails(201L)).willReturn(List.of(item));
        org.mockito.Mockito.lenient().when(gradeRepository.findByStudentIdAndCourseIdAndTermId(301L, 101L, 10L)).thenReturn(Optional.empty());
        org.mockito.Mockito.lenient().when(gradeRepository.findByStudentIdAndCourseId(301L, 101L)).thenReturn(Optional.empty());

        StudentCourseGrade sealedGrade = StudentCourseGrade.builder()
                .student(studentProfile)
                .course(course)
                .term(term)
                .numericalGrade(new BigDecimal("1.75"))
                .completionStatus("PASSED")
                .build();
        given(gradeRepository.findPassedGradesByStudentId(301L)).willReturn(List.of(sealedGrade));

        GradeActionResponse response = gradeService.sealGrades(201L, 4L);

        assertThat(response.gradeStatus()).isEqualTo("SEALED");
        assertThat(section.getGradeStatus()).isEqualTo(ClassSection.GradeStatus.SEALED);

        verify(gradeRepository).save(any(StudentCourseGrade.class));
        verify(profileRepository).save(studentProfile);
        assertThat(studentProfile.getTotalUnitsEarned()).isEqualByComparingTo("3.00");
        assertThat(studentProfile.getCumulativeGpa()).isEqualByComparingTo("1.75");
    }

    @Test
    @DisplayName("Should reject grade editing when section is SEALED")
    void saveGrades_WhenAlreadySealed_ThrowsException() {
        section.updateGradeStatus(ClassSection.GradeStatus.SEALED);
        given(sectionRepository.findById(201L)).willReturn(Optional.of(section));

        SaveSectionGradesRequest request = new SaveSectionGradesRequest(List.of(), false);

        assertThatThrownBy(() -> gradeService.saveGrades(201L, request, 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot update grades: section BSIT-2A is currently in SEALED status");
    }
}
