package com.sdt.web_app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class AsyncSecurityConfigTest extends BaseIntegrationTest {
    @Autowired
    @Qualifier("applicationTaskExecutor")
    private Executor executor;

    @Test
    @WithMockUser(username = "async-user", roles = {"USER"})
    void shouldPropagateSecurityContextAndExecuteTask() throws Exception {
        CompletableFuture<String> future = new CompletableFuture<>();

        executor.execute(() -> {
            SecurityContext context = SecurityContextHolder.getContext();
            Authentication auth = context.getAuthentication();
            if (auth != null) {
                future.complete(auth.getName());
            } else {
                future.completeExceptionally(new IllegalStateException("No SecurityContext found"));
            }
        });

        String authenticatedUser = future.get(5, TimeUnit.SECONDS);
        assertEquals("async-user", authenticatedUser);
    }
}
