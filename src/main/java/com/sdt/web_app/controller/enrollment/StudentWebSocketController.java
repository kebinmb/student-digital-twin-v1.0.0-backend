package com.sdt.web_app.controller.enrollment;

import com.sdt.web_app.config.WebSocketBroadcastService;
import com.sdt.web_app.config.WebSocketTopics;
import com.sdt.web_app.service.enrollment.StudentService;
import com.sdt.web_app.websocket.dto.StudentProfileMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StudentWebSocketController {

    private final StudentService studentService;
    private final WebSocketBroadcastService broadcastService;

    /**
     * Handles STOMP incoming message on /app/student/{id}
     * and broadcasts student profile to /topic/student.{id}.
     */
    @MessageMapping("/student/{id}")
    public StudentProfileMessage getStudentProfile(@DestinationVariable Long id) {
        log.debug("[WebSocket] Handling /app/student/{}", id);
        StudentProfileMessage message = studentService.getStudentAsMessage(id);
        if (message != null) {
            broadcastService.broadcast(WebSocketTopics.student(id), message);
        }
        return message;
    }
}
