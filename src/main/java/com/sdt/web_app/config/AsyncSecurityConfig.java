package com.sdt.web_app.config;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ContextSnapshotFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncSecurityConfig {
    @PostConstruct
    public void initContextRegistry() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                "SECURITY_CONTEXT",
                SecurityContextHolder::getContext,
                SecurityContextHolder::setContext,
                SecurityContextHolder::clearContext
        );
    }

    @Bean
    public TaskDecorator securityTaskDecorator() {
        ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder().build();
        return runnable -> snapshotFactory.captureAll().wrap(runnable);
    }

    @Bean(name = "applicationTaskExecutor")
    public Executor applicationTaskExecutor(TaskDecorator securityTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setVirtualThreads(true);
        executor.setTaskDecorator(securityTaskDecorator);
        executor.initialize();
        return executor;
    }
}
