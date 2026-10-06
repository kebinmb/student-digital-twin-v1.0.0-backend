package com.sdt.web_app.service.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class SectionEventPublisherService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    // Map of termId -> list of active emitters. termId = 0L represents all terms.
    private final Map<Long, List<SseEmitter>> termEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribeToTermSectionEvents(Long termId) {
        Long key = (termId != null && termId > 0) ? termId : 0L;
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        termEmitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(() -> removeEmitter(key, emitter));
        emitter.onError(e -> removeEmitter(key, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data(Map.of("status", "CONNECTED", "termId", key)));
        } catch (IOException e) {
            removeEmitter(key, emitter);
        }

        return emitter;
    }

    private void removeEmitter(Long key, SseEmitter emitter) {
        List<SseEmitter> list = termEmitters.get(key);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                termEmitters.remove(key);
            }
        }
    }

    public void publishSectionEnlistmentEvent(Long termId, Long sectionId, String sectionCode, int enrolledCount, int maxCapacity) {
        Map<String, Object> data = Map.of(
                "eventType", "ENLISTMENT_UPDATE",
                "termId", termId != null ? termId : 0L,
                "sectionId", sectionId,
                "sectionCode", sectionCode != null ? sectionCode : "",
                "enrolledCount", enrolledCount,
                "maxCapacity", maxCapacity
        );
        broadcast(termId, "enlistment-updated", data);
    }

    public void publishGradeStatusEvent(Long termId, Long sectionId, String sectionCode, String gradeStatus) {
        Map<String, Object> data = Map.of(
                "eventType", "GRADE_STATUS_UPDATE",
                "termId", termId != null ? termId : 0L,
                "sectionId", sectionId,
                "sectionCode", sectionCode != null ? sectionCode : "",
                "gradeStatus", gradeStatus != null ? gradeStatus : ""
        );
        broadcast(termId, "grade-status-updated", data);
    }

    public void dispatchLocally(Long termId, String eventName, Object data) {
        broadcast(termId, eventName, data);
    }

    private void broadcast(Long termId, String eventName, Object data) {
        sendToKey(termId, eventName, data);
        if (termId != null && termId > 0) {
            sendToKey(0L, eventName, data); // Global listeners
        }
    }

    private void sendToKey(Long key, String eventName, Object data) {
        if (key == null) return;
        List<SseEmitter> list = termEmitters.get(key);
        if (list == null || list.isEmpty()) return;

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        if (!deadEmitters.isEmpty()) {
            list.removeAll(deadEmitters);
        }
    }
}
