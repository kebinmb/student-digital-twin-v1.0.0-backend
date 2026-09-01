package com.sdt.web_app.config;

import io.grpc.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class GrpcAuthInterceptor implements ServerInterceptor {
    public static final Context.Key<AbstractAuthenticationToken> AUTH_CONTEXT_KEY = Context.key("GRPC_AUTH_KEY");

    private static final Metadata.Key<String> AUTH_HEADER = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
    private final JwtDecoder jwtDecoder;
    private final JwtRoleConverter jwtRoleConverter;

    public GrpcAuthInterceptor(JwtDecoder jwtDecoder, JwtRoleConverter jwtRoleConverter) {
        this.jwtDecoder = jwtDecoder;
        this.jwtRoleConverter = jwtRoleConverter;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        String rawToken = headers.get(AUTH_HEADER);

        if (rawToken == null || !rawToken.startsWith("Bearer ")) {
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or malformed Authorization header"), headers);
            return new ServerCall.Listener<>() {
            };
        }
        try {
            String tokenValue = rawToken.substring(7);
            Jwt jwt = jwtDecoder.decode(tokenValue);
            AbstractAuthenticationToken auth = jwtRoleConverter.convert(jwt);
            Context context = Context.current().withValue(AUTH_CONTEXT_KEY, auth);
            return Contexts.interceptCall(context, call, headers, next);
        } catch (Exception ex) {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid JWT: " + ex.getMessage()), headers);
            return new ServerCall.Listener<>() {
            };
        }
    }

}
