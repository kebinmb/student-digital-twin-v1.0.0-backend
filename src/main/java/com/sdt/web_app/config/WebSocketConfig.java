package com.sdt.web_app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8080,http://localhost:4200,http://192.168.254.120:4200,http://10.100.168.114:4200,http://136.158.184.122:4200}")
    private List<String> allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // In-memory broker — broadcasts to /topic/** subscribers
        config.enableSimpleBroker("/topic");
        // Prefix for client -> server messages
        config.setApplicationDestinationPrefixes("/app");
        // Prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = allowedOrigins != null && !allowedOrigins.isEmpty()
                ? allowedOrigins.toArray(new String[0])
                : new String[]{"http://localhost:4200"};

        // Plain WebSocket endpoint for native STOMP clients (primary)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins);

        // SockJS fallback endpoint
        registry.addEndpoint("/ws-sockjs")
                .setAllowedOriginPatterns(origins)
                .withSockJS();
    }
}
