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
}
