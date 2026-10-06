package com.sdt.web_app.controller.institution;

import com.sdt.web_app.dto.institution.HonorRollDtos.CertificateVerificationDto;
import com.sdt.web_app.service.institution.HonorRollCertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/verify")
@RequiredArgsConstructor
public class PublicCertificateVerificationController {

    private final HonorRollCertificateService certificateService;

    @GetMapping("/honor-certificate")
    public ResponseEntity<CertificateVerificationDto> verifyCertificate(
            @RequestParam String cert,
            @RequestParam(required = false) String hash
    ) {
        return ResponseEntity.ok(certificateService.verifyPublicCertificate(cert, hash));
    }
}
