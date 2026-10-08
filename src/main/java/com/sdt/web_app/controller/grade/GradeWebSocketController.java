package com.sdt.web_app.controller.grade;

import com.sdt.web_app.config.WebSocketBroadcastService;
import com.sdt.web_app.config.WebSocketTopics;
import com.sdt.web_app.entities.enrollment.StudentCourseGrade;
import com.sdt.web_app.repositories.enrollment.StudentCourseGradeRepository;
import com.sdt.web_app.websocket.dto.GradeUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GradeWebSocketController {

    private final StudentCourseGradeRepository gradeRepository;
    private final WebSocketBroadcastService broadcastService;

    /**
     * Handles STOMP incoming message on /app/grades/{studentId}
     * and broadcasts student grades to /topic/grades.{studentId}.
     */
    @MessageMapping("/grades/{studentId}")
    public void getStudentGrades(@DestinationVariable Long studentId) {
        log.debug("[WebSocket] Handling /app/grades/{}", studentId);
        List<StudentCourseGrade> grades = gradeRepository.findByStudentId(studentId);
        for (StudentCourseGrade g : grades) {
            GradeUpdateMessage msg = new GradeUpdateMessage(
                    studentId,
                    null,
                    g.getCourse() != null ? g.getCourse().getCode() : "",
                    null,
                    g.getNumericalGrade() != null ? g.getNumericalGrade().doubleValue() : null,
                    g.getNumericalGrade() != null ? g.getNumericalGrade().doubleValue() : null,
                    g.getCompletionStatus(),
                    Instant.now()
            );
            broadcastService.broadcast(WebSocketTopics.grades(studentId), msg);
        }
    }
}
