package com.sdt.web_app.service.scheduling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter.DataWithMediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SectionEventPublisherServiceTest {

    private SectionEventPublisherService service;

    @BeforeEach
    void setUp() {
        service = new SectionEventPublisherService();
    }

    @Test
    @DisplayName("subscribeToTermSectionEvents with termId registers emitter and dispatches INIT event")
    void subscribe_WithTermId_RegistersEmitterAndSendsInit() {
        Long termId = 15L;
        SseEmitter emitter = service.subscribeToTermSectionEvents(termId);

        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(30 * 60 * 1000L);

        // Verify registration in internal map
        @SuppressWarnings("unchecked")
        Map<Long, List<SseEmitter>> map = (Map<Long, List<SseEmitter>>) ReflectionTestUtils.getField(service, "termEmitters");
        assertThat(map).isNotNull();
        assertThat(map.get(termId)).contains(emitter);

        // Verify INIT event queued in earlySendAttempts
        assertThat(getEarlySendCount(emitter)).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("subscribeToTermSectionEvents with null or zero termId defaults to global key 0L")
    void subscribe_WithNullOrZero_DefaultsToZeroKey() {
        SseEmitter nullEmitter = service.subscribeToTermSectionEvents(null);
        SseEmitter zeroEmitter = service.subscribeToTermSectionEvents(0L);

        @SuppressWarnings("unchecked")
        Map<Long, List<SseEmitter>> map = (Map<Long, List<SseEmitter>>) ReflectionTestUtils.getField(service, "termEmitters");
        assertThat(map).isNotNull();
        assertThat(map.get(0L)).contains(nullEmitter, zeroEmitter);
    }

    @Test
    @DisplayName("publishSectionEnlistmentEvent dispatches events to term-specific and global listeners")
    void publishSectionEnlistmentEvent_DispatchesToTargetTermAndGlobal() {
        Long termId = 42L;
        SseEmitter termListener = service.subscribeToTermSectionEvents(termId);
        SseEmitter globalListener = service.subscribeToTermSectionEvents(0L);
        SseEmitter unrelatedListener = service.subscribeToTermSectionEvents(99L);

        int termInitialEvents = getEarlySendCount(termListener);
        int globalInitialEvents = getEarlySendCount(globalListener);
        int unrelatedInitialEvents = getEarlySendCount(unrelatedListener);

        service.publishSectionEnlistmentEvent(termId, 101L, "CS-301", 35, 40);

        assertThat(getEarlySendCount(termListener)).isGreaterThan(termInitialEvents);
        assertThat(getEarlySendCount(globalListener)).isGreaterThan(globalInitialEvents);
        assertThat(getEarlySendCount(unrelatedListener)).isEqualTo(unrelatedInitialEvents);
    }

    @Test
    @DisplayName("publishGradeStatusEvent dispatches grade status update payload properly")
    void publishGradeStatusEvent_DispatchesToListeners() {
        Long termId = 7L;
        SseEmitter termListener = service.subscribeToTermSectionEvents(termId);

        int initialEvents = getEarlySendCount(termListener);
        service.publishGradeStatusEvent(termId, 202L, "IT-402", "VERIFIED");

        assertThat(getEarlySendCount(termListener)).isGreaterThan(initialEvents);
    }

    @Test
    @DisplayName("Emitter lifecycle completion and timeout callbacks clean up termEmitters map")
    void emitterLifecycleCallbacks_RemoveEmitterFromMap() {
        Long termId = 88L;
        SseEmitter emitter = service.subscribeToTermSectionEvents(termId);

        @SuppressWarnings("unchecked")
        Map<Long, List<SseEmitter>> map = (Map<Long, List<SseEmitter>>) ReflectionTestUtils.getField(service, "termEmitters");
        assertThat(map.get(termId)).contains(emitter);

        // Trigger completion callback
        Runnable completionCallback = (Runnable) ReflectionTestUtils.getField(emitter, "completionCallback");
        assertThat(completionCallback).isNotNull();
        completionCallback.run();

        // Should be pruned
        assertThat(map.get(termId)).isNull();

        // Now test timeout callback
        SseEmitter timeoutEmitter = service.subscribeToTermSectionEvents(termId);
        assertThat(map.get(termId)).contains(timeoutEmitter);

        Runnable timeoutCallback = (Runnable) ReflectionTestUtils.getField(timeoutEmitter, "timeoutCallback");
        assertThat(timeoutCallback).isNotNull();
        timeoutCallback.run();

        assertThat(map.get(termId)).isNull();
    }

    @Test
    @DisplayName("Publishing with no listeners executes cleanly without error")
    void publish_WithNoListeners_DoesNotThrow() {
        assertDoesNotThrow(() -> {
            service.publishSectionEnlistmentEvent(999L, 1L, "EMPTY", 0, 50);
            service.publishGradeStatusEvent(999L, 1L, "EMPTY", "DRAFT");
        });
    }

    private int getEarlySendCount(SseEmitter emitter) {
        @SuppressWarnings("unchecked")
        Set<DataWithMediaType> earlySendAttempts = (Set<DataWithMediaType>) ReflectionTestUtils.getField(emitter, "earlySendAttempts");
        return earlySendAttempts != null ? earlySendAttempts.size() : 0;
    }
}
