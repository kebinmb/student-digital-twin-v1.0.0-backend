package com.sdt.web_app.service.authentication;

import com.sdt.web_app.entities.authentication.User;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TokenService {
    private final JwtEncoder jwtEncoder;

    @Value("${spring.application.name:sdt-web-app}")
    private String issuer;

    @Value("${spring.security.oauth2.resourceserver.jwt.audiences:api://sdt-webapp}")
    private List<String> audiences;

    public TokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        List<String> roleNames = user.getRoles().stream()
                .map(Enum::name) // Converts Roles.ADMIN -> "ADMIN"
                .toList();
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(15, ChronoUnit.MINUTES))
                .subject(user.getId().toString())
                .audience(audiences)
                .claim("preferred_username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("roles", roleNames);

        if (user.getCollege() != null) {
            claimsBuilder.claim("college_id", user.getCollege().getId());
        }
        if (user.getProgram() != null) {
            claimsBuilder.claim("program_id", user.getProgram().getId());
        }

        return this.jwtEncoder.encode(JwtEncoderParameters.from(claimsBuilder.build())).getTokenValue();
    }
}
