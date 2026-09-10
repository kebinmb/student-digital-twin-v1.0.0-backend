---
title: Spring gRPC Security Interceptors and Context Binding
tags:
  - spring-boot
  - grpc
  - microservices
  - rpc-security
---
## Security Risk & Mechanism
gRPC traffic uses HTTP/2 transport frames with custom serialization and does not route through standard Servlet `Filter` chains or WebFlux filters. Any endpoint declared as a gRPC service will bypass `@EnableWebSecurity` filter chains entirely unless secured by dedicated gRPC `ServerInterceptor` beans.

### Implementation Steps
1. Create a `ServiceInterceptor` to intercept inbound RPC invocations.
2. Read the `Authorization` metadata key from gRPC metadata headers.
3. Validate credentials and build a Spring `Authentication` object.
4. Bind authentication to `io.grpc.Context` for thread-safe access within gRPC service methods.

### Code Example

```java

package com.example.security.grpc;

import io.grpc.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class GrpcSecurityInterceptor implements ServerInterceptor{
	public static final Context.Key<Authentication> AUTH_KEY = Context.key("GRPC_AUTH_KEY");
	private static final Metadata.Key<String> AUTH_METADATA_KEY = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
	
	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
		ServerCall<ReqT, RespT> call,
		Metadata headers,
		ServerCallHandler<ReqT, RespT> next
	)
	{
		String authHeader = headers.get(AUTH_METADATA_KEY);
		
		if(authHeader == null || !authHeader.startsWith("Bearer ")){
			call.close(
				Status.UNAUTHENTICATED.withDescription("Missing or invalid Bearer token"),
			);
			return new ServerCall.Listener<>(){};
		}
		String token = authHeader.substring(7);
		try{
			Authentication auth = validateTokenAndBuildPrincipal(token);
			Context contextWithAuth = Context.current().withValue(AUTH_KEY, auth);
			return Contexts.interceptCall(contextWithAuth, call, headers, next);
		}catch(Exception ex){
			call.close(
				Status.UNAUTHENTICATED.withDescription("Token validation failed: " + ex.getMessage()),
				headers
			);
			return new ServerCall.Listener<>(){};
		}
	}
	
	private Authentication validateTokenAndBuildPrincipal(String token){
		return new UsernamePasswordAuthenticationToken(
			"service-user",
			null,
			List.of(new SimpleGrantedAuthority("ROLE_RPC_CLIENT"))
		);
	}
}
```