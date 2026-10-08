package com.sdt.web_app.controller.institution;

import com.sdt.web_app.config.WebSocketTopics;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.service.institution.TermLifecycleService;
import com.sdt.web_app.websocket.dto.ActiveTermMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TermWebSocketController {

    private final TermLifecycleService termLifecycleService;

    /**
     * Handles STOMP incoming message on /app/terms/active and broadcasts to /topic/terms/active.
     */
    @MessageMapping("/terms/active")
    @SendTo(WebSocketTopics.ACTIVE_TERM)
    public ActiveTermMessage getActiveTerm() {
        Term term = termLifecycleService.getActiveTerm();
        log.debug("[WebSocket] Received request on /app/terms/active. Returning active term: {}", term.getId());
        return new ActiveTermMessage(
                term.getId(),
                term.getAcademicYear().getId(),
                term.getAcademicYear().getCode(),
                term.getTermType().name(),
                term.getTermType().name(),
                term.getStartDate(),
                term.getEndDate(),
                term.getAcademicYear().isCurrent(),
                term.isActive(),
                term.isEnrollmentOpen(),
                term.isGradingOpen(),
                term.isAddDropOpen()
        );
    }
}
