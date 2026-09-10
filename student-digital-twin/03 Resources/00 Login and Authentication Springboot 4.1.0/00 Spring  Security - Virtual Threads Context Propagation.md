---
title: Spring Security - Virtual Threads Context Propagation
tags:
  - spring-boot
  - spring-security
  - java-21
  - virtual-threads
  - concurrency
---
## Security Risk & Mechanism

In the traditional Spring applications, `SecurityContextHolder.MODE_INHERITABLETHREADLOCAL` is often used to pass security contexts to child threads. On `Java 21+` Virtual Threads, virtual threads are mounted onto a small pool of carrier threads.

## Using standard `InheritableThreadLocal` leads to:
1. **Context Leaks / Bleeding:** Carrier threads retain stale user principals across recycled virtual thread execution.
2. **Memory Leaks:** Retained thread locals prevent garbage collection of transient session objects.

## To propagate context cleanly across `@Async`, custom `TaskExecutor` instances, and reactive stream boundaries, configure **Micrometer Context Propagation** with a custom `TaskDecorator`.

## Implementation Steps
1. Add `io.micrometer:context-propagation` to dependencies.
2. Register the Spring Security `SecurityContext` accessor with the `ContextRegistry`.
3. Configure an `AsyncConfigurer` or `TaskExecutor` bean using `ContextSnapshotFactory`.

## Configuration & Code Example

```java

package com.example.security.config;

import io.micrometer.context.ContextRegisry;
import io.micrometer.context.ContextSnapshotFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.securityContextHolder;
import org.springframework.security.core.context.SecurityContext;

import jakarta.annotation.PostContruct;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsycnSecurityConfig{
	@PostConstruct
	public void setupContextPropagation(){
		ContextRegistry.getInstanct().registerThreadLocalAccessor(
		"SECURITY_CONTEXT",
		SecurityContextHolder::getContext,
		SecurityContextHolder::setCOntext,
		SecurityContextHolder::clearContext
		);
	}
	
	@Bean
	public TaskDecorator securityTaskDecorator(){
		ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder().build();
return snapshotFactory::captureAll;		
	}
	
	@Bean(name = "virtualThreadExecutor")
	public Executor virtualThreadExecutor(TaskDecorator securityTaskDecorator){
	ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	executor.setVirtualThreads(true);
	executor.setTaskDecorator(securityTaskDecorator);
	executor.initialize();
	return executor;
	}
}
```