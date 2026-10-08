package com.sdt.web_app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtDecoder jwtDecoder;
    private final JwtRoleConverter jwtRoleConverter;

    public WebSocketSecurityConfig(
            @Qualifier("localJwtDecoder") JwtDecoder jwtDecoder,
            JwtRoleConverter jwtRoleConverter) {
        this.jwtDecoder = jwtDecoder;
        this.jwtRoleConverter = jwtRoleConverter;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor
                        .getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    String token = null;

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        token = authHeader.substring(7).trim();
                    } else if (accessor.getFirstNativeHeader("access_token") != null) {
                        token = accessor.getFirstNativeHeader("access_token").trim();
                    }

                    if (token != null && !token.isBlank()) {
                        try {
                            Jwt jwt = jwtDecoder.decode(token);
                            AbstractAuthenticationToken authentication = jwtRoleConverter.convert(jwt);
                            accessor.setUser(authentication);
                            log.debug("[WebSocket] STOMP CONNECT authenticated for principal: {}", authentication.getName());
                        } catch (Exception e) {
                            log.warn("[WebSocket] STOMP CONNECT authentication rejected: {}", e.getMessage());
                            throw new AccessDeniedException("Invalid STOMP authentication token: " + e.getMessage());
                        }
                    }
                }
                return message;
            }
        });
    }
}
