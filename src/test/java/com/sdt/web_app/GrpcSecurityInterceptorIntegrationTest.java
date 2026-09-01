package com.sdt.web_app;

import com.sdt.web_app.config.GrpcAuthInterceptor;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.service.authentication.TokenService;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.ServerCalls;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class GrpcSecurityInterceptorIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private GrpcAuthInterceptor grpcAuthInterceptor;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TokenService tokenService;

    private ManagedChannel inProcessChannel;
    private Server inProcessServer;
    private final String serverName = InProcessServerBuilder.generateName();

    static class DummySecureService implements BindableService {
        @Override
        public ServerServiceDefinition bindService() {
            return ServerServiceDefinition.builder("DummySecureService")
                    .addMethod(
                            MethodDescriptor.<String, String>newBuilder()
                                    .setType(MethodDescriptor.MethodType.UNARY)
                                    .setFullMethodName("DummySecureService/GetIdentity")
                                    .setRequestMarshaller(new StringMarshaller())
                                    .setResponseMarshaller(new StringMarshaller())
                                    .build(),
                            ServerCalls.asyncUnaryCall((req, responseObserver) -> {
                                // Retrieve Authentication from the gRPC Context bound by GrpcAuthInterceptor
                                AbstractAuthenticationToken auth = GrpcAuthInterceptor.AUTH_CONTEXT_KEY.get();
                                String user = (auth != null && auth.isAuthenticated()) ? auth.getName() : "ANONYMOUS";

                                responseObserver.onNext(user);
                                responseObserver.onCompleted();
                            })
                    ).build();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();

        inProcessServer = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(ServerInterceptors.intercept(new DummySecureService(), grpcAuthInterceptor))
                .build()
                .start();

        inProcessChannel = InProcessChannelBuilder.forName(serverName)
                .directExecutor()
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (inProcessChannel != null) {
            inProcessChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
        if (inProcessServer != null) {
            inProcessServer.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("gRPC Interceptor should reject unauthenticated RPC calls with UNAUTHENTICATED status")
    void shouldRejectCallWithoutMetadata() {
        MethodDescriptor<String, String> method = MethodDescriptor.<String, String>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("DummySecureService/GetIdentity")
                .setRequestMarshaller(new StringMarshaller())
                .setResponseMarshaller(new StringMarshaller())
                .build();

        assertThatThrownBy(() -> ClientCalls.blockingUnaryCall(inProcessChannel, method, CallOptions.DEFAULT, "ping"))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(ex -> assertThat(((StatusRuntimeException) ex).getStatus().getCode())
                        .isEqualTo(Status.Code.UNAUTHENTICATED));
    }

    @Test
    @DisplayName("gRPC Interceptor should accept valid Bearer token and bind security context")
    void shouldAuthenticateCallWithValidMetadata() {
        MethodDescriptor<String, String> method = MethodDescriptor.<String, String>newBuilder()
                .setType(MethodDescriptor.MethodType.UNARY)
                .setFullMethodName("DummySecureService/GetIdentity")
                .setRequestMarshaller(new StringMarshaller())
                .setResponseMarshaller(new StringMarshaller())
                .build();

        // 1. Create and persist user
        User testUser = User.builder()
                .username("grpc-test-user")
                .email("grpc-user@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .enabled(true)
                .roles(Set.of(Roles.STUDENT))
                .build();
        userRepository.save(testUser);

        // 2. Generate token
        String validSignedJwt = tokenService.generateAccessToken(testUser);

        // 3. Stub the local JwtDecoder mock with complete claims for JwtRoleConverter
        Jwt mockedJwt = Jwt.withTokenValue(validSignedJwt)
                .header("alg", "RS256")
                .header("typ", "JWT")
                .subject("grpc-test-user")
                .claim("user_name", "grpc-test-user")
                .claim("roles", List.of("ROLE_USER"))
                .claim("scope", "ROLE_USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(mockedJwt);

        // 4. Attach metadata and execute call
        Metadata headers = new Metadata();
        Metadata.Key<String> authHeader = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
        headers.put(authHeader, "Bearer " + validSignedJwt);

        Channel interceptedChannel = ClientInterceptors.intercept(
                inProcessChannel,
                new ClientInterceptor() {
                    @Override
                    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                            MethodDescriptor<ReqT, RespT> methodDescriptor,
                            CallOptions callOptions,
                            Channel next) {
                        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(methodDescriptor, callOptions)) {
                            @Override
                            public void start(Listener<RespT> responseListener, Metadata headersMap) {
                                headersMap.merge(headers);
                                super.start(responseListener, headersMap);
                            }
                        };
                    }
                }
        );

        String result = ClientCalls.blockingUnaryCall(interceptedChannel, method, CallOptions.DEFAULT, "ping");
        assertThat(result).isEqualTo("grpc-test-user");
    }

    private static class StringMarshaller implements MethodDescriptor.Marshaller<String> {
        @Override
        public InputStream stream(String value) {
            return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public String parse(InputStream stream) {
            try {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}