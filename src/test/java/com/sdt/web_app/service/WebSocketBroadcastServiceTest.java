package com.sdt.web_app.service;

import com.sdt.web_app.config.WebSocketBroadcastService;
import com.sdt.web_app.config.WebSocketTopics;
import com.sdt.web_app.websocket.dto.ActiveTermMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketBroadcastServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketBroadcastService broadcastService;

    @Test
    @DisplayName("Should convert and send message to specified topic")
    void shouldBroadcastToTopic() {
        ActiveTermMessage message = new ActiveTermMessage(
                1L, 1L, "AY-2026-2027", "FIRST_SEM", "First Semester",
                LocalDate.now(), LocalDate.now().plusMonths(5),
                true, true, true, true, true
        );

        broadcastService.broadcast(WebSocketTopics.ACTIVE_TERM, message);

        verify(messagingTemplate).convertAndSend(WebSocketTopics.ACTIVE_TERM, message);
    }

    @Test
    @DisplayName("Should send message to specific user")
    void shouldSendToUser() {
        String username = "student1";
        String destination = "/queue/alerts";
        String payload = "Test alert";

        broadcastService.sendToUser(username, destination, payload);

        verify(messagingTemplate).convertAndSendToUser(username, destination, payload);
    }
}
