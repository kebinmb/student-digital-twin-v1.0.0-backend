package com.sdt.web_app.controller.institution;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.institution.HonorRollDtos.*;
import com.sdt.web_app.service.institution.HonorRollCertificateService;
import com.sdt.web_app.service.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/institution/certificates")
@RequiredArgsConstructor
public class CertificateRevocationAuditController {

    private final HonorRollCertificateService certificateService;
    private final SecurityUtils securityUtils;

    @Auditable(action = "REVOKE_CERTIFICATE", entityName = "CertificateRevocation")
    @PostMapping("/revoke")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<CertificateRevocationSummaryDto> revokeCertificate(
            @Valid @RequestBody CertificateRevocationRequest request,
            Authentication authentication
    ) {
        Long adminUserId = securityUtils.resolveUserId(authentication);
        CertificateRevocationSummaryDto summary = certificateService.revokeCertificate(
                request.certificateId(),
                adminUserId,
                request.reason()
        );
        return ResponseEntity.ok(summary);
    }

    @Auditable(action = "REINSTATE_CERTIFICATE", entityName = "CertificateRevocation")
    @PostMapping("/reinstate")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<CertificateRevocationSummaryDto> reinstateCertificate(
            @RequestParam String certificateId
    ) {
        CertificateRevocationSummaryDto summary = certificateService.reinstateCertificate(certificateId);
        return ResponseEntity.ok(summary);
    }

    @Auditable(action = "LIST_CERTIFICATE_REVOCATIONS", entityName = "CertificateRevocation")
    @GetMapping("/revocations")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN')")
    public ResponseEntity<List<CertificateRevocationSummaryDto>> getActiveRevocations() {
        return ResponseEntity.ok(certificateService.getActiveRevocations());
    }

    @Auditable(action = "EXPORT_CERTIFICATE_REVOCATIONS", entityName = "CertificateRevocation")
    @GetMapping("/revocations/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR', 'DEAN')")
    public ResponseEntity<?> exportRevocations(
            @RequestParam(defaultValue = "csv") String format
    ) {
        if ("html".equalsIgnoreCase(format)) {
            String html = certificateService.generateRevocationAuditHtmlReport();
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                    .body(html);
        } else {
            byte[] csv = certificateService.generateRevocationAuditCsv();
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"certificate-revocations-audit.csv\"")
                    .body(csv);
        }
    }
}
