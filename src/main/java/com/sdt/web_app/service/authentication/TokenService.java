package com.sdt.web_app.service.authentication;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.repositories.institution.ProgramRepository;
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

    private final ProgramRepository programRepository;
    private final com.sdt.web_app.repositories.enrollment.StudentProfileRepository studentProfileRepository;

    public TokenService(JwtEncoder jwtEncoder,
                        ProgramRepository programRepository,
                        com.sdt.web_app.repositories.enrollment.StudentProfileRepository studentProfileRepository) {
        this.jwtEncoder = jwtEncoder;
        this.programRepository = programRepository;
        this.studentProfileRepository = studentProfileRepository;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        List<String> roleNames = user.getRoles().stream()
                .map(Enum::name) // Converts Roles.ADMIN -> "ADMIN"
                .toList();
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .id(java.util.UUID.randomUUID().toString())
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
        Long programId = user.getProgram() != null ? user.getProgram().getId() : null;
        if (programId == null && programRepository != null && user.getId() != null) {
            programId = programRepository.findFirstByChairpersonUserId(user.getId())
                    .map(com.sdt.web_app.entities.institution.Program::getId)
                    .orElse(null);
        }
        if (programId != null) {
            claimsBuilder.claim("program_id", programId);
        }

        if (studentProfileRepository != null && user.getId() != null) {
            studentProfileRepository.findByUserId(user.getId()).ifPresent(sp -> {
                claimsBuilder.claim("student_profile_id", sp.getId());
                claimsBuilder.claim("student_number", sp.getStudentNumber());
            });
        }

        return this.jwtEncoder.encode(JwtEncoderParameters.from(claimsBuilder.build())).getTokenValue();
    }
}
