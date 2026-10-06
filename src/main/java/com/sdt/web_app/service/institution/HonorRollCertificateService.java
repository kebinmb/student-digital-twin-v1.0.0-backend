package com.sdt.web_app.service.institution;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sdt.web_app.dto.institution.HonorRollDtos.*;
import com.sdt.web_app.entities.enrollment.StudentEnrollment;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.exceptions.ResourceNotFoundException;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.institution.CertificateRevocation;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.StudentEnrollmentRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.CertificateRevocationRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HonorRollCertificateService {

    private final HonorRollComputationService honorRollComputationService;
    private final StudentProfileRepository studentProfileRepository;
    private final TermRepository termRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final CertificateRevocationRepository certificateRevocationRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CertificateVerificationDto generateCertificateMetadata(Long termId, Long studentId) {
        Term term = termRepository.findById(termId)
                .orElseThrow(() -> new ResourceNotFoundException("Term not found: " + termId));

        StudentProfile student = studentProfileRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found: " + studentId));

        TermHonorRollReportDto report = honorRollComputationService.computeTermHonorRoll(termId, null);
        HonorStudentDto honorInfo = report.honorees().stream()
                .filter(h -> h.studentId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Student is not an active honoree for term " + term.getId()));

        String certId = "CERT-HONOR-" + termId + "-" + student.getStudentNumber();
        String issuedAt = LocalDate.now().toString();

        // Cryptographic SHA-256 integrity hash
        String rawPayload = String.format("%s|%s|%s|%s|%.2f|CHMSU-OFFICIAL-VERIFICATION",
                certId,
                student.getStudentNumber(),
                honorInfo.honorCategory(),
                term.getId(),
                honorInfo.termGpa());

        String verificationHash = computeSha256(rawPayload);

        // Verification QR Data payload
        String qrPayload = String.format("https://portal.chmsu.edu.ph/verify/honor-certificate?cert=%s&hash=%s",
                certId,
                verificationHash.substring(0, 16));

        String qrDataUrl = generateQrDataUrl(qrPayload);

        String termName = (term.getAcademicYear() != null ? term.getAcademicYear().getCode() : "") + " " +
                (term.getTermType() != null ? term.getTermType().name() : "");

        boolean isRevoked = false;
        String revocationReason = null;
        String revokedAt = null;

        if (certificateRevocationRepository != null) {
            var revocationOpt = certificateRevocationRepository.findByCertificateIdAndActiveTrue(certId);
            if (revocationOpt.isPresent()) {
                isRevoked = true;
                revocationReason = revocationOpt.get().getRevocationReason();
                revokedAt = revocationOpt.get().getRevokedAt() != null ? revocationOpt.get().getRevokedAt().toString() : null;
            }
        }

        return new CertificateVerificationDto(
                certId,
                student.getId(),
                student.getStudentNumber(),
                student.getFullName(),
                term.getId(),
                termName.trim(),
                student.getProgram() != null ? student.getProgram().getCode() : "N/A",
                honorInfo.honorCategory(),
                honorInfo.termGpa(),
                verificationHash,
                issuedAt,
                qrDataUrl,
                isRevoked,
                revocationReason,
                revokedAt
        );
    }

    @Transactional(readOnly = true)
    public CertificateVerificationDto verifyPublicCertificate(String certId, String hashPrefix) {
        if (certId == null || !certId.startsWith("CERT-HONOR-")) {
            throw new IllegalArgumentException("Invalid certificate identifier format.");
        }

        // certId format: CERT-HONOR-<termId>-<studentNumber>
        String[] parts = certId.split("-", 4);
        if (parts.length < 4) {
            throw new IllegalArgumentException("Malformed certificate identifier.");
        }

        Long termId;
        try {
            termId = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid term ID in certificate.");
        }

        String studentNumber = parts[3];

        StudentProfile student = studentProfileRepository.findByStudentNumber(studentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for cert: " + studentNumber));

        CertificateVerificationDto cert = generateCertificateMetadata(termId, student.getId());

        if (hashPrefix != null && !hashPrefix.isBlank()) {
            if (!cert.verificationHash().startsWith(hashPrefix.toLowerCase())) {
                throw new SecurityException("Cryptographic verification failed: hash checksum mismatch.");
            }
        }

        return cert;
    }

    public static String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "HASH-" + System.currentTimeMillis();
        }
    }

    public static String generateQrDataUrl(String text) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());
        } catch (Exception e) {
            return "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200'><text x='50%' y='50%'>VERIFIED</text></svg>";
        }
    }

    public static byte[] generateQrPngBytes(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            log.warn("Failed to generate QR PNG bytes: {}", e.getMessage());
            return new byte[0];
        }
    }

    public static String generatePrintableHtmlCertificate(CertificateVerificationDto cert, HonorStudentDto h, String qrDataUrl) {
        String honorTitle = "PRESIDENTS_LIST".equalsIgnoreCase(cert.honorCategory()) ? "President's List" : "Dean's List";
        String honorDescription = "PRESIDENTS_LIST".equalsIgnoreCase(cert.honorCategory())
                ? "in recognition of extraordinary academic scholarship, exemplary integrity, and distinction as one of the highest ranking scholars"
                : "in recognition of distinguished academic standing, dedication to scholastic pursuit, and sustained intellectual excellence";

        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <title>Certificate of Academic Distinction - " + cert.studentName() + "</title>\n" +
                "  <style>\n" +
                "    @page { size: A4 landscape; margin: 0; }\n" +
                "    * { box-sizing: border-box; }\n" +
                "    body { margin: 0; padding: 20px; font-family: 'Georgia', 'Times New Roman', serif; background-color: #f8fafc; color: #1e293b; display: flex; justify-content: center; align-items: center; min-height: 100vh; }\n" +
                "    .cert-frame { width: 1000px; height: 700px; padding: 32px; background: #ffffff; border: 12px double #c59b27; box-shadow: 0 10px 25px rgba(0,0,0,0.1); position: relative; display: flex; flex-direction: column; justify-content: space-between; text-align: center; }\n" +
                "    .cert-inner-border { border: 2px solid #0f2a4a; height: 100%; padding: 24px; display: flex; flex-direction: column; justify-content: space-between; position: relative; }\n" +
                "    .cert-header h1 { margin: 0; font-size: 22px; letter-spacing: 2px; color: #0f2a4a; text-transform: uppercase; font-family: 'Cinzel', 'Trajan Pro', serif; }\n" +
                "    .cert-header h2 { margin: 4px 0 0; font-size: 13px; letter-spacing: 1.5px; color: #64748b; text-transform: uppercase; font-weight: normal; }\n" +
                "    .cert-title { margin-top: 14px; font-size: 34px; color: #c59b27; font-weight: bold; text-transform: uppercase; letter-spacing: 3px; border-bottom: 2px solid #e2e8f0; display: inline-block; padding-bottom: 6px; }\n" +
                "    .cert-presented-to { margin-top: 12px; font-size: 14px; color: #64748b; text-transform: uppercase; letter-spacing: 2px; }\n" +
                "    .cert-student-name { font-size: 32px; color: #0f2a4a; font-weight: bold; font-family: 'Playfair Display', Georgia, serif; margin: 10px 0; border-bottom: 1px solid #cbd5e1; display: inline-block; padding: 0 30px; }\n" +
                "    .cert-body { font-size: 15px; line-height: 1.6; color: #334155; max-width: 800px; margin: 0 auto; }\n" +
                "    .cert-body strong { color: #0f2a4a; }\n" +
                "    .cert-footer { display: flex; justify-content: space-between; align-items: flex-end; margin-top: 20px; padding: 0 30px; }\n" +
                "    .cert-sig { text-align: center; width: 220px; }\n" +
                "    .sig-line { border-bottom: 1px solid #475569; margin-bottom: 6px; height: 35px; }\n" +
                "    .sig-name { font-size: 13px; font-weight: bold; color: #0f2a4a; }\n" +
                "    .sig-role { font-size: 11px; color: #64748b; text-transform: uppercase; }\n" +
                "    .cert-qr-block { display: flex; flex-direction: column; align-items: center; justify-content: center; font-size: 10px; color: #64748b; }\n" +
                "    .cert-qr-img { width: 90px; height: 90px; border: 1px solid #cbd5e1; padding: 4px; border-radius: 4px; }\n" +
                "    .cert-id-tag { font-family: monospace; font-size: 9px; margin-top: 4px; color: #475569; }\n" +
                "    @media print {\n" +
                "      body { padding: 0; background: transparent; }\n" +
                "      .cert-frame { box-shadow: none; width: 100%; height: 100vh; }\n" +
                "      .no-print { display: none !important; }\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"no-print\" style=\"position: fixed; top: 15px; right: 15px; z-index: 9999;\">\n" +
                "    <button onclick=\"window.print()\" style=\"padding: 10px 20px; background: #0f2a4a; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; font-family: sans-serif;\">\n" +
                "      🖨️ Print / Save as PDF\n" +
                "    </button>\n" +
                "  </div>\n" +
                "  <div class=\"cert-frame\">\n" +
                "    <div class=\"cert-inner-border\">\n" +
                "      <div class=\"cert-header\">\n" +
                "        <h1>Carlos Hilado Memorial State University</h1>\n" +
                "        <h2>Office of Academic Affairs & University Registrar</h2>\n" +
                "      </div>\n" +
                "      <div class=\"cert-title\">" + honorTitle + "</div>\n" +
                "      <div class=\"cert-presented-to\">This official distinction is proudly conferred upon</div>\n" +
                "      <div><span class=\"cert-student-name\">" + cert.studentName() + "</span></div>\n" +
                "      <div class=\"cert-body\">\n" +
                "        Student No. <strong>" + cert.studentNumber() + "</strong> of the program <strong>" + cert.programCode() + "</strong>,<br>\n" +
                "        " + honorDescription + " during the <strong>" + cert.termName() + "</strong> academic term with a Term GPA of <strong>" + String.format("%.2f", cert.gpa()) + "</strong> (Honors Rank #" + h.rank() + ").\n" +
                "      </div>\n" +
                "      <div class=\"cert-footer\">\n" +
                "        <div class=\"cert-sig\">\n" +
                "          <div class=\"sig-line\"></div>\n" +
                "          <div class=\"sig-name\">UNIVERSITY REGISTRAR</div>\n" +
                "          <div class=\"sig-role\">Office of Admissions & Records</div>\n" +
                "        </div>\n" +
                "        <div class=\"cert-qr-block\">\n" +
                "          <img class=\"cert-qr-img\" src=\"" + qrDataUrl + "\" alt=\"Verification QR Code\">\n" +
                "          <div class=\"cert-id-tag\">" + cert.certificateId() + "</div>\n" +
                "          <div class=\"cert-id-tag\">HASH: " + cert.verificationHash().substring(0, 16) + "...</div>\n" +
                "        </div>\n" +
                "        <div class=\"cert-sig\">\n" +
                "          <div class=\"sig-line\"></div>\n" +
                "          <div class=\"sig-name\">UNIVERSITY PRESIDENT</div>\n" +
                "          <div class=\"sig-role\">Carlos Hilado Memorial State University</div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "</body>\n" +
                "</html>";
    }

    @Transactional(readOnly = true)
    public byte[] generateTermCertificatesZip(Long termId, Long programId) {
        TermHonorRollReportDto report = honorRollComputationService.computeTermHonorRoll(termId, programId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {

            for (HonorStudentDto h : report.honorees()) {
                CertificateVerificationDto cert = generateCertificateMetadata(termId, h.studentId());

                String certSummary = String.format(
                        "=====================================================\n" +
                        "CARLOS HILADO MEMORIAL STATE UNIVERSITY\n" +
                        "OFFICIAL CERTIFICATE OF ACADEMIC DISTINCTION\n" +
                        "=====================================================\n" +
                        "Certificate ID:      %s\n" +
                        "Student Number:      %s\n" +
                        "Student Full Name:   %s\n" +
                        "Academic Program:    %s\n" +
                        "Academic Term:       %s\n" +
                        "Term Honors Rank:    #%d\n" +
                        "Honor Category:      %s\n" +
                        "Term GPA:            %.2f\n" +
                        "Total Graded Units:  %s\n" +
                        "Date of Conferment:  %s\n" +
                        "-----------------------------------------------------\n" +
                        "Cryptographic SHA-256 Verification Hash:\n" +
                        "%s\n" +
                        "Online Verification Link:\n" +
                        "https://portal.chmsu.edu.ph/verify/honor-certificate?cert=%s&hash=%s\n" +
                        "=====================================================\n",
                        cert.certificateId(),
                        cert.studentNumber(),
                        cert.studentName(),
                        cert.programCode(),
                        cert.termName(),
                        h.rank(),
                        cert.honorCategory(),
                        cert.gpa(),
                        h.totalUnits(),
                        cert.issuedAt(),
                        cert.verificationHash(),
                        cert.certificateId(),
                        cert.verificationHash().substring(0, 16)
                );

                String programFolder = (cert.programCode() != null && !cert.programCode().isBlank()) ? cert.programCode() : "GENERAL";

                // 1. Plaintext ledger audit file
                String txtFileName = String.format("%s/%s_%s_Certificate.txt", programFolder, cert.studentNumber(), cert.honorCategory());
                java.util.zip.ZipEntry txtEntry = new java.util.zip.ZipEntry(txtFileName);
                zos.putNextEntry(txtEntry);
                zos.write(certSummary.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                // 2. High-resolution QR Code PNG image (300x300)
                String verifyUrl = String.format("https://portal.chmsu.edu.ph/verify/honor-certificate?cert=%s&hash=%s",
                        cert.certificateId(), cert.verificationHash().substring(0, 16));
                byte[] qrPngBytes = generateQrPngBytes(verifyUrl, 300, 300);
                if (qrPngBytes.length > 0) {
                    String qrFileName = String.format("%s/%s_%s_QRCode.png", programFolder, cert.studentNumber(), cert.honorCategory());
                    java.util.zip.ZipEntry qrEntry = new java.util.zip.ZipEntry(qrFileName);
                    zos.putNextEntry(qrEntry);
                    zos.write(qrPngBytes);
                    zos.closeEntry();
                }

                // 3. High-fidelity printable HTML certificate with embedded QR data URL
                String htmlCertificate = generatePrintableHtmlCertificate(cert, h, cert.qrVerificationDataUrl());
                String htmlFileName = String.format("%s/%s_%s_Certificate.html", programFolder, cert.studentNumber(), cert.honorCategory());
                java.util.zip.ZipEntry htmlEntry = new java.util.zip.ZipEntry(htmlFileName);
                zos.putNextEntry(htmlEntry);
                zos.write(htmlCertificate.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate zip export for term {}", termId, e);
            throw new RuntimeException("Could not build batch certificates zip: " + e.getMessage(), e);
        }
    }

    @Transactional
    public CertificateRevocationSummaryDto revokeCertificate(String certId, Long adminUserId, String reason) {
        if (certId == null || certId.isBlank()) {
            throw new IllegalArgumentException("Certificate ID is required.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Revocation reason is required.");
        }

        User adminUser = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;

        CertificateRevocation record = certificateRevocationRepository.findByCertificateId(certId)
                .orElseGet(() -> CertificateRevocation.builder()
                        .certificateId(certId)
                        .build());

        record.setRevokedBy(adminUser);
        record.setRevocationReason(reason);
        record.setActive(true);
        record.setReinstatedAt(null);
        record.setRevokedAt(Instant.now());

        CertificateRevocation saved = certificateRevocationRepository.save(record);
        log.warn("Revoked certificate {} by user {}: {}", certId, adminUserId, reason);

        return new CertificateRevocationSummaryDto(
                saved.getId(),
                saved.getCertificateId(),
                saved.getRevokedBy() != null ? saved.getRevokedBy().getId() : null,
                saved.getRevokedBy() != null ? saved.getRevokedBy().getUsername() : null,
                saved.getRevocationReason(),
                saved.getRevokedAt() != null ? saved.getRevokedAt().toString() : null,
                null,
                true
        );
    }

    @Transactional
    public CertificateRevocationSummaryDto reinstateCertificate(String certId) {
        CertificateRevocation record = certificateRevocationRepository.findByCertificateId(certId)
                .orElseThrow(() -> new ResourceNotFoundException("Revocation record not found for certificate: " + certId));

        record.setActive(false);
        record.setReinstatedAt(Instant.now());
        CertificateRevocation saved = certificateRevocationRepository.save(record);
        log.info("Reinstated certificate {}", certId);

        return new CertificateRevocationSummaryDto(
                saved.getId(),
                saved.getCertificateId(),
                saved.getRevokedBy() != null ? saved.getRevokedBy().getId() : null,
                saved.getRevokedBy() != null ? saved.getRevokedBy().getUsername() : null,
                saved.getRevocationReason(),
                saved.getRevokedAt() != null ? saved.getRevokedAt().toString() : null,
                saved.getReinstatedAt() != null ? saved.getReinstatedAt().toString() : null,
                false
        );
    }

    @Transactional(readOnly = true)
    public List<CertificateRevocationSummaryDto> getActiveRevocations() {
        return certificateRevocationRepository.findByActiveTrueOrderByRevokedAtDesc().stream()
                .map(r -> new CertificateRevocationSummaryDto(
                        r.getId(),
                        r.getCertificateId(),
                        r.getRevokedBy() != null ? r.getRevokedBy().getId() : null,
                        r.getRevokedBy() != null ? r.getRevokedBy().getUsername() : null,
                        r.getRevocationReason(),
                        r.getRevokedAt() != null ? r.getRevokedAt().toString() : null,
                        r.getReinstatedAt() != null ? r.getReinstatedAt().toString() : null,
                        r.isActive()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] generateRevocationAuditCsv() {
        List<CertificateRevocation> list = certificateRevocationRepository.findAllByOrderByRevokedAtDesc();
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Certificate ID,Revoked By,Reason,Revoked At,Reinstated At,Status\n");
        for (CertificateRevocation r : list) {
            String admin = r.getRevokedBy() != null ? r.getRevokedBy().getUsername() : "SYSTEM";
            String reason = r.getRevocationReason() != null ? r.getRevocationReason().replace("\"", "\"\"") : "";
            String revokedAt = r.getRevokedAt() != null ? r.getRevokedAt().toString() : "";
            String reinstatedAt = r.getReinstatedAt() != null ? r.getReinstatedAt().toString() : "";
            String status = r.isActive() ? "ACTIVE_REVOKED" : "REINSTATED";
            sb.append(r.getId()).append(",")
              .append("\"").append(r.getCertificateId()).append("\",")
              .append("\"").append(admin).append("\",")
              .append("\"").append(reason).append("\",")
              .append("\"").append(revokedAt).append("\",")
              .append("\"").append(reinstatedAt).append("\",")
              .append("\"").append(status).append("\"\n");
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public String generateRevocationAuditHtmlReport() {
        List<CertificateRevocation> list = certificateRevocationRepository.findAllByOrderByRevokedAtDesc();
        long activeCount = list.stream().filter(CertificateRevocation::isActive).count();
        long reinstatedCount = list.size() - activeCount;

        StringBuilder rows = new StringBuilder();
        for (CertificateRevocation r : list) {
            String statusClass = r.isActive() ? "badge-danger" : "badge-success";
            String statusLabel = r.isActive() ? "ACTIVE REVOCATION" : "REINSTATED";
            String admin = r.getRevokedBy() != null ? r.getRevokedBy().getUsername() : "—";
            rows.append("<tr>")
                .append("<td><code>").append(r.getCertificateId()).append("</code></td>")
                .append("<td>").append(admin).append("</td>")
                .append("<td>").append(r.getRevocationReason() != null ? r.getRevocationReason() : "—").append("</td>")
                .append("<td>").append(r.getRevokedAt() != null ? r.getRevokedAt().toString() : "—").append("</td>")
                .append("<td>").append(r.getReinstatedAt() != null ? r.getReinstatedAt().toString() : "—").append("</td>")
                .append("<td><span class=\"badge ").append(statusClass).append("\">").append(statusLabel).append("</span></td>")
                .append("</tr>\n");
        }

        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <title>Certificate Revocation & Integrity Audit Report</title>\n" +
                "  <style>\n" +
                "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; margin: 40px; color: #1e293b; background: #fff; }\n" +
                "    .header { border-bottom: 2px solid #0f2a4a; padding-bottom: 16px; margin-bottom: 24px; }\n" +
                "    .header h1 { margin: 0; font-size: 24px; color: #0f2a4a; }\n" +
                "    .header p { margin: 4px 0 0; color: #64748b; font-size: 14px; }\n" +
                "    .summary-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-bottom: 24px; }\n" +
                "    .summary-card { padding: 16px; border-radius: 8px; border: 1px solid #e2e8f0; background: #f8fafc; }\n" +
                "    .summary-card.danger { border-color: #fecaca; background: #fef2f2; }\n" +
                "    .summary-value { font-size: 28px; font-weight: bold; color: #0f2a4a; }\n" +
                "    .summary-card.danger .summary-value { color: #dc2626; }\n" +
                "    .summary-label { font-size: 12px; color: #64748b; text-transform: uppercase; margin-top: 4px; }\n" +
                "    table { width: 100%; border-collapse: collapse; margin-top: 16px; font-size: 13px; }\n" +
                "    th, td { padding: 10px 12px; text-align: left; border-bottom: 1px solid #e2e8f0; }\n" +
                "    th { background: #f1f5f9; color: #334155; font-weight: 600; text-transform: uppercase; font-size: 11px; letter-spacing: 0.5px; }\n" +
                "    code { font-family: monospace; font-size: 12px; color: #0f2a4a; background: #f1f5f9; padding: 2px 6px; border-radius: 4px; }\n" +
                "    .badge { display: inline-block; padding: 2px 8px; border-radius: 9999px; font-size: 11px; font-weight: 600; }\n" +
                "    .badge-danger { background: #fee2e2; color: #991b1b; }\n" +
                "    .badge-success { background: #dcfce7; color: #166534; }\n" +
                "    .no-print { position: fixed; top: 20px; right: 20px; }\n" +
                "    .btn-print { padding: 8px 16px; background: #0f2a4a; color: #fff; border: none; border-radius: 6px; font-weight: 600; cursor: pointer; }\n" +
                "    @media print { .no-print { display: none; } body { margin: 20px; } }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <div class=\"no-print\">\n" +
                "    <button class=\"btn-print\" onclick=\"window.print()\">🖨️ Print / Save PDF</button>\n" +
                "  </div>\n" +
                "  <div class=\"header\">\n" +
                "    <h1>Carlos Hilado Memorial State University</h1>\n" +
                "    <p>Academic Integrity Registry & Certificate Revocation Audit Report — Generated at " + Instant.now() + "</p>\n" +
                "  </div>\n" +
                "  <div class=\"summary-grid\">\n" +
                "    <div class=\"summary-card danger\">\n" +
                "      <div class=\"summary-value\">" + activeCount + "</div>\n" +
                "      <div class=\"summary-label\">Active Revocations</div>\n" +
                "    </div>\n" +
                "    <div class=\"summary-card\">\n" +
                "      <div class=\"summary-value\">" + reinstatedCount + "</div>\n" +
                "      <div class=\"summary-label\">Reinstated Credentials</div>\n" +
                "    </div>\n" +
                "    <div class=\"summary-card\">\n" +
                "      <div class=\"summary-value\">" + list.size() + "</div>\n" +
                "      <div class=\"summary-label\">Total Audit Records</div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <table>\n" +
                "    <thead>\n" +
                "      <tr>\n" +
                "        <th>Certificate ID</th>\n" +
                "        <th>Revoked By</th>\n" +
                "        <th>Reason / Finding</th>\n" +
                "        <th>Revoked At</th>\n" +
                "        <th>Reinstated At</th>\n" +
                "        <th>Status</th>\n" +
                "      </tr>\n" +
                "    </thead>\n" +
                "    <tbody>\n" +
                rows +
                "    </tbody>\n" +
                "  </table>\n" +
                "</body>\n" +
                "</html>";
    }
}
