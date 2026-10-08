package com.sdt.web_app.service.compliance;

import com.sdt.web_app.config.WebSocketBroadcastService;
import com.sdt.web_app.config.WebSocketTopics;
import com.sdt.web_app.dto.compliance.ComplianceDtos.InitiateClearanceRequest;
import com.sdt.web_app.dto.compliance.ComplianceDtos.ProcessSignoffRequest;
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
import com.sdt.web_app.websocket.dto.ClearanceStatusMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClearanceBroadcastTest {

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

    @Mock
    private com.sdt.web_app.service.lms.StudentNotificationPublisherService studentNotificationPublisherService;

    @Mock
    private WebSocketBroadcastService broadcastService;

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
                .id(7L)
                .studentNumber("2026-0007")
                .user(actor)
                .build();

        term = Term.builder().id(100L).build();
        lenient().when(termService.getTermById(any())).thenReturn(term);
    }

    @Test
    @DisplayName("Initiating clearance request broadcasts multi-department snapshot to student and admin topics")
    void testInitiateClearanceBroadcast() {
        InitiateClearanceRequest req = new InitiateClearanceRequest(7L, 100L, "GRADUATION");

        when(studentProfileRepository.findById(7L)).thenReturn(Optional.of(studentProfile));
        when(clearanceRequestRepository.findByStudentProfileIdAndTermId(7L, 100L)).thenReturn(Optional.empty());
        when(clearanceRequestRepository.save(any(ClearanceRequest.class))).thenAnswer(invocation -> {
            ClearanceRequest c = invocation.getArgument(0);
            c.setId(88L);
            return c;
        });

        clearanceWorkflowService.initiateClearanceRequest(req, 10L);

        ArgumentCaptor<ClearanceStatusMessage> captor = ArgumentCaptor.forClass(ClearanceStatusMessage.class);
        verify(broadcastService).broadcast(eq(WebSocketTopics.clearance(7L)), captor.capture());
        verify(broadcastService).broadcast(eq(WebSocketTopics.ADMIN_CLEARANCE), any(ClearanceStatusMessage.class));

        ClearanceStatusMessage msg = captor.getValue();
        assertThat(msg.studentId()).isEqualTo(7L);
        assertThat(msg.termId()).isEqualTo(100L);
        assertThat(msg.overallStatus()).isEqualTo("PENDING");
        assertThat(msg.departments()).hasSize(5);
        assertThat(msg.departments()).extracting("departmentName")
                .containsExactlyInAnyOrder("LIBRARY", "ACCOUNTING", "LABORATORY", "STUDENT_AFFAIRS", "DEAN");
    }

    @Test
    @DisplayName("Approving signoff broadcasts updated multi-department snapshot to student and admin topics")
    void testProcessSignoffApprovedBroadcast() {
        ClearanceRequest request = ClearanceRequest.builder()
                .id(88L)
                .studentProfile(studentProfile)
                .term(term)
                .purpose("GRADUATION")
                .overallStatus("PENDING")
                .signoffs(new ArrayList<>())
                .build();

        ClearanceSignoff signoff = ClearanceSignoff.builder()
                .id(201L)
                .clearanceRequest(request)
                .departmentType("LIBRARY")
                .signoffStatus("PENDING")
                .build();
        request.getSignoffs().add(signoff);

        when(clearanceSignoffRepository.findById(201L)).thenReturn(Optional.of(signoff));
        when(userRepository.findById(10L)).thenReturn(Optional.of(actor));
        when(clearanceSignoffRepository.save(any(ClearanceSignoff.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessSignoffRequest processReq = new ProcessSignoffRequest("APPROVED", "All books returned");
        clearanceWorkflowService.processSignoff(201L, processReq, 10L);

        ArgumentCaptor<ClearanceStatusMessage> captor = ArgumentCaptor.forClass(ClearanceStatusMessage.class);
        verify(broadcastService).broadcast(eq(WebSocketTopics.clearance(7L)), captor.capture());
        verify(broadcastService).broadcast(eq(WebSocketTopics.ADMIN_CLEARANCE), any(ClearanceStatusMessage.class));

        ClearanceStatusMessage msg = captor.getValue();
        assertThat(msg.studentId()).isEqualTo(7L);
        assertThat(msg.departments()).isNotEmpty();
        assertThat(msg.departments().get(0).departmentName()).isEqualTo("LIBRARY");
        assertThat(msg.departments().get(0).status()).isEqualTo("APPROVED");
        assertThat(msg.departments().get(0).remarks()).isEqualTo("All books returned");
    }

    @Test
    @DisplayName("Cascading grade sealing broadcasts DEAN clearance approval to student topic")
    void testCascadeGradeSealingBroadcast() {
        ClearanceRequest request = ClearanceRequest.builder()
                .id(88L)
                .studentProfile(studentProfile)
                .term(term)
                .purpose("GRADUATION")
                .overallStatus("PENDING")
                .signoffs(new ArrayList<>())
                .build();

        ClearanceSignoff deanSignoff = ClearanceSignoff.builder()
                .id(205L)
                .clearanceRequest(request)
                .departmentType("DEAN")
                .signoffStatus("PENDING")
                .build();
        request.getSignoffs().add(deanSignoff);

        when(clearanceRequestRepository.findByStudentProfileIdAndTermId(7L, 100L)).thenReturn(Optional.of(request));
        when(clearanceSignoffRepository.save(any(ClearanceSignoff.class))).thenAnswer(inv -> inv.getArgument(0));

        clearanceWorkflowService.cascadeGradeSealingToClearance(7L, 100L);

        ArgumentCaptor<ClearanceStatusMessage> captor = ArgumentCaptor.forClass(ClearanceStatusMessage.class);
        verify(broadcastService).broadcast(eq(WebSocketTopics.clearance(7L)), captor.capture());
        verify(broadcastService).broadcast(eq(WebSocketTopics.ADMIN_CLEARANCE), any(ClearanceStatusMessage.class));

        ClearanceStatusMessage msg = captor.getValue();
        assertThat(msg.studentId()).isEqualTo(7L);
        assertThat(msg.departments().get(0).departmentName()).isEqualTo("DEAN");
        assertThat(msg.departments().get(0).status()).isEqualTo("APPROVED");
    }
}
