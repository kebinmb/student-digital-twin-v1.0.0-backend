package com.sdt.web_app.service.enrollment;

import com.sdt.web_app.dto.enrollment.EnrollmentDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.CourseEquivalency;
import com.sdt.web_app.entities.enrollment.StudentCourseGrade;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.CourseEquivalencyRepository;
import com.sdt.web_app.repositories.enrollment.StudentCourseGradeRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.CourseRepository;
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
class TransfereeCreditingServiceTest {

    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseEquivalencyRepository equivalencyRepository;
    @Mock
    private StudentCourseGradeRepository studentCourseGradeRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransfereeCreditingService creditingService;

    private StudentProfile student;
    private Course internalCourse;
    private User approver;

    @BeforeEach
    void setUp() {
        Program program = Program.builder().code("BSIT").name("BSIT").build();
        Curriculum curriculum = Curriculum.builder().code("BSIT-2026").build();
        User user = User.builder().username("transferee_mary").email("mary@example.com").build();

        student = StudentProfile.builder()
                .user(user)
                .studentNumber("2026-TR-0001")
                .program(program)
                .curriculum(curriculum)
                .classification(StudentProfile.StudentClassification.TRANSFEREE)
                .totalUnitsEarned(BigDecimal.ZERO)
                .build();
        ReflectionTestUtils.setField(student, "id", 60L);

        internalCourse = Course.builder()
                .code("CC-101")
                .title("Introduction to Computing")
                .lectureUnits(new BigDecimal("2.00"))
                .labUnits(new BigDecimal("1.00"))
                .creditUnits(new BigDecimal("3.00"))
                .build();
        ReflectionTestUtils.setField(internalCourse, "id", 101L);

        approver = User.builder().username("dean_morris").build();
        ReflectionTestUtils.setField(approver, "id", 2L);
    }

    @Test
    @DisplayName("Should successfully credit external course and unlock prerequisite DAG")
    void creditTransfereeCourses_Success() {
        CreditCourseItemRequest item = new CreditCourseItemRequest(
                "Technological University of the Philippines",
                "IT-101",
                "Intro to IT and Computing",
                101L,
                new BigDecimal("1.50"),
                new BigDecimal("3.00"),
                "Accredited under CMO 25 equivalency matrix"
        );
        CreditTransfereeCoursesRequest request = new CreditTransfereeCoursesRequest(List.of(item));

        given(studentProfileRepository.findByIdWithProgramAndCurriculum(60L)).willReturn(Optional.of(student));
        given(userRepository.findById(2L)).willReturn(Optional.of(approver));
        given(courseRepository.findById(101L)).willReturn(Optional.of(internalCourse));

        CourseEquivalency savedEquiv = CourseEquivalency.builder()
                .student(student)
                .externalInstitution(item.externalInstitution())
                .externalCourseCode(item.externalCourseCode())
                .externalCourseTitle(item.externalCourseTitle())
                .internalCourse(internalCourse)
                .externalNumericalGrade(item.externalNumericalGrade())
                .creditsGranted(item.creditsGranted())
                .status(CourseEquivalency.Status.APPROVED)
                .approvedBy(approver)
                .build();
        ReflectionTestUtils.setField(savedEquiv, "id", 999L);
        given(equivalencyRepository.save(any(CourseEquivalency.class))).willReturn(savedEquiv);

        given(studentCourseGradeRepository.findByStudentIdAndCourseId(60L, 101L)).willReturn(Optional.empty());

        StudentCourseGrade creditedGrade = StudentCourseGrade.builder()
                .student(student)
                .course(internalCourse)
                .numericalGrade(new BigDecimal("1.50"))
                .completionStatus("PASSED")
                .isCredited(true)
                .build();
        given(studentCourseGradeRepository.findPassedGradesByStudentId(60L)).willReturn(List.of(creditedGrade));

        TransfereeCreditingSummaryResponse response = creditingService.creditTransfereeCourses(60L, request, 2L);

        assertThat(response).isNotNull();
        assertThat(response.studentId()).isEqualTo(60L);
        assertThat(response.creditedCoursesCount()).isEqualTo(1);
        assertThat(response.totalUnitsCredited()).isEqualByComparingTo("3.00");
        assertThat(response.creditedCourses()).hasSize(1);
        assertThat(response.creditedCourses().get(0).internalCourseCode()).isEqualTo("CC-101");
        assertThat(response.creditedCourses().get(0).status()).isEqualTo("APPROVED");

        verify(equivalencyRepository).save(any(CourseEquivalency.class));
        verify(studentCourseGradeRepository).save(any(StudentCourseGrade.class));
        verify(studentProfileRepository).save(student);
        assertThat(student.getTotalUnitsEarned()).isEqualByComparingTo("3.00");
    }
}
