package com.sdt.web_app.service.financial;

import com.sdt.web_app.dto.financial.FinancialDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.financial.StudentAccountLedger;
import com.sdt.web_app.entities.financial.StudentAssessmentInvoice;
import com.sdt.web_app.entities.financial.UnifastFheClaim;
import com.sdt.web_app.entities.financial.UnifastFheClaimItem;
import com.sdt.web_app.entities.institution.AcademicYear;
import com.sdt.web_app.entities.institution.Campus;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.institution.TermType;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.financial.StudentAccountLedgerRepository;
import com.sdt.web_app.repositories.financial.StudentAssessmentInvoiceRepository;
import com.sdt.web_app.repositories.financial.UnifastFheClaimItemRepository;
import com.sdt.web_app.repositories.financial.UnifastFheClaimRepository;
import com.sdt.web_app.repositories.institution.CampusRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.service.institution.TermService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UnifastBillingServiceTest {

    @Mock
    private UnifastFheClaimRepository claimRepository;
    @Mock
    private UnifastFheClaimItemRepository claimItemRepository;
    @Mock
    private StudentAssessmentInvoiceRepository invoiceRepository;
    @Mock
    private StudentAccountLedgerRepository ledgerRepository;
    @Mock
    private TermRepository termRepository;
    @Mock
    private TermService termService;
    @Mock
    private CampusRepository campusRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UnifastBillingService unifastBillingService;

    private User actor;
    private Campus campus;
    private Term term;
    private StudentProfile student;
    private StudentAssessmentInvoice invoice;
    private UnifastFheClaim claimBatch;
    private UnifastFheClaimItem claimItem;

    @BeforeEach
    void setUp() {
        actor = User.builder().id(1L).username("accountant_jane").build();

        campus = Campus.builder().id(10L).name("Talisay Main Campus").code("MAIN").build();

        AcademicYear ay = AcademicYear.builder().id(1L).code("AY 2026-2027").build();
        term = Term.builder().id(100L).academicYear(ay).termType(TermType.FIRST_SEM).build();

        Program program = Program.builder().id(5L).code("BSIT").name("Bachelor of Science in Information Technology").degreeLevel("UNDERGRADUATE").build();

        student = StudentProfile.builder()
                .id(50L)
                .studentNumber("2026-CS-0001")
                .firstName("Juan")
                .lastName("Dela Cruz")
                .middleName("Perez")
                .program(program)
                .yearLevel(1)
                .build();

        invoice = StudentAssessmentInvoice.builder()
                .id(200L)
                .invoiceNumber("INV-2026-0001")
                .studentProfile(student)
                .term(term)
                .totalTuitionFee(new BigDecimal("2000.00"))
                .totalLabFee(new BigDecimal("300.00"))
                .totalMiscFee(new BigDecimal("750.00"))
                .fheSubsidyAmount(new BigDecimal("3050.00"))
                .outstandingBalance(BigDecimal.ZERO)
                .status(StudentAssessmentInvoice.InvoiceStatus.FHE_COVERED)
                .fheEligible(true)
                .build();

        claimBatch = UnifastFheClaim.builder()
                .id(1L)
                .claimBatchNumber("UNIFAST-FHE-AY2026-2027-0001")
                .term(term)
                .campus(campus)
                .totalBeneficiaries(1)
                .totalTuitionClaimed(new BigDecimal("2000.00"))
                .totalTosfClaimed(new BigDecimal("1050.00"))
                .totalClaimAmount(new BigDecimal("3050.00"))
                .status(UnifastFheClaim.ClaimStatus.DRAFT)
                .createdByUser(actor)
                .build();

        claimItem = UnifastFheClaimItem.builder()
                .id(10L)
                .claimBatch(claimBatch)
                .studentProfile(student)
                .assessmentInvoice(invoice)
                .enrolledUnits(new BigDecimal("18.00"))
                .tuitionAmount(new BigDecimal("2000.00"))
                .miscAmount(new BigDecimal("750.00"))
                .labAmount(new BigDecimal("300.00"))
                .totalClaimedAmount(new BigDecimal("3050.00"))
                .verificationStatus("VERIFIED")
                .build();

        claimBatch.setClaimItems(new ArrayList<>(List.of(claimItem)));
    }

    @Test
    @DisplayName("Generate UniFAST Claim Batch: groups FHE eligible invoices into statutory batch")
    void testGenerateUnifastClaimBatch_Success() {
        given(termService.getTermById(100L)).willReturn(term);
        given(campusRepository.findById(10L)).willReturn(Optional.of(campus));
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));
        given(invoiceRepository.findFheEligibleInvoicesByTerm(100L)).willReturn(List.of(invoice));
        given(claimRepository.count()).willReturn(0L);
        given(claimRepository.save(any(UnifastFheClaim.class))).willAnswer(inv -> {
            UnifastFheClaim c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CreateUnifastClaimRequest request = new CreateUnifastClaimRequest(100L, 10L);
        UnifastFheClaimDto result = unifastBillingService.generateUnifastClaimBatch(request, 1L);

        assertThat(result).isNotNull();
        assertThat(result.claimBatchNumber()).contains("UNIFAST-FHE-");
        assertThat(result.totalBeneficiaries()).isEqualTo(1);
        assertThat(result.totalTuitionClaimed()).isEqualByComparingTo("2000.00");
        assertThat(result.totalTosfClaimed()).isEqualByComparingTo("1050.00");
        assertThat(result.totalClaimAmount()).isEqualByComparingTo("3050.00");
    }

    @Test
    @DisplayName("Export Form 2 CSV: builds statutory 28-column CSV with 13 TOSF itemized breakdowns")
    void testExportForm2Csv_StatutoryColumns() {
        given(claimRepository.findById(1L)).willReturn(Optional.of(claimBatch));
        given(claimItemRepository.findByClaimBatchId(1L)).willReturn(List.of(claimItem));

        String csv = unifastBillingService.exportForm2Csv(1L);

        assertThat(csv).isNotNull();
        String[] lines = csv.split("\n");
        assertThat(lines.length).isGreaterThanOrEqualTo(2);

        String header = lines[0];
        String[] cols = header.split(",");
        assertThat(cols).hasSize(28);
        assertThat(cols[0]).isEqualTo("Seq No");
        assertThat(cols[1]).isEqualTo("Student ID");
        assertThat(cols[12]).isEqualTo("Tuition Fee");
        assertThat(cols[25]).isEqualTo("Total TOSF");
        assertThat(cols[26]).isEqualTo("Total FHE Amount");
        assertThat(cols[27]).isEqualTo("Remarks");

        String dataRow = lines[1];
        assertThat(dataRow).contains("2026-CS-0001");
        assertThat(dataRow).contains("Dela Cruz");
        assertThat(dataRow).contains("Juan");
        assertThat(dataRow).contains("BSIT");
        assertThat(dataRow).contains("2000.00");
        assertThat(dataRow).contains("1050.00");
        assertThat(dataRow).contains("3050.00");
        assertThat(dataRow).contains("VERIFIED");
    }

    @Test
    @DisplayName("Disallow Claim Item: marks item DISQUALIFIED, records ledger charge, and updates parent batch")
    void testDisallowClaimItem_Success() {
        given(claimItemRepository.findById(10L)).willReturn(Optional.of(claimItem));
        given(claimItemRepository.save(any(UnifastFheClaimItem.class))).willAnswer(inv -> inv.getArgument(0));
        given(ledgerRepository.findLatestByStudentProfileId(50L)).willReturn(List.of());
        given(userRepository.findById(1L)).willReturn(Optional.of(actor));
        given(claimItemRepository.findByClaimBatchId(1L)).willReturn(List.of(claimItem));

        DisallowClaimItemRequest request = new DisallowClaimItemRequest("Exceeded Maximum Residency Rule cap under Section 7");
        UnifastFheClaimItemDto result = unifastBillingService.disallowClaimItem(10L, request, 1L);

        assertThat(result).isNotNull();
        assertThat(result.verificationStatus()).isEqualTo("DISQUALIFIED");

        // Verify ledger reversal charge was saved
        assertThat(invoice.getStatus()).isEqualTo(StudentAssessmentInvoice.InvoiceStatus.UNPAID);
        assertThat(invoice.getOutstandingBalance()).isEqualByComparingTo("3050.00");

        // Verify parent batch totals recalculated (item is disqualified)
        assertThat(claimBatch.getTotalBeneficiaries()).isEqualTo(0);
        assertThat(claimBatch.getTotalClaimAmount()).isEqualByComparingTo("0.00");
    }
}
