package com.sdt.web_app.service.financial;

import com.sdt.web_app.dto.financial.FinancialDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.financial.CashierReceipt;
import com.sdt.web_app.entities.financial.StudentAssessmentInvoice;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.financial.CashierReceiptRepository;
import com.sdt.web_app.repositories.financial.StudentAccountLedgerRepository;
import com.sdt.web_app.repositories.financial.StudentAssessmentInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CashieringServiceTest {

    @Mock
    private CashierReceiptRepository receiptRepository;
    @Mock
    private StudentAssessmentInvoiceRepository invoiceRepository;
    @Mock
    private StudentAccountLedgerRepository ledgerRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrBookletService orBookletService;

    @InjectMocks
    private CashieringService cashieringService;

    private StudentProfile studentProfile;
    private User cashier;
    private StudentAssessmentInvoice invoice;

    @BeforeEach
    void setUp() {
        User user = User.builder().username("john_doe").build();
        studentProfile = StudentProfile.builder()
                .studentNumber("2026-CS-0002")
                .user(user)
                .build();

        cashier = User.builder().username("cashier_bob").build();

        invoice = StudentAssessmentInvoice.builder()
                .invoiceNumber("INV-2026-00001")
                .studentProfile(studentProfile)
                .netAssessedAmount(new BigDecimal("2000.00"))
                .totalPaidAmount(BigDecimal.ZERO)
                .outstandingBalance(new BigDecimal("2000.00"))
                .status(StudentAssessmentInvoice.InvoiceStatus.UNPAID)
                .build();
    }

    @Test
    @DisplayName("Phase 4: Cashier POS payment processes tender, issues serial OR, and updates invoice balance")
    void testProcessPayment_Success() {
        given(studentProfileRepository.findById(50L)).willReturn(Optional.of(studentProfile));
        given(userRepository.findById(10L)).willReturn(Optional.of(cashier));
        given(invoiceRepository.findById(100L)).willReturn(Optional.of(invoice));
        given(orBookletService.consumeNextOrNumber(any())).willReturn("OR-2026-00001");
        given(receiptRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        ProcessPaymentRequest request = new ProcessPaymentRequest(
                50L,
                100L,
                new BigDecimal("2000.00"), // tendered
                new BigDecimal("2000.00"), // paid
                "CASH",
                "REF-12345",
                "Full Payment"
        );

        CashierReceiptDto dto = cashieringService.processPayment(request, 10L);

        assertThat(dto).isNotNull();
        assertThat(dto.orNumber()).contains("OR-");
        assertThat(dto.amountTendered()).isEqualByComparingTo("2000.00");
        assertThat(dto.amountPaid()).isEqualByComparingTo("2000.00");
        assertThat(dto.changeAmount()).isEqualByComparingTo("0.00");
        assertThat(dto.paymentMethod()).isEqualTo("CASH");
        assertThat(invoice.getStatus()).isEqualTo(StudentAssessmentInvoice.InvoiceStatus.PAID);
        assertThat(invoice.getOutstandingBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Sprint 2: Check Payment records check number, drawee bank, and SUC fund cluster")
    void testProcessPayment_CheckWithFundCluster() {
        given(studentProfileRepository.findById(50L)).willReturn(Optional.of(studentProfile));
        given(userRepository.findById(10L)).willReturn(Optional.of(cashier));
        given(invoiceRepository.findById(100L)).willReturn(Optional.of(invoice));
        given(orBookletService.consumeNextOrNumber(any())).willReturn("OR-2026-00002");
        given(receiptRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        ProcessPaymentRequest request = new ProcessPaymentRequest(
                50L,
                100L,
                new BigDecimal("2000.00"),
                new BigDecimal("2000.00"),
                "CHECK",
                "CHK-REF-99",
                "Payment via Manager's Check",
                "CHK-987654",
                "Land Bank of the Philippines",
                "FUND_164"
        );

        CashierReceiptDto dto = cashieringService.processPayment(request, 10L);

        assertThat(dto).isNotNull();
        assertThat(dto.paymentMethod()).isEqualTo("CHECK");
        assertThat(dto.checkNumber()).isEqualTo("CHK-987654");
        assertThat(dto.draweeBank()).isEqualTo("Land Bank of the Philippines");
        assertThat(dto.fundClusterCode()).isEqualTo("FUND_164");
    }

    @Test
    @DisplayName("Sprint 2: EOD RCD Report groups collections dynamically by Fund Cluster")
    void testGenerateEodRcdReport_FundClusterGrouping() {
        given(userRepository.findById(10L)).willReturn(Optional.of(cashier));

        CashierReceipt r1 = CashierReceipt.builder()
                .id(1L)
                .orNumber("OR-2026-00001")
                .studentProfile(studentProfile)
                .amountPaid(new BigDecimal("1000.00"))
                .status(CashierReceipt.ReceiptStatus.VALID)
                .cashierUser(cashier)
                .fundClusterCode("FUND_164")
                .build();

        CashierReceipt r2 = CashierReceipt.builder()
                .id(2L)
                .orNumber("OR-2026-00002")
                .studentProfile(studentProfile)
                .amountPaid(new BigDecimal("500.00"))
                .status(CashierReceipt.ReceiptStatus.VALID)
                .cashierUser(cashier)
                .fundClusterCode("FUND_101")
                .build();

        given(receiptRepository.findByCashierUserId(10L)).willReturn(java.util.List.of(r1, r2));

        EodRcdReportDto report = cashieringService.generateEodRcdReport(10L, "2026-09-24");

        assertThat(report).isNotNull();
        assertThat(report.totalCollections()).isEqualByComparingTo("1500.00");
        assertThat(report.totalReceiptsIssued()).isEqualTo(2);
        assertThat(report.fundClusterSummaries()).hasSize(2);
        assertThat(report.fundClusterSummaries().stream().anyMatch(s -> "FUND_164".equals(s.fundClusterCode()))).isTrue();
        assertThat(report.fundClusterSummaries().stream().anyMatch(s -> "FUND_101".equals(s.fundClusterCode()))).isTrue();
    }

    @Test
    @DisplayName("Sprint 2: LinkBiz e-Payment webhook records receipt and updates student ledger")
    void testProcessLinkBizPayment_Success() {
        cashier.addRole(com.sdt.web_app.entities.authentication.Roles.CASHIER);
        given(studentProfileRepository.findByStudentNumber("2026-CS-0002")).willReturn(Optional.of(studentProfile));
        given(invoiceRepository.findByStudentProfileId(any())).willReturn(java.util.List.of(invoice));
        given(userRepository.findAll()).willReturn(java.util.List.of(cashier));
        given(orBookletService.consumeNextOrNumber(any())).willReturn("OR-2026-00003");
        given(receiptRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        LinkBizWebhookRequest request = new LinkBizWebhookRequest(
                "LANDBANK-SUC",
                "LB-20260924-001",
                "2026-CS-0002",
                new BigDecimal("1000.00"),
                "LANDBANK",
                "2026-09-24",
                "checksum_hash"
        );

        LinkBizWebhookResponse response = cashieringService.processLinkBizPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.orNumber()).isEqualTo("OR-2026-00003");
        assertThat(invoice.getTotalPaidAmount()).isEqualByComparingTo("1000.00");
        assertThat(invoice.getOutstandingBalance()).isEqualByComparingTo("1000.00");
    }
}
