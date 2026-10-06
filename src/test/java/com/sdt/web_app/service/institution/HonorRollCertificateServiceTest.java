package com.sdt.web_app.service.institution;

import com.sdt.web_app.dto.institution.HonorRollDtos.CertificateVerificationDto;
import com.sdt.web_app.dto.institution.HonorRollDtos.HonorStudentDto;
import com.sdt.web_app.dto.institution.HonorRollDtos.TermHonorRollReportDto;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.AcademicYear;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.institution.TermType;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.CertificateRevocationRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class HonorRollCertificateServiceTest {

    @Mock
    private HonorRollComputationService honorRollComputationService;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private TermRepository termRepository;

    @Mock
    private CertificateRevocationRepository certificateRevocationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HonorRollCertificateService certificateService;

    private Term term;
    private StudentProfile student;

    @BeforeEach
    void setUp() {
        AcademicYear ay = AcademicYear.builder().id(1L).code("AY 2026-2027").build();
        term = Term.builder().id(10L).academicYear(ay).termType(TermType.FIRST_SEM).build();

        Program prog = Program.builder().id(1L).code("BSCS").name("BS Computer Science").build();
        student = StudentProfile.builder()
                .id(100L)
                .studentNumber("2026-0001")
                .program(prog)
                .build();
    }

    @Test
    @DisplayName("Generate certificate metadata generates valid SHA-256 hash and QR data URI")
    void generateCertificateMetadata_Success() {
        given(termRepository.findById(10L)).willReturn(Optional.of(term));
        given(studentProfileRepository.findById(100L)).willReturn(Optional.of(student));

        HonorStudentDto honorStudent = new HonorStudentDto(
                100L,
                "2026-0001",
                "Jane Doe",
                "BSCS",
                "PRESIDENTS_LIST",
                1.15,
                BigDecimal.valueOf(18.0),
                1
        );

        TermHonorRollReportDto report = new TermHonorRollReportDto(
                10L,
                "AY 2026-2027 FIRST_SEM",
                null,
                10,
                1,
                List.of(honorStudent)
        );

        given(honorRollComputationService.computeTermHonorRoll(10L, null)).willReturn(report);

        CertificateVerificationDto cert = certificateService.generateCertificateMetadata(10L, 100L);

        assertThat(cert).isNotNull();
        assertThat(cert.certificateId()).isEqualTo("CERT-HONOR-10-2026-0001");
        assertThat(cert.honorCategory()).isEqualTo("PRESIDENTS_LIST");
        assertThat(cert.gpa()).isEqualTo(1.15);
        assertThat(cert.verificationHash()).isNotBlank();
        assertThat(cert.verificationHash().length()).isEqualTo(64); // SHA-256 hex length
        assertThat(cert.qrVerificationDataUrl()).startsWith("data:image/png;base64,");
        assertThat(cert.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("Revoke certificate creates active revocation record")
    void revokeCertificate_Success() {
        String certId = "CERT-HONOR-10-2026-0001";
        given(certificateRevocationRepository.findByCertificateId(certId)).willReturn(Optional.empty());
        given(userRepository.findById(1L)).willReturn(Optional.empty());
        given(certificateRevocationRepository.save(org.mockito.ArgumentMatchers.any())).willAnswer(inv -> inv.getArgument(0));

        var result = certificateService.revokeCertificate(certId, 1L, "Academic dishonesty sanction");

        assertThat(result).isNotNull();
        assertThat(result.certificateId()).isEqualTo(certId);
        assertThat(result.revocationReason()).isEqualTo("Academic dishonesty sanction");
        assertThat(result.isActive()).isTrue();
    }
}
