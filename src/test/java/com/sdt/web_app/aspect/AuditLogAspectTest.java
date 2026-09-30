package com.sdt.web_app.aspect;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.controller.analytics.AttendanceController;
import com.sdt.web_app.dto.analytics.AnalyticsDtos.AttendanceSessionResponse;
import com.sdt.web_app.dto.analytics.AnalyticsDtos.StartAttendanceSessionRequest;
import com.sdt.web_app.entities.audit.AuditLog;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.repositories.authentication.PasswordResetTokenRepository;
import com.sdt.web_app.repositories.authentication.RefreshTokenRepository;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.service.audit.AuditLogService;
import com.sdt.web_app.service.security.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AuditLogAspectTest {

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private SecurityUtils securityUtils;

    private ObjectMapper objectMapper;

    private AuditLogAspect aspect;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        aspect = new AuditLogAspect(
                auditLogService,
                userRepository,
                refreshTokenRepository,
                passwordResetTokenRepository,
                securityUtils,
                objectMapper
        );
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("AuditLog records GENERATE_ATTENDANCE_QR with session entityId and logged-in user")
    void auditMethodExecution_GenerateAttendanceQr_RecordsAuditLog() throws Throwable {
        // Arrange
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        Method startSessionMethod = AttendanceController.class.getMethod(
                "startSession",
                StartAttendanceSessionRequest.class,
                Authentication.class
        );
        Auditable auditable = startSessionMethod.getAnnotation(Auditable.class);

        StartAttendanceSessionRequest request = new StartAttendanceSessionRequest(
                100L,
                new BigDecimal("10.6385"),
                new BigDecimal("122.9723"),
                50
        );

        Authentication auth = mock(Authentication.class);
        Jwt jwt = mock(Jwt.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn(jwt);
        when(jwt.getSubject()).thenReturn("42");
        when(jwt.getClaimAsString("preferred_username")).thenReturn("instructor_jane");
        when(auth.getName()).thenReturn("42");

        when(securityUtils.resolveUserId(auth)).thenReturn(42L);

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(42L);
        when(mockUser.getUsername()).thenReturn("instructor_jane");
        when(userRepository.findById(42L)).thenReturn(Optional.of(mockUser));

        AttendanceSessionResponse responseDto = new AttendanceSessionResponse(
                777L,
                100L,
                "QR-ATT-XYZ",
                Instant.now().plusSeconds(900),
                new BigDecimal("10.6385"),
                new BigDecimal("122.9723"),
                50,
                "data:image/png;base64,ABC"
        );
        ResponseEntity<AttendanceSessionResponse> responseEntity = ResponseEntity.status(HttpStatus.CREATED).body(responseDto);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(startSessionMethod);
        when(signature.getDeclaringTypeName()).thenReturn(AttendanceController.class.getName());
        when(signature.getName()).thenReturn("startSession");
        when(signature.getParameterNames()).thenReturn(new String[]{"request", "authentication"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{request, auth});
        when(joinPoint.proceed()).thenReturn(responseEntity);

        // Act
        Object result = aspect.auditMethodExecution(joinPoint, auditable);

        // Assert
        assertThat(result).isEqualTo(responseEntity);

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).saveAuditLogAsync(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertThat(savedLog).isNotNull();
        assertThat(savedLog.getAction()).isEqualTo("GENERATE_ATTENDANCE_QR");
        assertThat(savedLog.getEntityName()).isEqualTo("AttendanceSession");
        assertThat(savedLog.getEntityId()).isEqualTo("777");
        assertThat(savedLog.getUserId()).isEqualTo(42L);
        assertThat(savedLog.getUsername()).isEqualTo("instructor_jane");
        assertThat(savedLog.getStatus()).isEqualTo("SUCCESS");
        assertThat(savedLog.getDetails()).contains("777");
    }

    @Test
    @DisplayName("AuditLog resolves numeric username fallback when SecurityUtils returns null")
    void auditMethodExecution_NumericUsernameFallback_ResolvesUser() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);

        Method startSessionMethod = AttendanceController.class.getMethod(
                "startSession",
                StartAttendanceSessionRequest.class,
                Authentication.class
        );
        Auditable auditable = startSessionMethod.getAnnotation(Auditable.class);

        StartAttendanceSessionRequest request = new StartAttendanceSessionRequest(100L, null, null, null);
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("15");
        when(auth.getName()).thenReturn("15");

        when(securityUtils.resolveUserId(auth)).thenReturn(null);

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(15L);
        when(mockUser.getUsername()).thenReturn("faculty_bob");
        when(userRepository.findById(15L)).thenReturn(Optional.of(mockUser));

        AttendanceSessionResponse responseDto = new AttendanceSessionResponse(
                888L, 100L, "QR-ATT-888", Instant.now(), null, null, 50, "data:image/png;base64,123"
        );

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(startSessionMethod);
        when(signature.getDeclaringTypeName()).thenReturn(AttendanceController.class.getName());
        when(signature.getName()).thenReturn("startSession");
        when(signature.getParameterNames()).thenReturn(new String[]{"request", "authentication"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{request, auth});
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok(responseDto));

        aspect.auditMethodExecution(joinPoint, auditable);

        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).saveAuditLogAsync(logCaptor.capture());

        AuditLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(15L);
        assertThat(savedLog.getUsername()).isEqualTo("faculty_bob");
        assertThat(savedLog.getAction()).isEqualTo("GENERATE_ATTENDANCE_QR");
        assertThat(savedLog.getEntityId()).isEqualTo("888");
    }
}
