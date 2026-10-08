package com.sdt.web_app.service.compliance;

import com.sdt.web_app.config.WebSocketBroadcastService;
import com.sdt.web_app.config.WebSocketTopics;
import com.sdt.web_app.dto.compliance.ComplianceDtos.ClearanceRequestDto;
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
class ClearanceDataConsistencyTest {

    @Mock
    private ClearanceRequestRepository clearanceRequestRepository;

    @Mock
    private ClearanceSignoffRepository clearanceSignoffRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebSocketBroadcastService broadcastService;

    @InjectMocks
    private ClearanceWorkflowService clearanceWorkflowService;

    private StudentProfile studentProfile;
    private Term term;
    private User adminUser;
    private ClearanceRequest clearanceRequest;

    @BeforeEach
    void setUp() {
        adminUser = User.builder().id(1L).username("admin_officer").build();
        adminUser.addRole(Roles.ADMIN);

        User studentUser = User.builder().id(18L).username("student_a").build();
        studentUser.addRole(Roles.STUDENT);

        studentProfile = StudentProfile.builder()
                .id(10L)
                .studentNumber("2026-0001")
                .user(studentUser)
                .build();

        term = Term.builder().id(10L).build();

        clearanceRequest = ClearanceRequest.builder()
                .id(50L)
                .studentProfile(studentProfile)
                .term(term)
                .purpose("GRADUATION")
                .overallStatus("PENDING")
                .signoffs(new ArrayList<>())
                .build();

        ClearanceSignoff librarySignoff = ClearanceSignoff.builder()
                .id(101L)
                .clearanceRequest(clearanceRequest)
                .departmentType("LIBRARY")
                .signoffStatus("PENDING")
                .build();

        ClearanceSignoff acctSignoff = ClearanceSignoff.builder()
                .id(102L)
                .clearanceRequest(clearanceRequest)
                .departmentType("ACCOUNTING")
                .signoffStatus("PENDING")
                .build();

        clearanceRequest.getSignoffs().add(librarySignoff);
        clearanceRequest.getSignoffs().add(acctSignoff);
    }

    @Test
    @DisplayName("Route A (Student Number) and Route B (Student Profile ID) return identical clearance DTO")
    void testTwoRouteStudentIdentityResolutionConsistency() {
        when(studentProfileRepository.findByStudentNumber("2026-0001")).thenReturn(Optional.of(studentProfile));
        when(studentProfileRepository.findById(10L)).thenReturn(Optional.of(studentProfile));
        when(clearanceRequestRepository.findByStudentProfileIdAndTermId(10L, 10L)).thenReturn(Optional.of(clearanceRequest));

        // Route A: Staff/Registrar query by studentNumber string
        ClearanceRequestDto dtoByNumber = clearanceWorkflowService.getClearanceByStudentAndTerm("2026-0001", 10L);

        // Route B: Student self-service query by numeric studentProfileId
        ClearanceRequestDto dtoById = clearanceWorkflowService.getClearanceByStudentAndTerm(10L, 10L);

        assertThat(dtoByNumber).isNotNull();
        assertThat(dtoById).isNotNull();
        assertThat(dtoByNumber.id()).isEqualTo(dtoById.id());
        assertThat(dtoByNumber.studentProfileId()).isEqualTo(dtoById.studentProfileId()).isEqualTo(10L);
        assertThat(dtoByNumber.studentNumber()).isEqualTo(dtoById.studentNumber()).isEqualTo("2026-0001");
        assertThat(dtoByNumber.overallStatus()).isEqualTo(dtoById.overallStatus()).isEqualTo("PENDING");
        assertThat(dtoByNumber.signoffs()).hasSize(dtoById.signoffs().size());
    }

    @Test
    @DisplayName("Signoff update delivers identical payload to both student topic and admin topic")
    void testSignoffWebSocketDualDeliveryConsistency() {
        ClearanceSignoff libSignoff = clearanceRequest.getSignoffs().get(0);
        when(clearanceSignoffRepository.findById(101L)).thenReturn(Optional.of(libSignoff));
        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));
        when(clearanceSignoffRepository.save(any(ClearanceSignoff.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessSignoffRequest req = new ProcessSignoffRequest("APPROVED", "All library dues cleared");
        clearanceWorkflowService.processSignoff(101L, req, 1L);

        ArgumentCaptor<ClearanceStatusMessage> studentCaptor = ArgumentCaptor.forClass(ClearanceStatusMessage.class);
        ArgumentCaptor<ClearanceStatusMessage> adminCaptor = ArgumentCaptor.forClass(ClearanceStatusMessage.class);

        verify(broadcastService).broadcast(eq(WebSocketTopics.clearance(10L)), studentCaptor.capture());
        verify(broadcastService).broadcast(eq(WebSocketTopics.ADMIN_CLEARANCE), adminCaptor.capture());

        ClearanceStatusMessage studentMsg = studentCaptor.getValue();
        ClearanceStatusMessage adminMsg = adminCaptor.getValue();

        assertThat(studentMsg.studentId()).isEqualTo(adminMsg.studentId()).isEqualTo(10L);
        assertThat(studentMsg.termId()).isEqualTo(adminMsg.termId()).isEqualTo(10L);
        assertThat(studentMsg.overallStatus()).isEqualTo(adminMsg.overallStatus()).isEqualTo("PENDING");
        assertThat(studentMsg.departments()).hasSize(adminMsg.departments().size());

        ClearanceStatusMessage.DepartmentClearanceItem libItem = studentMsg.departments().stream()
                .filter(d -> "LIBRARY".equalsIgnoreCase(d.departmentName()))
                .findFirst()
                .orElse(null);

        assertThat(libItem).isNotNull();
        assertThat(libItem.status()).isEqualTo("APPROVED");
        assertThat(libItem.remarks()).isEqualTo("All library dues cleared");
        assertThat(libItem.clearedBy()).isEqualTo("admin_officer");
    }
}
