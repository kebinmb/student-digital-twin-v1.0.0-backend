package com.sdt.web_app.service.authentication;

import com.sdt.web_app.dto.authentication.AuthDtos;
import com.sdt.web_app.entities.authentication.RefreshToken;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.exceptions.InvalidTokenException;
import com.sdt.web_app.exceptions.UserAlreadyExistsException;
import com.sdt.web_app.repositories.authentication.RefreshTokenRepository;
import com.sdt.web_app.repositories.authentication.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AuthService {

    private static final String DUMMY_HASH = "$argon2id$v=19$m=16384,t=2,p=1$ZHVtbXlzYWx0MTIzNA$dummyhashfordummyuserverification123456789";
    private static final int DEFAULT_EXPIRATION_SECONDS = 900;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final SecureRandom secureRandom = new SecureRandom();

    // Bounded concurrency gate for CPU/Memory intensive Argon2 hashing (prevents OOM on virtual threads)
    private final Semaphore hashingGate = new Semaphore(Math.max(2, Runtime.getRuntime().availableProcessors() * 2), true);

    // Fine-grained non-pinning lock map for serializing concurrent refresh token rotation on identical tokens
    private final ConcurrentHashMap<String, ReentrantLock> tokenRotationLocks = new ConcurrentHashMap<>();

    @Value("${app.security.refresh-token.expiration-days:7}")
    private int refreshTokenDurationDays;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * User registration: Hashes password OUTSIDE the transaction boundary to prevent HikariCP
     * connection starvation during CPU-bound Argon2 hashing.
     */
    public void register(AuthDtos.RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("Username is already in use");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("Email is already in use");
        }

        String hashedPassword = computeHashWithGate(request.password());

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(hashedPassword)
                .enabled(true)
                .roles(Set.of(Roles.STUDENT))
                .build();

        persistUserSafely(user);
    }

    @Transactional
    protected void persistUserSafely(User user) {
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new UserAlreadyExistsException("Username or email already exists");
        }
    }

    /**
     * Authentication: Resolves user, computes password verification OUTSIDE of the database transaction,
     * then executes a short, atomic write transaction solely to persist the refresh token.
     */
    public AuthDtos.AuthResult authenticate(AuthDtos.LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByUsername(loginRequest.usernameOrEmail())
                .or(() -> userRepository.findByEmail(loginRequest.usernameOrEmail()))
                .filter(User::isEnabled);

        String hashToVerify = userOpt.map(User::getPasswordHash).orElse(DUMMY_HASH);
        boolean passwordMatches = verifyHashWithGate(loginRequest.password(), hashToVerify);

        if (userOpt.isEmpty() || !passwordMatches) {
            throw new BadCredentialsException("Invalid username or password");
        }

        User user = userOpt.get();
        String accessToken = tokenService.generateAccessToken(user);
        RefreshToken refreshToken = createAndPersistRefreshToken(user);

        return new AuthDtos.AuthResult(accessToken, refreshToken.getToken(), DEFAULT_EXPIRATION_SECONDS);
    }

    /**
     * Rotate Refresh Token: Uses ReentrantLock to serialize concurrent requests on the same incoming token,
     * ensuring zero carrier-thread pinning under Java 21 Loom virtual threads.
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthDtos.AuthResult rotateRefreshToken(String incomingToken) {
        if (incomingToken == null || incomingToken.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }

        ReentrantLock lock = tokenRotationLocks.computeIfAbsent(incomingToken, k -> new ReentrantLock());
        lock.lock();
        try {
            return executeTokenRotation(incomingToken);
        } finally {
            lock.unlock();
            tokenRotationLocks.remove(incomingToken, lock);
        }
    }

    @Transactional(noRollbackFor = InvalidTokenException.class)
    protected AuthDtos.AuthResult executeTokenRotation(String incomingToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(incomingToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        User user = storedToken.getUser();

        if (!storedToken.isValid()) {
            refreshTokenRepository.deleteAllByUserId(user.getId());
            throw new InvalidTokenException("Revoked or expired token used. All active sessions invalidated.");
        }

        int rowsUpdated = refreshTokenRepository.revokeIfActive(incomingToken);
        if (rowsUpdated == 0) {
            refreshTokenRepository.deleteAllByUserId(user.getId());
            throw new InvalidTokenException("Concurrent token rotation detected. All active sessions invalidated.");
        }

        String newAccessToken = tokenService.generateAccessToken(user);
        RefreshToken newRefreshToken = createAndPersistRefreshToken(user);
        return new AuthDtos.AuthResult(newAccessToken, newRefreshToken.getToken(), DEFAULT_EXPIRATION_SECONDS);
    }

    @Transactional
    public void logout(String incomingToken) {
        if (incomingToken != null && !incomingToken.isBlank()) {
            refreshTokenRepository.findByToken(incomingToken).ifPresent(RefreshToken::revoke);
        }
    }

    @Transactional
    protected RefreshToken createAndPersistRefreshToken(User user) {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        String tokenString = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenString)
                .expiryDate(Instant.now().plus(refreshTokenDurationDays, ChronoUnit.DAYS))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    private String computeHashWithGate(String rawPassword) {
        try {
            hashingGate.acquire();
            try {
                return passwordEncoder.encode(rawPassword);
            } finally {
                hashingGate.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Authentication thread interrupted during password hashing", e);
        }
    }

    private boolean verifyHashWithGate(String rawPassword, String hash) {
        try {
            hashingGate.acquire();
            try {
                return passwordEncoder.matches(rawPassword, hash);
            } finally {
                hashingGate.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Authentication thread interrupted during password verification", e);
        }
    }
}