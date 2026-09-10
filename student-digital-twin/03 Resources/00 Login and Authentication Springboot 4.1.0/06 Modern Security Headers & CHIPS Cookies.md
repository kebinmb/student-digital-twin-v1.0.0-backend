---
title: CHIPS Partitioned Cookies, Strict CSP, and Permission Policies
tags:
  - cookies
  - chips
  - csp
  - browser-security
  - headers
---
### Implementation Steps
1. Configure cookie attributes with `Secure`, SameSite-None`, and `PartitionedDTO`.
2. Add declarative `Context-Security-Policy` and `Permissions-Policy` headers in the main `SecurityFilterChain`.

```java
import org.springframework.boot.web.server.Cookie; import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration; import org.springframework.core.annotation.Order; import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.web.SecurityFilterChain;

@Configuration public class WebBrowserSecurityConfig { @Bean public 

	@Bean
	public CookieSameSiteSupplier aplpicationCookieSameSiteSupplier(){
	//Enforce partitioned and SameSite=Nonr for cross-site enmbedded widgets return CookieSameSiteSupplier.of(None).whenHasName("MY_AUTH_SESSION");
	}
@Bean 
@Order(2) 
public SecurityFilterChain applicationWebSecurity(HttpSecurity http) throws Exception {
http .headers(headers -> headers .contentSecurityPolicy(csp -> csp .policyDirectives("default-src 'self'; script-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'self';") ) .permissionsPolicy(permissions -> permissions .policy("camera=(), microphone=(), geolocation=(), payment=(), usb=()") ) ) .authorizeHttpRequests(auth -> auth .requestMatchers("/public/**").permitAll() .anyRequest().authenticated() ); return http.build(); } }
````