package com.sdt.web_app.controller.compliance;

import com.sdt.web_app.config.WebSocketBroadcastService;
import com.sdt.web_app.config.WebSocketTopics;
import com.sdt.web_app.service.compliance.ClearanceWorkflowService;
import com.sdt.web_app.websocket.dto.ClearanceStatusMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ClearanceWebSocketController {

    private final ClearanceWorkflowService clearanceWorkflowService;
    private final WebSocketBroadcastService broadcastService;

    /**
     * Handles STOMP incoming message on /app/clearance/{studentId}/{termId}
     * and broadcasts multi-department status to /topic/clearance.{studentId}.
     */
    @MessageMapping("/clearance/{studentId}/{termId}")
    public ClearanceStatusMessage getClearanceStatus(
            @DestinationVariable Long studentId,
            @DestinationVariable Long termId) {
        log.debug("[WebSocket] Handling /app/clearance/{}/{}", studentId, termId);
        ClearanceStatusMessage message = clearanceWorkflowService.getClearanceAsMessage(studentId, termId);
        broadcastService.broadcast(WebSocketTopics.clearance(studentId), message);
        return message;
    }
}
