package com.sdt.web_app.service.compliance;

import com.sdt.web_app.dto.compliance.ComplianceDtos.*;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.compliance.ClearanceRequest;
import com.sdt.web_app.entities.compliance.ClearanceSignoff;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.compliance.ClearanceRequestRepository;
import com.sdt.web_app.repositories.compliance.ClearanceSignoffRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClearanceWorkflowServiceTest {

    @Mock
    private ClearanceRequestRepository clearanceRequestRepository;

    @Mock
    private ClearanceSignoffRepository clearanceSignoffRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private TermRepository termRepository;

    @Mock
    private com.sdt.web_app.service.institution.TermService termService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ClearanceWorkflowService clearanceWorkflowService;

    private StudentProfile studentProfile;
    private Term term;
    private User actor;

    @BeforeEach
    void setUp() {
        actor = User.builder().id(10L).username("admin").build();
        actor.addRole(Roles.ADMIN);

        studentProfile = StudentProfile.builder()
                .id(1L)
                .studentNumber("2026-0001")
                .user(actor)
                .build();

        term = Term.builder().id(100L).build();
        lenient().when(termService.getTermById(any())).thenReturn(term);
    }

    @Test
    @DisplayName("initiateClearanceRequest creates request with 5 department signoffs")
    void testInitiateClearanceRequest() {
        InitiateClearanceRequest req = new InitiateClearanceRequest(1L, 100L, "GRADUATION");

        when(studentProfileRepository.findById(1L)).thenReturn(Optional.of(studentProfile));
        when(clearanceRequestRepository.findByStudentProfileIdAndTermId(1L, 100L)).thenReturn(Optional.empty());

        when(clearanceRequestRepository.save(any(ClearanceRequest.class))).thenAnswer(invocation -> {
            ClearanceRequest c = invocation.getArgument(0);
            c.setId(50L);
            return c;
        });

        ClearanceRequestDto result = clearanceWorkflowService.initiateClearanceRequest(req, 10L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(50L);
        assertThat(result.purpose()).isEqualTo("GRADUATION");
        assertThat(result.overallStatus()).isEqualTo("PENDING");
        assertThat(result.signoffs()).hasSize(5);

        verify(clearanceRequestRepository, times(1)).save(any(ClearanceRequest.class));
    }

    @Test
    @DisplayName("processSignoff updates signoff status and marks overall clearance CLEARED when all approved")
    void testProcessSignoffAllApproved() {
        ClearanceRequest request = ClearanceRequest.builder()
                .id(50L)
                .studentProfile(studentProfile)
                .term(term)
                .purpose("GRADUATION")
                .overallStatus("PENDING")
                .signoffs(new ArrayList<>())
                .build();

        ClearanceSignoff signoff = ClearanceSignoff.builder()
                .id(101L)
                .clearanceRequest(request)
                .departmentType("LIBRARY")
                .signoffStatus("PENDING")
                .build();

        request.getSignoffs().add(signoff);

        when(clearanceSignoffRepository.findById(101L)).thenReturn(Optional.of(signoff));
        when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
        when(clearanceSignoffRepository.save(any(ClearanceSignoff.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessSignoffRequest processReq = new ProcessSignoffRequest("APPROVED", "No unreturned books");
        ClearanceSignoffDto result = clearanceWorkflowService.processSignoff(101L, processReq, 10L);

        assertThat(result.signoffStatus()).isEqualTo("APPROVED");
        assertThat(request.getOverallStatus()).isEqualTo("CLEARED");
    }

    @Test
    @DisplayName("processSignoff throws AccessDeniedException when role does not match department")
    void testProcessSignoff_UnauthorizedRole_ThrowsAccessDeniedException() {
        User deanUser = User.builder().id(20L).username("dean_smith").build();
        deanUser.addRole(Roles.DEAN);

        ClearanceRequest request = ClearanceRequest.builder()
                .id(50L)
                .studentProfile(studentProfile)
                .term(term)
                .purpose("GRADUATION")
                .overallStatus("PENDING")
                .signoffs(new ArrayList<>())
                .build();

        ClearanceSignoff signoff = ClearanceSignoff.builder()
                .id(102L)
                .clearanceRequest(request)
                .departmentType("ACCOUNTING")
                .signoffStatus("PENDING")
                .build();

        when(clearanceSignoffRepository.findById(102L)).thenReturn(Optional.of(signoff));
        when(userRepository.findById(20L)).thenReturn(Optional.of(deanUser));

        ProcessSignoffRequest processReq = new ProcessSignoffRequest("APPROVED", "Cleared");

        assertThatThrownBy(() -> clearanceWorkflowService.processSignoff(102L, processReq, 20L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("is not authorized to sign off for department: ACCOUNTING");
    }

    @Test
    @DisplayName("processSignoff transitions REJECTED clearance to PENDING when rejected signoff is approved but other signoffs are pending")
    void testProcessSignoff_RejectedToPending_WhenOtherSignoffsPending() {
        ClearanceRequest request = ClearanceRequest.builder()
                .id(50L)
                .studentProfile(studentProfile)
                .term(term)
                .purpose("GRADUATION")
                .overallStatus("REJECTED")
                .signoffs(new ArrayList<>())
                .build();

        ClearanceSignoff signoff1 = ClearanceSignoff.builder()
                .id(101L)
                .clearanceRequest(request)
                .departmentType("LIBRARY")
                .signoffStatus("REJECTED")
                .remarks("Unreturned books")
                .build();

        ClearanceSignoff signoff2 = ClearanceSignoff.builder()
                .id(102L)
                .clearanceRequest(request)
                .departmentType("ACCOUNTING")
                .signoffStatus("PENDING")
                .build();

        request.getSignoffs().add(signoff1);
        request.getSignoffs().add(signoff2);

        when(clearanceSignoffRepository.findById(101L)).thenReturn(Optional.of(signoff1));
        when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
        when(clearanceSignoffRepository.save(any(ClearanceSignoff.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessSignoffRequest processReq = new ProcessSignoffRequest("APPROVED", "Books returned");
        ClearanceSignoffDto result = clearanceWorkflowService.processSignoff(101L, processReq, 10L);

        assertThat(result.signoffStatus()).isEqualTo("APPROVED");
        assertThat(request.getOverallStatus()).isEqualTo("PENDING");
    }

    @Test
    @DisplayName("processSignoff transitions REJECTED clearance to CLEARED when all signoffs become approved")
    void testProcessSignoff_RejectedToCleared_WhenAllSignoffsApproved() {
        ClearanceRequest request = ClearanceRequest.builder()
                .id(50L)
                .studentProfile(studentProfile)
                .term(term)
                .purpose("GRADUATION")
                .overallStatus("REJECTED")
                .signoffs(new ArrayList<>())
                .build();

        ClearanceSignoff signoff1 = ClearanceSignoff.builder()
                .id(101L)
                .clearanceRequest(request)
                .departmentType("LIBRARY")
                .signoffStatus("REJECTED")
                .build();

        ClearanceSignoff signoff2 = ClearanceSignoff.builder()
                .id(102L)
                .clearanceRequest(request)
                .departmentType("ACCOUNTING")
                .signoffStatus("APPROVED")
                .build();

        request.getSignoffs().add(signoff1);
        request.getSignoffs().add(signoff2);

        when(clearanceSignoffRepository.findById(101L)).thenReturn(Optional.of(signoff1));
        when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
        when(clearanceSignoffRepository.save(any(ClearanceSignoff.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessSignoffRequest processReq = new ProcessSignoffRequest("APPROVED", "Books returned & fines paid");
        ClearanceSignoffDto result = clearanceWorkflowService.processSignoff(101L, processReq, 10L);

        assertThat(result.signoffStatus()).isEqualTo("APPROVED");
        assertThat(request.getOverallStatus()).isEqualTo("CLEARED");
    }
}
