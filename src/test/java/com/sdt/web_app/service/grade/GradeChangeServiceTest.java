package com.sdt.web_app.service.grade;

import com.sdt.web_app.dto.grade.GradeChangeDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.GradeChangeRequest;
import com.sdt.web_app.entities.enrollment.StudentCourseGrade;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.GradeChangeRequestRepository;
import com.sdt.web_app.repositories.enrollment.StudentCourseGradeRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.CourseRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.entities.institution.TermType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradeChangeServiceTest {

    @Mock private GradeChangeRequestRepository requestRepository;
    @Mock private StudentProfileRepository profileRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private TermRepository termRepository;
    @Mock private com.sdt.web_app.service.institution.TermService termService;
    @Mock private UserRepository userRepository;
    @Mock private StudentCourseGradeRepository gradeRepository;
    @Mock private com.sdt.web_app.repositories.enrollment.EnrollmentCourseItemRepository itemRepository;
    @Mock private com.sdt.web_app.repositories.grade.GradeSealingAuditRepository sealingAuditRepository;
    @Mock private com.sdt.web_app.service.security.AcademicScopeAssertionService academicScopeAssertionService;
    @Mock private com.sdt.web_app.config.WebSocketBroadcastService broadcastService;

    @InjectMocks
    private GradeChangeService gradeChangeService;

    private StudentProfile mockStudent;
    private Course mockCourse;
    private Term mockTerm;
    private User mockUser;
    private GradeChangeRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).username("instructor1").build();
        mockStudent = StudentProfile.builder().id(10L).studentNumber("2024-0001").user(mockUser).build();
        mockCourse = Course.builder().id(100L).code("CS101").title("Intro to CS").creditUnits(new BigDecimal("3.00")).build();
        mockTerm = Term.builder().id(50L).termType(TermType.FIRST_SEM).build();
        lenient().when(termService.getTermById(any())).thenReturn(mockTerm);


        mockRequest = GradeChangeRequest.builder()
                .id(500L)
                .student(mockStudent)
                .course(mockCourse)
                .term(mockTerm)
                .previousGrade(new BigDecimal("3.00"))
                .newGrade(new BigDecimal("1.75"))
                .reason("Encoding error on final exam component")
                .status(GradeChangeRequest.Status.PENDING)
                .requestedBy(mockUser)
                .build();
    }

    @Test
    @DisplayName("Submit grade change request successfully")
    void submitRequest_Success() {
        when(profileRepository.findById(10L)).thenReturn(Optional.of(mockStudent));
        when(courseRepository.findById(100L)).thenReturn(Optional.of(mockCourse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        when(requestRepository.save(any(GradeChangeRequest.class))).thenReturn(mockRequest);

        CreateGradeChangeRequest req = new CreateGradeChangeRequest(
                10L, 100L, 50L, new BigDecimal("3.00"), new BigDecimal("1.75"), "Encoding error on final exam component"
        );

        GradeChangeResponse res = gradeChangeService.submitRequest(req, 1L);

        assertThat(res).isNotNull();
        assertThat(res.id()).isEqualTo(500L);
        assertThat(res.newGrade()).isEqualTo(new BigDecimal("1.75"));
        assertThat(res.status()).isEqualTo("PENDING");
    }


    @Test
    @DisplayName("Approve grade change request updates grade and GPA")
    void approveRequest_Success() {
        when(requestRepository.findById(500L)).thenReturn(Optional.of(mockRequest));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));
        lenient().when(gradeRepository.findByStudentIdAndCourseIdAndTermId(10L, 100L, 50L)).thenReturn(Optional.empty());
        lenient().when(gradeRepository.findByStudentIdAndCourseId(10L, 100L)).thenReturn(Optional.empty());
        when(gradeRepository.findPassedGradesByStudentId(10L)).thenReturn(Collections.emptyList());
        when(requestRepository.save(any(GradeChangeRequest.class))).thenReturn(mockRequest);

        GradeChangeResponse res = gradeChangeService.approveRequest(500L, 1L);

        assertThat(res).isNotNull();
        verify(gradeRepository).save(any(StudentCourseGrade.class));
        verify(profileRepository).save(mockStudent);
    }

    @Test
    @DisplayName("getPendingRequests filters by programId for CHAIRPERSON")
    void getPendingRequests_Chairperson_ProgramScoped() {
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("chairperson");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        com.sdt.web_app.service.security.AcademicScopeContext scope =
                com.sdt.web_app.service.security.AcademicScopeContext.chairperson(1L, 2L, 5L);
        when(academicScopeAssertionService.assertAndResolveScope(auth)).thenReturn(scope);
        when(requestRepository.findPendingByProgramId(GradeChangeRequest.Status.PENDING, 5L, 50L))
                .thenReturn(List.of(mockRequest));

        List<GradeChangeResponse> responses = gradeChangeService.getPendingRequests(50L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(500L);
        verify(requestRepository).findPendingByProgramId(GradeChangeRequest.Status.PENDING, 5L, 50L);
    }

    @Test
    @DisplayName("getPendingRequests filters by collegeId for DEAN")
    void getPendingRequests_Dean_CollegeScoped() {
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("dean");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        com.sdt.web_app.service.security.AcademicScopeContext scope =
                com.sdt.web_app.service.security.AcademicScopeContext.dean(1L, 2L, List.of(5L, 6L));
        when(academicScopeAssertionService.assertAndResolveScope(auth)).thenReturn(scope);
        when(requestRepository.findPendingByCollegeId(GradeChangeRequest.Status.PENDING, 2L, 50L))
                .thenReturn(List.of(mockRequest));

        List<GradeChangeResponse> responses = gradeChangeService.getPendingRequests(50L);

        assertThat(responses).hasSize(1);
        verify(requestRepository).findPendingByCollegeId(GradeChangeRequest.Status.PENDING, 2L, 50L);
    }

    @Test
    @DisplayName("getPendingRequests returns unrestricted results for ADMIN")
    void getPendingRequests_Admin_Unrestricted() {
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("admin");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        com.sdt.web_app.service.security.AcademicScopeContext scope =
                com.sdt.web_app.service.security.AcademicScopeContext.unrestricted(1L);
        when(academicScopeAssertionService.assertAndResolveScope(auth)).thenReturn(scope);
        when(requestRepository.findByStatusWithDetails(GradeChangeRequest.Status.PENDING, 50L))
                .thenReturn(List.of(mockRequest));

        List<GradeChangeResponse> responses = gradeChangeService.getPendingRequests(50L);

        assertThat(responses).hasSize(1);
        verify(requestRepository).findByStatusWithDetails(GradeChangeRequest.Status.PENDING, 50L);
    }
}
