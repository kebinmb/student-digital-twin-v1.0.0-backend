---
title: Actuator Network Segregation and Dedicated Security Chains
tags:
  - spring-boot-actuator
  - devops
  - network-security
  - mTLS
---
### Security Risk & Mechanism

Actuator endpoints (`/env`, `/heapdump`, `/threaddump`, `/beans`) expose application internals and environment variables. If exposed on the same port and filter chain as public web endpoints, misconfigurations can lead to information disclosure.

### Configuration & Code Example

**`application.yml`**
```yaml
management:
	server:
		port:8081 #Bound to private/internal interface
	endpoints:
		web:
			exposures:
				include: "health,info,metrics,prometheus"
	endpoint:
		health:
		show-detailed: when_authorized

```

```java
package com.example.security.actuator;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ActuatorSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
                .requestMatchers(EndpointRequest.to("prometheus", "metrics")).hasRole("MONITORING")
                .anyRequest().hasRole("OPS_ADMIN")
            )
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}

```