package com.sdt.web_app.config;

import tools.jackson.databind.ObjectMapper;
import com.sdt.web_app.event.lms.StudentStreamingEvent;
import com.sdt.web_app.event.scheduling.SectionStreamingEvent;
import com.sdt.web_app.service.events.ClusterEventBridge;
import com.sdt.web_app.service.lms.StudentNotificationPublisherService;
import com.sdt.web_app.service.scheduling.SectionEventPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
@Profile("cluster-redis")
@ConditionalOnProperty(name = "sdt.cluster.redis.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class RedisClusterStreamingConfig {

    public static final String SECTION_STREAMING_CHANNEL = "sdt:events:sections";
    public static final String STUDENT_STREAMING_CHANNEL = "sdt:events:students";

    private final ObjectMapper objectMapper;
    private final ClusterEventBridge clusterEventBridge;
    private final SectionEventPublisherService sectionEventPublisherService;
    private final StudentNotificationPublisherService studentNotificationPublisherService;

    @Bean
    public ChannelTopic sectionStreamingTopic() {
        return new ChannelTopic(SECTION_STREAMING_CHANNEL);
    }

    @Bean
    public ChannelTopic studentStreamingTopic() {
        return new ChannelTopic(STUDENT_STREAMING_CHANNEL);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener((message, pattern) -> {
            try {
                String json = new String(message.getBody(), StandardCharsets.UTF_8);
                SectionStreamingEvent event = objectMapper.readValue(json, SectionStreamingEvent.class);
                if (event != null && !clusterEventBridge.getNodeId().equals(event.originNodeId())) {
                    log.debug("Redis PubSub -> Remote section event received on node {}: {} (term={})",
                            clusterEventBridge.getNodeId(), event.eventName(), event.termId());
                    sectionEventPublisherService.dispatchLocally(event.termId(), event.eventName(), event.data());
                }
            } catch (Exception e) {
                log.error("Failed to deserialize and dispatch section streaming event from Redis PubSub", e);
            }
        }, sectionStreamingTopic());

        container.addMessageListener((message, pattern) -> {
            try {
                String json = new String(message.getBody(), StandardCharsets.UTF_8);
                StudentStreamingEvent event = objectMapper.readValue(json, StudentStreamingEvent.class);
                if (event != null && !clusterEventBridge.getNodeId().equals(event.originNodeId())) {
                    log.debug("Redis PubSub -> Remote student event received on node {}: {} (student={})",
                            clusterEventBridge.getNodeId(), event.eventName(), event.studentId());
                    studentNotificationPublisherService.dispatchLocally(event.studentId(), event.eventName(), event.data());
                }
            } catch (Exception e) {
                log.error("Failed to deserialize and dispatch student streaming event from Redis PubSub", e);
            }
        }, studentStreamingTopic());

        log.info("Initialized RedisClusterStreamingConfig: listening to channels '{}' & '{}'",
                SECTION_STREAMING_CHANNEL, STUDENT_STREAMING_CHANNEL);
        return container;
    }
}
