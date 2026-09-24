package com.sdt.web_app.service.financial;

import com.sdt.web_app.dto.financial.FinancialDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.financial.OrBooklet;
import com.sdt.web_app.entities.financial.VoidedOfficialReceipt;
import com.sdt.web_app.exceptions.ResourceNotFoundException;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.financial.OrBookletRepository;
import com.sdt.web_app.repositories.financial.VoidedOfficialReceiptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrBookletService {

    private final OrBookletRepository bookletRepository;
    private final VoidedOfficialReceiptRepository voidedReceiptRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrBookletDto createBooklet(CreateOrBookletRequest request) {
        User cashier = userRepository.findById(request.assignedCashierId())
                .orElseThrow(() -> new ResourceNotFoundException("Cashier user not found: " + request.assignedCashierId()));

        OrBooklet booklet = OrBooklet.builder()
                .bookletCode(request.bookletCode().trim())
                .startOrNumber(request.startOrNumber().trim())
                .endOrNumber(request.endOrNumber().trim())
                .currentOrNumber(request.startOrNumber().trim())
                .assignedCashier(cashier)
                .status(OrBooklet.BookletStatus.ACTIVE)
                .build();

        OrBooklet saved = bookletRepository.save(booklet);
        log.info("COA O.R. Booklet assigned: {} to cashier {}", saved.getBookletCode(), cashier.getUsername());
        return mapToBookletDto(saved);
    }

    @Transactional(readOnly = true)
    public OrBookletDto getActiveBookletForCashier(Long cashierUserId) {
        OrBooklet booklet = bookletRepository.findActiveBookletByCashierId(cashierUserId)
                .orElseThrow(() -> new ResourceNotFoundException("No active O.R. booklet found for cashier user ID: " + cashierUserId));
        return mapToBookletDto(booklet);
    }

    @Transactional
    public VoidedOfficialReceiptDto voidOfficialReceipt(VoidOfficialReceiptRequest request, Long cashierUserId) {
        OrBooklet booklet = bookletRepository.findById(request.bookletId())
                .orElseThrow(() -> new ResourceNotFoundException("O.R. Booklet not found: " + request.bookletId()));

        User cashier = userRepository.findById(cashierUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Cashier user not found: " + cashierUserId));

        VoidedOfficialReceipt voided = VoidedOfficialReceipt.builder()
                .orNumber(request.orNumber().trim())
                .booklet(booklet)
                .voidedByCashier(cashier)
                .voidReason(request.voidReason().trim())
                .build();

        VoidedOfficialReceipt saved = voidedReceiptRepository.save(voided);
        log.warn("Official Receipt VOIDED: O.R. #{} by cashier {}. Reason: {}", saved.getOrNumber(), cashier.getUsername(), request.voidReason());
        return mapToVoidedDto(saved);
    }

    @Transactional(readOnly = true)
    public List<OrBookletDto> getCashierBooklets(Long cashierUserId) {
        return bookletRepository.findByAssignedCashierId(cashierUserId).stream()
                .map(this::mapToBookletDto)
                .toList();
    }

    @Transactional
    public String consumeNextOrNumber(Long cashierUserId) {
        OrBooklet booklet = bookletRepository.findActiveBookletByCashierId(cashierUserId).orElse(null);
        if (booklet == null) {
            return "OR-" + java.time.Year.now().getValue() + "-" + String.format("%05d", bookletRepository.count() + 1);
        }

        String currentOr = booklet.getCurrentOrNumber();
        try {
            java.util.regex.Matcher mCurrent = java.util.regex.Pattern.compile("^(.*?)(\\d+)$").matcher(currentOr);
            java.util.regex.Matcher mEnd = java.util.regex.Pattern.compile("^(.*?)(\\d+)$").matcher(booklet.getEndOrNumber());
            if (mCurrent.matches() && mEnd.matches()) {
                String prefix = mCurrent.group(1);
                String numStr = mCurrent.group(2);
                long currentNum = Long.parseLong(numStr);
                long endNum = Long.parseLong(mEnd.group(2));
                if (currentNum >= endNum) {
                    booklet.setStatus(OrBooklet.BookletStatus.EXHAUSTED);
                } else {
                    int numLen = numStr.length();
                    booklet.setCurrentOrNumber(prefix + String.format("%0" + numLen + "d", currentNum + 1));
                }
                bookletRepository.save(booklet);
            }
        } catch (Exception e) {
            log.warn("Could not auto-increment O.R. number format: {}", currentOr);
        }

        return currentOr;
    }

    private OrBookletDto mapToBookletDto(OrBooklet b) {
        return new OrBookletDto(
                b.getId(),
                b.getBookletCode(),
                b.getStartOrNumber(),
                b.getEndOrNumber(),
                b.getCurrentOrNumber(),
                b.getAssignedCashier().getId(),
                b.getAssignedCashier().getUsername(),
                b.getStatus().name(),
                b.getCreatedAt() != null ? b.getCreatedAt().toString() : java.time.Instant.now().toString()
        );
    }

    private VoidedOfficialReceiptDto mapToVoidedDto(VoidedOfficialReceipt v) {
        return new VoidedOfficialReceiptDto(
                v.getId(),
                v.getOrNumber(),
                v.getBooklet().getId(),
                v.getVoidedByCashier().getId(),
                v.getVoidedByCashier().getUsername(),
                v.getVoidReason(),
                v.getVoidedAt() != null ? v.getVoidedAt().toString() : java.time.Instant.now().toString()
        );
    }
}
