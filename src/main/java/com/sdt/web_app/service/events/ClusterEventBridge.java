package com.sdt.web_app.service.events;

import tools.jackson.databind.ObjectMapper;
import com.sdt.web_app.config.RedisClusterStreamingConfig;
import com.sdt.web_app.event.lms.StudentStreamingEvent;
import com.sdt.web_app.event.scheduling.SectionStreamingEvent;
import com.sdt.web_app.service.lms.StudentNotificationPublisherService;
import com.sdt.web_app.service.scheduling.SectionEventPublisherService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClusterEventBridge {

    @Getter
    private final String nodeId = UUID.randomUUID().toString();

    private final ApplicationEventPublisher applicationEventPublisher;
    private final SectionEventPublisherService sectionEventPublisherService;
    private final StudentNotificationPublisherService studentNotificationPublisherService;

    @Autowired(required = false)
    @Setter
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    @Setter
    private ObjectMapper objectMapper;

    public void broadcastSectionEvent(Long termId, String eventName, Object data) {
        SectionStreamingEvent event = new SectionStreamingEvent(termId, eventName, data, nodeId);
        log.debug("Publishing section streaming event from node {}: {} (termId={})", nodeId, eventName, termId);
        applicationEventPublisher.publishEvent(event);

        if (redisTemplate != null && objectMapper != null) {
            try {
                String json = objectMapper.writeValueAsString(event);
                redisTemplate.convertAndSend(RedisClusterStreamingConfig.SECTION_STREAMING_CHANNEL, json);
            } catch (Exception e) {
                log.warn("Failed to broadcast section streaming event to Redis PubSub", e);
            }
        }
    }

    public void broadcastStudentEvent(Long studentId, String eventName, Object data) {
        StudentStreamingEvent event = new StudentStreamingEvent(studentId, eventName, data, nodeId);
        log.debug("Publishing student streaming event from node {}: {} (studentId={})", nodeId, eventName, studentId);
        applicationEventPublisher.publishEvent(event);

        if (redisTemplate != null && objectMapper != null) {
            try {
                String json = objectMapper.writeValueAsString(event);
                redisTemplate.convertAndSend(RedisClusterStreamingConfig.STUDENT_STREAMING_CHANNEL, json);
            } catch (Exception e) {
                log.warn("Failed to broadcast student streaming event to Redis PubSub", e);
            }
        }
    }

    @EventListener
    public void onSectionStreamingEvent(SectionStreamingEvent event) {
        // Only deliver locally if originated from another node in a cluster setup
        if (event != null && !nodeId.equals(event.originNodeId())) {
            log.debug("Received remote cluster section streaming event: {} for term {}", event.eventName(), event.termId());
            sectionEventPublisherService.dispatchLocally(event.termId(), event.eventName(), event.data());
        }
    }

    @EventListener
    public void onStudentStreamingEvent(StudentStreamingEvent event) {
        // Only deliver locally if originated from another node in a cluster setup
        if (event != null && !nodeId.equals(event.originNodeId())) {
            log.debug("Received remote cluster student streaming event: {} for student {}", event.eventName(), event.studentId());
            studentNotificationPublisherService.dispatchLocally(event.studentId(), event.eventName(), event.data());
        }
    }
}
