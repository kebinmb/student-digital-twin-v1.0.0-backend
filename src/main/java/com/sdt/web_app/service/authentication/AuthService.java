package com.sdt.web_app.service.authentication;

import com.sdt.web_app.dto.authentication.AuthDtos;
import com.sdt.web_app.entities.authentication.RefreshToken;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.exceptions.InvalidTokenException;
import com.sdt.web_app.exceptions.UserAlreadyExistsException;
import com.sdt.web_app.repositories.authentication.RefreshTokenRepository;
import com.sdt.web_app.repositories.authentication.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {
    private static final String DUMMY_HASH = "$argon2id$v=19$m=16384,t=2,p=1$ZHVtbXlzYWx0MTIzNA$dummyhashfordummyuserverification123456789";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.security.refresh-token.expiration-days:7}")
    private int refreshTokenDurationDays;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public void register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Username is already in use");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email is already in use");
        }
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .enabled(true)
                .roles(Set.of(Roles.STUDENT))
                .build();
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new UserAlreadyExistsException("Username or email already exists");
        }
    }

    @Transactional
    public AuthDtos.AuthResult authenticate(AuthDtos.LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByUsername(loginRequest.usernameOrEmail())
                .or(() -> userRepository.findByEmail(loginRequest.usernameOrEmail()))
                .filter(User::isEnabled);

        String hashToVerify = userOpt.map(User::getPasswordHash).orElse(DUMMY_HASH);
        boolean passwordMatches = passwordEncoder.matches(loginRequest.password(), hashToVerify);

        if (userOpt.isEmpty() || !passwordMatches) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userOpt.get();
        String accessToken = tokenService.generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user);
        return new AuthDtos.AuthResult(accessToken, refreshToken.getToken(), 900);
    }

    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthDtos.AuthResult rotateRefreshToken(String incomingToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(incomingToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        User user = storedToken.getUser();

        if (!storedToken.isValid()) {
            // Delete all sessions for the user upon detecting reuse
            refreshTokenRepository.deleteAllByUserId(user.getId());
            throw new InvalidTokenException("Revoked or expired token used. All active sessions invalidated.");
        }

        int rowsUpdated = refreshTokenRepository.revokeIfActive(incomingToken);
        if (rowsUpdated == 0) {
            refreshTokenRepository.deleteAllByUserId(user.getId());
            throw new InvalidTokenException("Concurrent token rotation detected. All active sessions invalidated.");
        }

        String newAccessToken = tokenService.generateAccessToken(user);
        RefreshToken newRefreshToken = createRefreshToken(user);
        return new AuthDtos.AuthResult(newAccessToken, newRefreshToken.getToken(), 900);
    }

    @Transactional
    public void logout(String incomingToken) {
        refreshTokenRepository.findByToken(incomingToken).ifPresent(RefreshToken::revoke);
    }

    private RefreshToken createRefreshToken(User user) {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes); // Populates the random array
        String tokenString = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenString)
                .expiryDate(Instant.now().plus(refreshTokenDurationDays, ChronoUnit.DAYS))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }
}