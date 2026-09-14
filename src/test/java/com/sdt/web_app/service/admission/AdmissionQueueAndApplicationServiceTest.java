package com.sdt.web_app.service.admission;

import com.sdt.web_app.dto.admission.AdmissionDtos.QueueTokenResponse;
import com.sdt.web_app.dto.admission.AdmissionDtos.SubmitAdmissionRequest;
import com.sdt.web_app.entities.admission.AdmissionConfig;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.exceptions.QueueSessionExpiredException;
import com.sdt.web_app.repositories.admission.AdmissionApplicationRepository;
import com.sdt.web_app.repositories.admission.AdmissionConfigRepository;
import com.sdt.web_app.repositories.admission.EntranceExamSlotRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.service.institution.TermService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdmissionQueueAndApplicationServiceTest {

    @Spy
    private AdmissionQueueService admissionQueueService = new AdmissionQueueService();

    @Mock
    private AdmissionApplicationRepository admissionApplicationRepository;

    @Mock
    private EntranceExamSlotRepository entranceExamSlotRepository;

    @Mock
    private AdmissionConfigRepository admissionConfigRepository;

    @Mock
    private ProgramRepository programRepository;

    @Mock
    private TermRepository termRepository;

    @Mock
    private TermService termService;

    @InjectMocks
    private AdmissionService admissionService;

    private Term testTerm;

    @BeforeEach
    void setUp() {
        testTerm = Term.builder()
                .id(4L)
                .academicYear(com.sdt.web_app.entities.institution.AcademicYear.builder().code("AY 2026-2027").build())
                .termType(com.sdt.web_app.entities.institution.TermType.FIRST_SEM)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Queue token issuance includes expiresAt ISO-8601 string and ttlSeconds")
    void issueToken_shouldIncludeExpiresAtAndTtlSeconds() {
        QueueTokenResponse tokenRes = admissionQueueService.issueToken("client-123");

        assertThat(tokenRes.queueToken()).isNotBlank();
        assertThat(tokenRes.status()).isEqualTo("ACTIVE");
        assertThat(tokenRes.allowedToProceed()).isTrue();
        assertThat(tokenRes.expiresAt()).isNotNull();
        assertThat(tokenRes.ttlSeconds()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Token validation separates verification from consumption")
    void validateToken_shouldNotConsumeTokenUntilExplicitlyConsumed() {
        QueueTokenResponse tokenRes = admissionQueueService.issueToken("client-456");
        String token = tokenRes.queueToken();

        // Validating repeatedly should remain true
        assertThat(admissionQueueService.validateToken(token)).isTrue();
        assertThat(admissionQueueService.validateToken(token)).isTrue();

        // Explicit consumption
        admissionQueueService.consumeToken(token);

        // After consumption, validation should fail
        assertThat(admissionQueueService.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("isEmailAvailable verifies email availability with case-insensitivity")
    void isEmailAvailable_shouldReturnCorrectAvailability() {
        when(termRepository.findByIsActiveTrue()).thenReturn(Optional.of(testTerm));
        when(admissionApplicationRepository.existsByEmailIgnoreCaseAndTermId("student@example.com", 4L)).thenReturn(false);
        when(admissionApplicationRepository.existsByEmailIgnoreCaseAndTermId("taken@example.com", 4L)).thenReturn(true);

        assertThat(admissionService.isEmailAvailable("student@example.com", null)).isTrue();
        assertThat(admissionService.isEmailAvailable("taken@example.com", null)).isFalse();
        assertThat(admissionService.isEmailAvailable("", null)).isFalse();
        assertThat(admissionService.isEmailAvailable(null, null)).isFalse();
    }

    @Test
    @DisplayName("submitApplication throws QueueSessionExpiredException when token is invalid or expired")
    void submitApplication_shouldThrowQueueSessionExpiredException_whenTokenInvalid() {
        when(termService.getTermById(1L)).thenReturn(testTerm);
        AdmissionConfig cfg = AdmissionConfig.builder().isActive(true).totalOpenedSlots(100).build();
        when(admissionConfigRepository.findByTermId(any())).thenReturn(Optional.of(cfg));
        when(admissionApplicationRepository.countByTermId(any())).thenReturn(5L);

        SubmitAdmissionRequest req = new SubmitAdmissionRequest(
                "ADM-Q-EXPIRED-TOKEN",
                1L, 1L, "juan@example.com"
        );

        assertThatThrownBy(() -> admissionService.submitApplication(req))
                .isInstanceOf(QueueSessionExpiredException.class)
                .hasMessageContaining("QUEUE_SESSION_EXPIRED");
    }
}