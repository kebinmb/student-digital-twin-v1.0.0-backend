package com.sdt.web_app.service.compliance;

import com.sdt.web_app.config.WebSocketBroadcastService;
import com.sdt.web_app.config.WebSocketTopics;
import com.sdt.web_app.controller.compliance.ClearanceWebSocketController;
import com.sdt.web_app.service.compliance.ClearanceWorkflowService;
import com.sdt.web_app.websocket.dto.ClearanceStatusMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClearanceWebSocketDeliveryTest {

    @Mock
    private ClearanceWorkflowService clearanceWorkflowService;

    @Mock
    private WebSocketBroadcastService broadcastService;

    @InjectMocks
    private ClearanceWebSocketController clearanceWebSocketController;

    @Test
    @DisplayName("Should deliver clearance snapshot for student to correct topic with student isolation")
    void shouldDeliverClearanceSnapshotToCorrectTopic() {
        Long studentId = 42L;
        Long termId = 11L;

        ClearanceStatusMessage.DepartmentClearanceItem libItem = new ClearanceStatusMessage.DepartmentClearanceItem(
                1L, "LIBRARY", "APPROVED", "No overdue books", "librarian", LocalDateTime.now()
        );
        ClearanceStatusMessage.DepartmentClearanceItem acctItem = new ClearanceStatusMessage.DepartmentClearanceItem(
                2L, "ACCOUNTING", "PENDING", "Pending balance check", null, null
        );

        ClearanceStatusMessage expectedMessage = new ClearanceStatusMessage(
                studentId, termId, "PENDING", List.of(libItem, acctItem)
        );

        when(clearanceWorkflowService.getClearanceAsMessage(studentId, termId)).thenReturn(expectedMessage);

        ClearanceStatusMessage response = clearanceWebSocketController.getClearanceStatus(studentId, termId);

        assertThat(response).isNotNull();
        assertThat(response.studentId()).isEqualTo(42L);
        assertThat(response.termId()).isEqualTo(11L);
        assertThat(response.overallStatus()).isEqualTo("PENDING");
        assertThat(response.departments()).hasSize(2);

        // Verify broadcast goes to the specific student's topic (isolation)
        verify(broadcastService, times(1)).broadcast("/topic/clearance.42", expectedMessage);
        verify(broadcastService, never()).broadcast(eq("/topic/clearance.43"), any());
    }

    @Test
    @DisplayName("Should handle missing clearance request gracefully with uninitiated status")
    void shouldHandleMissingClearanceGracefully() {
        Long studentId = 99L;
        Long termId = 12L;

        ClearanceStatusMessage defaultMessage = new ClearanceStatusMessage(
                studentId, termId, "NOT_INITIATED", List.of()
        );

        when(clearanceWorkflowService.getClearanceAsMessage(studentId, termId)).thenReturn(defaultMessage);

        ClearanceStatusMessage response = clearanceWebSocketController.getClearanceStatus(studentId, termId);

        assertThat(response).isNotNull();
        assertThat(response.overallStatus()).isEqualTo("NOT_INITIATED");
        verify(broadcastService).broadcast("/topic/clearance.99", defaultMessage);
    }
}
