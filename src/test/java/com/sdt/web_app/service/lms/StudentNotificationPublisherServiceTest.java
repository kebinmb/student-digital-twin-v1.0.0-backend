package com.sdt.web_app.service.lms;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class StudentNotificationPublisherServiceTest {

    private StudentNotificationPublisherService service;

    @BeforeEach
    void setUp() {
        service = new StudentNotificationPublisherService();
    }

    @Test
    @DisplayName("subscribeToStudentEvents with valid studentId registers emitter and dispatches INIT handshake")
    void subscribe_WithValidStudentId_RegistersEmitter() {
        Long studentId = 55L;
        SseEmitter emitter = service.subscribeToStudentEvents(studentId);

        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(30 * 60 * 1000L);

        @SuppressWarnings("unchecked")
        Map<Long, List<SseEmitter>> map = (Map<Long, List<SseEmitter>>) ReflectionTestUtils.getField(service, "studentEmitters");
        assertThat(map).isNotNull();
        assertThat(map.get(studentId)).contains(emitter);

        assertThat(getEarlySendCount(emitter)).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("subscribeToStudentEvents with null or non-positive studentId throws IllegalArgumentException")
    void subscribe_WithInvalidStudentId_ThrowsException() {
        assertThatThrownBy(() -> service.subscribeToStudentEvents(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.subscribeToStudentEvents(0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.subscribeToStudentEvents(-1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("publishGradeReleasedEvent broadcasts payload to subscribed student")
    void publishGradeReleasedEvent_BroadcastsToStudent() {
        Long studentId = 101L;
        SseEmitter emitter = service.subscribeToStudentEvents(studentId);
        int initialCount = getEarlySendCount(emitter);

        service.publishGradeReleasedEvent(studentId, 45L, "CS-302", "Operating Systems", 1.25, "SEALED");

        assertThat(getEarlySendCount(emitter)).isGreaterThan(initialCount);
    }

    @Test
    @DisplayName("publishClearanceUpdatedEvent broadcasts clearance updates to subscribed student")
    void publishClearanceUpdatedEvent_BroadcastsToStudent() {
        Long studentId = 102L;
        SseEmitter emitter = service.subscribeToStudentEvents(studentId);
        int initialCount = getEarlySendCount(emitter);

        service.publishClearanceUpdatedEvent(studentId, 3L, "DEAN", "APPROVED", "CLEARED", "All requirements verified");

        assertThat(getEarlySendCount(emitter)).isGreaterThan(initialCount);
    }

    @Test
    @DisplayName("publishStandingUpdatedEvent broadcasts academic standing update")
    void publishStandingUpdatedEvent_BroadcastsToStudent() {
        Long studentId = 103L;
        SseEmitter emitter = service.subscribeToStudentEvents(studentId);
        int initialCount = getEarlySendCount(emitter);

        service.publishStandingUpdatedEvent(studentId, 1.45, new BigDecimal("84.00"), "REGULAR");

        assertThat(getEarlySendCount(emitter)).isGreaterThan(initialCount);
    }

    @Test
    @DisplayName("Emitter lifecycle completion callback prunes emitter cleanly")
    void emitterCompletion_PrunesMap() {
        Long studentId = 999L;
        SseEmitter emitter = service.subscribeToStudentEvents(studentId);

        @SuppressWarnings("unchecked")
        Map<Long, List<SseEmitter>> map = (Map<Long, List<SseEmitter>>) ReflectionTestUtils.getField(service, "studentEmitters");
        assertThat(map.get(studentId)).contains(emitter);

        Runnable completionCallback = (Runnable) ReflectionTestUtils.getField(emitter, "completionCallback");
        assertThat(completionCallback).isNotNull();
        completionCallback.run();

        assertThat(map.get(studentId)).isNull();
    }

    @Test
    @DisplayName("Publishing to non-connected student executes safely without throwing")
    void publish_NoSubscribers_DoesNotThrow() {
        assertDoesNotThrow(() -> {
            service.publishGradeReleasedEvent(404L, 1L, "CODE", "TITLE", 1.0, "SEALED");
            service.publishClearanceUpdatedEvent(404L, 1L, "LIB", "APPROVED", "CLEARED", "");
            service.publishStandingUpdatedEvent(404L, 1.0, BigDecimal.TEN, "REGULAR");
        });
    }

    private int getEarlySendCount(SseEmitter emitter) {
        @SuppressWarnings("unchecked")
        Set<DataWithMediaType> earlySendAttempts = (Set<DataWithMediaType>) ReflectionTestUtils.getField(emitter, "earlySendAttempts");
        return earlySendAttempts != null ? earlySendAttempts.size() : 0;
    }
}
