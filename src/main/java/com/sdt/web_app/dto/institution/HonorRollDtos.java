package com.sdt.web_app.dto.institution;

import java.math.BigDecimal;
import java.util.List;

public class HonorRollDtos {

    public record HonorStudentDto(
            Long studentId,
            String studentNumber,
            String fullName,
            String programCode,
            String honorCategory,
            Double termGpa,
            BigDecimal totalUnits,
            int rank
    ) {}

    public record TermHonorRollReportDto(
            Long termId,
            String termName,
            Long programId,
            int totalEvaluated,
            int totalQualified,
            List<HonorStudentDto> honorees
    ) {}

    public record CertificateVerificationDto(
            String certificateId,
            Long studentId,
            String studentNumber,
            String studentName,
            Long termId,
            String termName,
            String programCode,
            String honorCategory,
            Double gpa,
            String verificationHash,
            String issuedAt,
            String qrVerificationDataUrl,
            boolean isRevoked,
            String revocationReason,
            String revokedAt
    ) {
        // Convenience 12-param constructor for unrevoked certificates
        public CertificateVerificationDto(
                String certificateId,
                Long studentId,
                String studentNumber,
                String studentName,
                Long termId,
                String termName,
                String programCode,
                String honorCategory,
                Double gpa,
                String verificationHash,
                String issuedAt,
                String qrVerificationDataUrl
        ) {
            this(
                    certificateId,
                    studentId,
                    studentNumber,
                    studentName,
                    termId,
                    termName,
                    programCode,
                    honorCategory,
                    gpa,
                    verificationHash,
                    issuedAt,
                    qrVerificationDataUrl,
                    false,
                    null,
                    null
            );
        }
    }

    public record CertificateRevocationRequest(
            String certificateId,
            String reason
    ) {}

    public record CertificateRevocationSummaryDto(
            Long id,
            String certificateId,
            Long revokedByUserId,
            String revokedByUsername,
            String revocationReason,
            String revokedAt,
            String reinstatedAt,
            boolean isActive
    ) {}
}
