---
title: OAuth 2.1 Compliance, Mandatory PKCE, and Strict JWT Validation
tags:
  - spring-security
  - oauth2
  - jwt
  - pkce
  - authentication
---
## Security Risk & Mechanism
OAuth 2.1 tightens authorization safety:
1. **Mandatory PKCE:** Authorization Code Flow must use Proof Key for Code Exchange (`code_challenge_method=S256`) for public **and** confidential clients to eliminate authorization code interception attacks.
2. **Strict Audience (`aud`) Checks:** Prevents token reuse across separate backend microservices sharing an authentication provider.
3. **Bounded Clock Skew:** Standardizes clock drift limits to prevent extended replay attack windows.
## Implementation Steps
1. Configure Resource Server `JwtDecoder` with `OAuth2TokenValidator`.
2. Attach explicit `JwtTimestampValidator` with $\le 30\text{s}$ tolerance.
3. Attach `JwtClaimValidator` verifying the explicit application audient identifier.


### Code example
```java
package com.example.security.oauth2;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;
import java.util.List;

@Configuration
public class ResourceServerSecurityConfig{
	@Value("${app.security.jwt.expected-audience:api://order-service})
	private String expectedAudience;
	
	@Bean
	public SecurityFilterChain resourceServerFilterChain(HttpSecurity http) throw Exception{
	http
		.authorizeHttpRequests(auth -> auth
			.requestMatchers("/api/public/**).permitAll()
			.anyRequest().authenticated()
			)
			.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));
		return http.build();
	}
	
	@Bean
	public JwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties){
		NimbusJwtDecoder decoder = NimbusJwtDecoder
				.withJwkSetUri(properties.getJwt().getJwkSetUri())
				.build();
		OAuth2TokenValidator<Jwt> timestampValidator = 
				new JwtTimestampValidator(Duration.ofSeconds(30));
				
		OAuth2TokenValidator<Jwt> audienceValidator = 
				new JwtClaimValidator<List<String>>(
							JwtClaimNames.AUD,
							aud -> aud != null && aud.contains(expectedAudience)
						);
		OAuth2TokenValidator<Jwt> combinedValidator = 
				new DelegatingOAuth2TokenValidator<>(timestampValidator, audienceValidator);
		decoder.setJwtValidator(combinedValidator);
		return decoder;
	}

}
```