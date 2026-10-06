package com.sdt.web_app.controller.institution;

import com.sdt.web_app.dto.institution.HonorRollDtos.*;
import com.sdt.web_app.service.institution.HonorRollCertificateService;
import com.sdt.web_app.service.security.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class CertificateRevocationAuditControllerTest {

    @Mock
    private HonorRollCertificateService certificateService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CertificateRevocationAuditController controller;

    @Test
    @DisplayName("Revoke certificate calls service with resolved user and returns summary")
    void revokeCertificate_Success() {
        Authentication auth = mock(Authentication.class);
        CertificateRevocationRequest request = new CertificateRevocationRequest("CERT-HONOR-1-2026-0001", "Plagiarism");
        CertificateRevocationSummaryDto summary = new CertificateRevocationSummaryDto(
                1L, "CERT-HONOR-1-2026-0001", 10L, "admin", "Plagiarism", "2026-10-06T10:00:00Z", null, true
        );

        given(securityUtils.resolveUserId(auth)).willReturn(10L);
        given(certificateService.revokeCertificate("CERT-HONOR-1-2026-0001", 10L, "Plagiarism")).willReturn(summary);

        ResponseEntity<CertificateRevocationSummaryDto> response = controller.revokeCertificate(request, auth);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().certificateId()).isEqualTo("CERT-HONOR-1-2026-0001");
        assertThat(response.getBody().isActive()).isTrue();
    }

    @Test
    @DisplayName("Reinstate certificate calls service and returns summary")
    void reinstateCertificate_Success() {
        CertificateRevocationSummaryDto summary = new CertificateRevocationSummaryDto(
                1L, "CERT-HONOR-1-2026-0001", 10L, "admin", "Plagiarism", "2026-10-06T10:00:00Z", "2026-10-06T11:00:00Z", false
        );

        given(certificateService.reinstateCertificate("CERT-HONOR-1-2026-0001")).willReturn(summary);

        ResponseEntity<CertificateRevocationSummaryDto> response = controller.reinstateCertificate("CERT-HONOR-1-2026-0001");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isActive()).isFalse();
    }

    @Test
    @DisplayName("Get active revocations returns list of summaries")
    void getActiveRevocations_Success() {
        CertificateRevocationSummaryDto summary = new CertificateRevocationSummaryDto(
                1L, "CERT-HONOR-1-2026-0001", 10L, "admin", "Plagiarism", "2026-10-06T10:00:00Z", null, true
        );

        given(certificateService.getActiveRevocations()).willReturn(List.of(summary));

        ResponseEntity<List<CertificateRevocationSummaryDto>> response = controller.getActiveRevocations();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    @DisplayName("Export revocations as CSV returns 200 with text/csv")
    void exportRevocations_Csv_Success() {
        byte[] csvBytes = "ID,Certificate ID,Revoked By\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        given(certificateService.generateRevocationAuditCsv()).willReturn(csvBytes);

        ResponseEntity<?> response = controller.exportRevocations("csv");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType().toString()).contains("text/csv");
        assertThat((byte[]) response.getBody()).isEqualTo(csvBytes);
    }

    @Test
    @DisplayName("Export revocations as HTML returns 200 with text/html")
    void exportRevocations_Html_Success() {
        String html = "<html><body>Audit Report</body></html>";
        given(certificateService.generateRevocationAuditHtmlReport()).willReturn(html);

        ResponseEntity<?> response = controller.exportRevocations("html");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getHeaders().getContentType().toString()).contains("text/html");
        assertThat((String) response.getBody()).isEqualTo(html);
    }
}
