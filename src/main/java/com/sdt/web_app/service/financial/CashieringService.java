package com.sdt.web_app.service.financial;

import com.sdt.web_app.dto.financial.FinancialDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.financial.CashierReceipt;
import com.sdt.web_app.entities.financial.StudentAccountLedger;
import com.sdt.web_app.entities.financial.StudentAssessmentInvoice;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.financial.CashierReceiptRepository;
import com.sdt.web_app.repositories.financial.StudentAccountLedgerRepository;
import com.sdt.web_app.repositories.financial.StudentAssessmentInvoiceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sdt.web_app.dto.common.SliceResponse;
import com.sdt.web_app.utils.SortPropertyMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashieringService {

    private final CashierReceiptRepository receiptRepository;
    private final StudentAssessmentInvoiceRepository invoiceRepository;
    private final StudentAccountLedgerRepository ledgerRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public CashierReceiptDto processPayment(ProcessPaymentRequest request, Long cashierUserId) {
        if (request == null || request.studentProfileId() == null) {
            throw new IllegalArgumentException("Student profile ID is required for payment processing");
        }
        if (request.amountTendered() == null || request.amountTendered().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount tendered must be greater than zero");
        }
        if (request.amountPaid() == null || request.amountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount paid must be greater than zero");
        }
        if (request.amountTendered().compareTo(request.amountPaid()) < 0) {
            throw new IllegalArgumentException("Amount tendered (" + request.amountTendered() + ") cannot be less than amount paid (" + request.amountPaid() + ")");
        }

        StudentProfile student = studentProfileRepository.findByUserId(request.studentProfileId())
                .or(() -> studentProfileRepository.findById(request.studentProfileId()))
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found with ID: " + request.studentProfileId()));

        User cashierUser = userRepository.findById(cashierUserId)
                .orElseThrow(() -> new EntityNotFoundException("Cashier user not found with ID: " + cashierUserId));

        StudentAssessmentInvoice invoice = null;
        if (request.assessmentInvoiceId() != null) {
            invoice = invoiceRepository.findById(request.assessmentInvoiceId())
                    .orElseThrow(() -> new EntityNotFoundException("Assessment invoice not found: " + request.assessmentInvoiceId()));
        } else {
            List<StudentAssessmentInvoice> invoices = invoiceRepository.findByStudentProfileId(student.getId());
            if (!invoices.isEmpty()) {
                invoice = invoices.get(0);
            }
        }

        BigDecimal changeAmount = request.amountTendered().subtract(request.amountPaid()).setScale(2, RoundingMode.HALF_UP);
        CashierReceipt.PaymentMethod method;
        try {
            method = CashierReceipt.PaymentMethod.valueOf(request.paymentMethod().toUpperCase().trim());
        } catch (Exception e) {
            method = CashierReceipt.PaymentMethod.CASH;
        }

        // Generate serial OR number OR-YYYY-00001
        long totalReceipts = receiptRepository.countTotalReceipts() + 1;
        String orNumber = "OR-" + Year.now().getValue() + "-" + String.format("%05d", totalReceipts);

        CashierReceipt receipt = CashierReceipt.builder()
                .orNumber(orNumber)
                .studentProfile(student)
                .assessmentInvoice(invoice)
                .amountTendered(request.amountTendered())
                .amountPaid(request.amountPaid())
                .changeAmount(changeAmount)
                .paymentMethod(method)
                .referenceNumber(request.referenceNumber() != null ? request.referenceNumber().trim() : null)
                .remarks(request.remarks() != null ? request.remarks().trim() : "Payment received")
                .status(CashierReceipt.ReceiptStatus.VALID)
                .cashierUser(cashierUser)
                .issuedAt(Instant.now())
                .build();

        CashierReceipt savedReceipt = receiptRepository.save(receipt);

        // Update Invoice balance if attached
        if (invoice != null) {
            BigDecimal newTotalPaid = invoice.getTotalPaidAmount().add(request.amountPaid()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal newBalance = invoice.getNetAssessedAmount().subtract(newTotalPaid).setScale(2, RoundingMode.HALF_UP);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                newBalance = BigDecimal.ZERO;
            }

            invoice.setTotalPaidAmount(newTotalPaid);
            invoice.setOutstandingBalance(newBalance);

            if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
                invoice.setStatus(StudentAssessmentInvoice.InvoiceStatus.PAID);
            } else {
                invoice.setStatus(StudentAssessmentInvoice.InvoiceStatus.PARTIAL);
            }
            invoiceRepository.save(invoice);
        }

        // Add Payment Ledger Entry
        BigDecimal currentBalance = getCurrentLedgerBalance(student.getId());
        BigDecimal newLedgerBalance = currentBalance.subtract(request.amountPaid()).setScale(2, RoundingMode.HALF_UP);

        StudentAccountLedger ledger = StudentAccountLedger.builder()
                .transactionNumber("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .studentProfile(student)
                .term(invoice != null ? invoice.getTerm() : null)
                .assessmentInvoice(invoice)
                .transactionType(StudentAccountLedger.TransactionType.PAYMENT)
                .description("Payment Received via OR " + savedReceipt.getOrNumber())
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(request.amountPaid())
                .runningBalance(newLedgerBalance)
                .referenceNumber(savedReceipt.getOrNumber())
                .createdByUser(cashierUser)
                .build();
        ledgerRepository.save(ledger);

        log.info("Processed cashier payment OR {} student {} paid {} change {} by cashier {}",
                savedReceipt.getOrNumber(), student.getStudentNumber(), request.amountPaid(), changeAmount, cashierUser.getUsername());

        return mapToReceiptDto(savedReceipt);
    }

    @Transactional(readOnly = true)
    public CashierReceiptDto getReceiptByOrNumber(String orNumber) {
        CashierReceipt receipt = receiptRepository.findByOrNumber(orNumber)
                .orElseThrow(() -> new EntityNotFoundException("Official receipt not found with OR#: " + orNumber));
        return mapToReceiptDto(receipt);
    }

    @Transactional(readOnly = true)
    public List<CashierReceiptDto> getReceiptsByStudentProfile(Long studentProfileId) {
        return receiptRepository.findByStudentProfileId(studentProfileId).stream()
                .map(this::mapToReceiptDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public SliceResponse<CashierReceiptDto> getReceiptsByStudentProfileSlice(Long studentProfileId, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = SortPropertyMapper.createCashierReceiptPageable(page, size, sortBy, sortDir);
        Slice<CashierReceipt> slice = receiptRepository.findByStudentProfileId(studentProfileId, pageable);
        Slice<CashierReceiptDto> responseSlice = slice.map(this::mapToReceiptDto);
        return SliceResponse.from(responseSlice);
    }

    private BigDecimal getCurrentLedgerBalance(Long studentProfileId) {
        List<StudentAccountLedger> latest = ledgerRepository.findLatestByStudentProfileId(studentProfileId);
        if (latest.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return latest.get(0).getRunningBalance();
    }

    private CashierReceiptDto mapToReceiptDto(CashierReceipt r) {
        StudentProfile sp = r.getStudentProfile();
        String studentName = (sp != null && sp.getUser() != null) ? sp.getUser().getUsername() : "Student #" + (sp != null ? sp.getStudentNumber() : r.getId());

        return new CashierReceiptDto(
                r.getId(),
                r.getOrNumber(),
                sp != null ? sp.getId() : null,
                sp != null ? sp.getStudentNumber() : "N/A",
                studentName,
                r.getAssessmentInvoice() != null ? r.getAssessmentInvoice().getId() : null,
                r.getAmountTendered(),
                r.getAmountPaid(),
                r.getChangeAmount(),
                r.getPaymentMethod() != null ? r.getPaymentMethod().name() : "CASH",
                r.getReferenceNumber(),
                r.getRemarks(),
                r.getStatus() != null ? r.getStatus().name() : "VALID",
                r.getCashierUser() != null ? r.getCashierUser().getId() : null,
                r.getCashierUser() != null ? r.getCashierUser().getUsername() : "System",
                r.getIssuedAt() != null ? r.getIssuedAt().toString() : Instant.now().toString()
        );
    }
}
