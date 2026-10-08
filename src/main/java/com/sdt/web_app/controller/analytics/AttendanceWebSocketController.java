package com.sdt.web_app.controller.analytics;

import com.sdt.web_app.config.WebSocketBroadcastService;
import com.sdt.web_app.config.WebSocketTopics;
import com.sdt.web_app.websocket.dto.AttendanceUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.time.LocalDate;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AttendanceWebSocketController {

    private final WebSocketBroadcastService broadcastService;

    /**
     * Handles STOMP incoming message on /app/attendance/{classId}
     * and broadcasts attendance status to /topic/attendance.class.{classId}.
     */
    @MessageMapping("/attendance/{classId}")
    public void getClassAttendance(@DestinationVariable Long classId) {
        log.debug("[WebSocket] Handling /app/attendance/{}", classId);
        AttendanceUpdateMessage msg = new AttendanceUpdateMessage(
                null,
                classId,
                LocalDate.now(),
                "PING",
                "Class attendance topic active",
                Instant.now()
        );
        broadcastService.broadcast(WebSocketTopics.attendanceClass(classId), msg);
    }
}
