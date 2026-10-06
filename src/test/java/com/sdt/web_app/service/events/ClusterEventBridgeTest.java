package com.sdt.web_app.service.events;

import com.sdt.web_app.event.lms.StudentStreamingEvent;
import com.sdt.web_app.event.scheduling.SectionStreamingEvent;
import com.sdt.web_app.service.lms.StudentNotificationPublisherService;
import com.sdt.web_app.service.scheduling.SectionEventPublisherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClusterEventBridgeTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SectionEventPublisherService sectionPublisher;

    @Mock
    private StudentNotificationPublisherService studentPublisher;

    private ClusterEventBridge bridge;

    @BeforeEach
    void setUp() {
        bridge = new ClusterEventBridge(eventPublisher, sectionPublisher, studentPublisher);
    }

    @Test
    @DisplayName("broadcastSectionEvent publishes SectionStreamingEvent with current nodeId")
    void broadcastSectionEvent_PublishesEvent() {
        Map<String, Object> data = Map.of("capacity", 40);
        bridge.broadcastSectionEvent(5L, "enlistment-updated", data);

        ArgumentCaptor<SectionStreamingEvent> captor = ArgumentCaptor.forClass(SectionStreamingEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        SectionStreamingEvent published = captor.getValue();
        assertThat(published.termId()).isEqualTo(5L);
        assertThat(published.eventName()).isEqualTo("enlistment-updated");
        assertThat(published.data()).isEqualTo(data);
        assertThat(published.originNodeId()).isEqualTo(bridge.getNodeId());
    }

    @Test
    @DisplayName("broadcastStudentEvent publishes StudentStreamingEvent with current nodeId")
    void broadcastStudentEvent_PublishesEvent() {
        Map<String, Object> data = Map.of("grade", 1.25);
        bridge.broadcastStudentEvent(99L, "grade-released", data);

        ArgumentCaptor<StudentStreamingEvent> captor = ArgumentCaptor.forClass(StudentStreamingEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        StudentStreamingEvent published = captor.getValue();
        assertThat(published.studentId()).isEqualTo(99L);
        assertThat(published.eventName()).isEqualTo("grade-released");
        assertThat(published.data()).isEqualTo(data);
        assertThat(published.originNodeId()).isEqualTo(bridge.getNodeId());
    }

    @Test
    @DisplayName("onSectionStreamingEvent from remote node triggers local dispatch")
    void onSectionStreamingEvent_RemoteNode_DispatchesLocally() {
        SectionStreamingEvent remoteEvent = new SectionStreamingEvent(10L, "enlistment-updated", "data", "remote-node-123");
        bridge.onSectionStreamingEvent(remoteEvent);

        verify(sectionPublisher).dispatchLocally(10L, "enlistment-updated", "data");
    }

    @Test
    @DisplayName("onSectionStreamingEvent from same node is ignored locally")
    void onSectionStreamingEvent_LocalNode_Ignored() {
        SectionStreamingEvent localEvent = new SectionStreamingEvent(10L, "enlistment-updated", "data", bridge.getNodeId());
        bridge.onSectionStreamingEvent(localEvent);

        verifyNoInteractions(sectionPublisher);
    }

    @Test
    @DisplayName("onStudentStreamingEvent from remote node triggers local dispatch")
    void onStudentStreamingEvent_RemoteNode_DispatchesLocally() {
        StudentStreamingEvent remoteEvent = new StudentStreamingEvent(25L, "grade-released", "data", "remote-node-xyz");
        bridge.onStudentStreamingEvent(remoteEvent);

        verify(studentPublisher).dispatchLocally(25L, "grade-released", "data");
    }

    @Test
    @DisplayName("onStudentStreamingEvent from same node is ignored locally")
    void onStudentStreamingEvent_LocalNode_Ignored() {
        StudentStreamingEvent localEvent = new StudentStreamingEvent(25L, "grade-released", "data", bridge.getNodeId());
        bridge.onStudentStreamingEvent(localEvent);

        verifyNoInteractions(studentPublisher);
    }

    @Test
    @DisplayName("broadcastSectionEvent publishes to Redis channel when redisTemplate is present")
    void broadcastSectionEvent_WithRedis_PublishesToChannel() {
        org.springframework.data.redis.core.StringRedisTemplate redisTemplate = mock(org.springframework.data.redis.core.StringRedisTemplate.class);
        tools.jackson.databind.ObjectMapper mapper = mock(tools.jackson.databind.ObjectMapper.class);
        when(mapper.writeValueAsString(any())).thenReturn("{\"eventName\":\"section-updated\"}");

        bridge.setRedisTemplate(redisTemplate);
        bridge.setObjectMapper(mapper);

        Map<String, Object> data = Map.of("capacity", 35);
        bridge.broadcastSectionEvent(8L, "section-updated", data);

        verify(redisTemplate).convertAndSend(eq(com.sdt.web_app.config.RedisClusterStreamingConfig.SECTION_STREAMING_CHANNEL), contains("section-updated"));
    }

    @Test
    @DisplayName("broadcastStudentEvent publishes to Redis channel when redisTemplate is present")
    void broadcastStudentEvent_WithRedis_PublishesToChannel() {
        org.springframework.data.redis.core.StringRedisTemplate redisTemplate = mock(org.springframework.data.redis.core.StringRedisTemplate.class);
        tools.jackson.databind.ObjectMapper mapper = mock(tools.jackson.databind.ObjectMapper.class);
        when(mapper.writeValueAsString(any())).thenReturn("{\"eventName\":\"standing-updated\"}");

        bridge.setRedisTemplate(redisTemplate);
        bridge.setObjectMapper(mapper);

        Map<String, Object> data = Map.of("standing", "DEANS_LIST");
        bridge.broadcastStudentEvent(42L, "standing-updated", data);

        verify(redisTemplate).convertAndSend(eq(com.sdt.web_app.config.RedisClusterStreamingConfig.STUDENT_STREAMING_CHANNEL), contains("standing-updated"));
    }
}
