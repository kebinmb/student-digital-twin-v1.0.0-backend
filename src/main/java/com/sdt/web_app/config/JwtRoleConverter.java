package com.sdt.web_app.config;

import lombok.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        String principalClaimName = jwt.getClaimAsString("preferred_username") != null
                ? jwt.getClaimAsString("preferred_username")
                : jwt.getSubject();
        return new JwtAuthenticationToken(jwt, authorities, principalClaimName);
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        if (jwt == null) {
            return Collections.emptyList();
        }
        if (jwt.hasClaim("roles")) {
            List<String> roles = jwt.getClaimAsStringList("roles");
            return roles.stream()
                    .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                    .collect(Collectors.toSet());
        }

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                    .collect(Collectors.toSet());
        }

        return Collections.emptySet();
    }
}
