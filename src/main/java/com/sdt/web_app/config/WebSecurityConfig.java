package com.sdt.web_app.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.boot.web.server.servlet.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.audiences:api://sdt-webapp}")
    private List<String> expectedAudiences;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8080,http://localhost:4200,http://192.168.254.120:4200,http://10.100.168.114:4200,http://136.158.184.122:4200}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "X-Refresh-Token", "x-refresh-token"));
        configuration.setExposedHeaders(List.of("Set-Cookie"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Value("${app.security.cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${app.security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.cookie.name:REFRESH_TOKEN}")
    private String cookieName;

    @Bean
    public CookieSameSiteSupplier applicationCookieSameSiteSupplier() {
        if ("None".equalsIgnoreCase(cookieSameSite) && cookieSecure) {
            return CookieSameSiteSupplier.ofNone().whenHasName(cookieName);
        } else if ("Strict".equalsIgnoreCase(cookieSameSite)) {
            return CookieSameSiteSupplier.ofStrict().whenHasName(cookieName);
        } else {
            return CookieSameSiteSupplier.ofLax().whenHasName(cookieName);
        }
    }

    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            @Qualifier("localJwtDecoder") JwtDecoder jwtDecoder,
            JwtRoleConverter jwtRoleConverter,
            RateLimitingFilter rateLimitingFilter) throws Exception {
        return http
                .securityMatcher("/api/**", "/ws/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(rateLimitingFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; script-src 'self'; frame-ancestors 'none'; object-src 'none';")
                        )
                        .permissionsPolicyHeader(pp -> pp
                                .policy("camera=(self), geolocation=(self), microphone=(), payment=()")
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**", "/api/v1/public/**", "/api/v1/finance/gateways/**").permitAll()
                        .requestMatchers("/api/admin/**", "/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "GUIDANCE")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> {
                    org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver resolver =
                            new org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver();
                    resolver.setAllowUriQueryParameter(true);
                    oauth2.bearerTokenResolver(resolver)
                            .jwt(jwt -> jwt
                                    .decoder(jwtDecoder)
                                    .jwtAuthenticationConverter(jwtRoleConverter)
                            );
                })
                .build();
    }

    @Bean(name = "resourceServerJwtDecoder")
    @Primary
    public JwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties, JwtDenylistValidator denylistValidator) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(properties.getJwt().getJwkSetUri())
                .build();
        OAuth2TokenValidator<Jwt> defaultWithIssuer = JwtValidators.createDefaultWithIssuer(properties.getJwt().getIssuerUri());
        OAuth2TokenValidator<Jwt> clockSkewValidator = new JwtTimestampValidator(Duration.ofSeconds(20));
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD,
                audList -> audList != null && audList.stream().anyMatch(expectedAudiences::contains)
        );
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                defaultWithIssuer,
                clockSkewValidator,
                audienceValidator,
                denylistValidator
        ));
        return decoder;
    }
}