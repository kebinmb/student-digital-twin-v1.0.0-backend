package com.sdt.web_app.service.financial;

import com.sdt.web_app.dto.financial.FinancialDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.financial.OrBooklet;
import com.sdt.web_app.entities.financial.VoidedOfficialReceipt;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.financial.OrBookletRepository;
import com.sdt.web_app.repositories.financial.VoidedOfficialReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrBookletServiceTest {

    @Mock
    private OrBookletRepository bookletRepository;

    @Mock
    private VoidedOfficialReceiptRepository voidedReceiptRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrBookletService bookletService;

    private User cashier;
    private OrBooklet activeBooklet;

    @BeforeEach
    void setUp() {
        cashier = User.builder()
                .id(10L)
                .username("cashier_jane")
                .build();

        activeBooklet = OrBooklet.builder()
                .id(1L)
                .bookletCode("BKL-2026-001")
                .startOrNumber("OR-2026-00001")
                .endOrNumber("OR-2026-00050")
                .currentOrNumber("OR-2026-00001")
                .assignedCashier(cashier)
                .status(OrBooklet.BookletStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Create O.R. Booklet: assigns booklet to cashier and initializes serial number range")
    void testCreateBooklet_Success() {
        given(userRepository.findById(10L)).willReturn(Optional.of(cashier));
        given(bookletRepository.save(any())).willAnswer(inv -> {
            OrBooklet b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        CreateOrBookletRequest request = new CreateOrBookletRequest(
                "BKL-2026-001",
                "OR-2026-00001",
                "OR-2026-00050",
                10L
        );

        OrBookletDto result = bookletService.createBooklet(request);

        assertThat(result).isNotNull();
        assertThat(result.bookletCode()).isEqualTo("BKL-2026-001");
        assertThat(result.startOrNumber()).isEqualTo("OR-2026-00001");
        assertThat(result.currentOrNumber()).isEqualTo("OR-2026-00001");
        assertThat(result.endOrNumber()).isEqualTo("OR-2026-00050");
        assertThat(result.status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Consume Next O.R. Number: auto-increments current serial number sequentially")
    void testConsumeNextOrNumber_IncrementsCorrectly() {
        given(bookletRepository.findActiveBookletByCashierId(10L)).willReturn(Optional.of(activeBooklet));
        given(bookletRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        String consumed = bookletService.consumeNextOrNumber(10L);

        assertThat(consumed).isEqualTo("OR-2026-00001");
        assertThat(activeBooklet.getCurrentOrNumber()).isEqualTo("OR-2026-00002");
    }

    @Test
    @DisplayName("Consume Next O.R. Number: marks booklet EXHAUSTED when reaching end of range")
    void testConsumeNextOrNumber_MarksExhausted() {
        activeBooklet.setCurrentOrNumber("OR-2026-00050");
        given(bookletRepository.findActiveBookletByCashierId(10L)).willReturn(Optional.of(activeBooklet));
        given(bookletRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        String consumed = bookletService.consumeNextOrNumber(10L);

        assertThat(consumed).isEqualTo("OR-2026-00050");
        assertThat(activeBooklet.getStatus()).isEqualTo(OrBooklet.BookletStatus.EXHAUSTED);
    }

    @Test
    @DisplayName("Void Official Receipt: records voided receipt and audit justification")
    void testVoidOfficialReceipt_Success() {
        given(bookletRepository.findById(1L)).willReturn(Optional.of(activeBooklet));
        given(userRepository.findById(10L)).willReturn(Optional.of(cashier));
        given(voidedReceiptRepository.save(any())).willAnswer(inv -> {
            VoidedOfficialReceipt v = inv.getArgument(0);
            v.setId(99L);
            return v;
        });

        VoidOfficialReceiptRequest request = new VoidOfficialReceiptRequest(
                "OR-2026-00005",
                1L,
                "Spoiled Form 51 - printer error"
        );

        VoidedOfficialReceiptDto result = bookletService.voidOfficialReceipt(request, 10L);

        assertThat(result).isNotNull();
        assertThat(result.orNumber()).isEqualTo("OR-2026-00005");
        assertThat(result.voidReason()).isEqualTo("Spoiled Form 51 - printer error");
        assertThat(result.voidedByCashierUsername()).isEqualTo("cashier_jane");
    }
}
