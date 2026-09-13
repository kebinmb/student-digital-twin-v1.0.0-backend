package com.sdt.web_app.service.compliance;

import com.sdt.web_app.dto.compliance.ComplianceDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentCourseGrade;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.CurriculumCourse;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.repositories.compliance.GraduationApplicationRepository;
import com.sdt.web_app.repositories.enrollment.StudentCourseGradeRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.CurriculumCourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DegreeAuditServiceTest {

    @Mock
    private GraduationApplicationRepository graduationApplicationRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private CurriculumCourseRepository curriculumCourseRepository;

    @Mock
    private StudentCourseGradeRepository studentCourseGradeRepository;

    @Mock
    private TermRepository termRepository;

    @Mock
    private com.sdt.web_app.service.institution.TermService termService;

    @InjectMocks
    private DegreeAuditService degreeAuditService;

    private StudentProfile student;
    private Curriculum curriculum;
    private Course course1;
    private Course course2;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(10L).username("graduating_student").build();
        Program program = Program.builder().id(1L).code("BSCS").name("BS Computer Science").build();
        curriculum = Curriculum.builder().id(100L).code("BSCS-2024").build();

        student = StudentProfile.builder()
                .id(1L)
                .studentNumber("2022-0001")
                .user(user)
                .program(program)
                .curriculum(curriculum)
                .build();

        course1 = Course.builder().id(101L).code("CS101").title("Intro to Computing").creditUnits(BigDecimal.valueOf(3.0)).build();
        course2 = Course.builder().id(102L).code("CS102").title("Data Structures").creditUnits(BigDecimal.valueOf(3.0)).build();
    }

    @Test
    @DisplayName("evaluateDegreeAudit calculates GPA and qualifies student for Cum Laude")
    void testEvaluateDegreeAuditQualifiedCumLaude() {
        CurriculumCourse cc1 = CurriculumCourse.builder().id(1L).curriculum(curriculum).course(course1).build();
        CurriculumCourse cc2 = CurriculumCourse.builder().id(2L).curriculum(curriculum).course(course2).build();

        StudentCourseGrade scg1 = StudentCourseGrade.builder().id(1L).student(student).course(course1).numericalGrade(BigDecimal.valueOf(1.25)).completionStatus("PASSED").build();
        StudentCourseGrade scg2 = StudentCourseGrade.builder().id(2L).student(student).course(course2).numericalGrade(BigDecimal.valueOf(1.50)).completionStatus("PASSED").build();

        when(studentProfileRepository.findById(1L)).thenReturn(Optional.of(student));
        when(curriculumCourseRepository.findByCurriculumId(100L)).thenReturn(List.of(cc1, cc2));
        when(studentCourseGradeRepository.findByStudentId(1L)).thenReturn(List.of(scg1, scg2));

        DegreeAuditResultDto result = degreeAuditService.evaluateDegreeAudit(1L);

        assertThat(result).isNotNull();
        assertThat(result.totalCurriculumUnits()).isEqualTo(BigDecimal.valueOf(6.0));
        assertThat(result.totalUnitsEarned()).isEqualTo(BigDecimal.valueOf(6.0));
        assertThat(result.cumulativeGpa()).isEqualTo(BigDecimal.valueOf(1.38));
        assertThat(result.qualifiedForGraduation()).isTrue();
        assertThat(result.honorsEligible()).isEqualTo("MAGNA_CUM_LAUDE");
    }
}
