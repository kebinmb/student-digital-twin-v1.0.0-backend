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
    private final OrBookletService orBookletService;

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

        // Generate serial OR number from active booklet or fallback to serial generator
        String orNumber = (request.referenceNumber() != null && request.referenceNumber().startsWith("OR-")) 
                ? request.referenceNumber().trim() 
                : (orBookletService != null 
                        ? orBookletService.consumeNextOrNumber(cashierUserId) 
                        : "OR-" + Year.now().getValue() + "-" + String.format("%05d", receiptRepository.countTotalReceipts() + 1));

        String clusterCode = (request.fundClusterCode() != null && !request.fundClusterCode().isBlank()) 
                ? request.fundClusterCode().trim() : "FUND_164";

        CashierReceipt receipt = CashierReceipt.builder()
                .orNumber(orNumber)
                .studentProfile(student)
                .assessmentInvoice(invoice)
                .amountTendered(request.amountTendered())
                .amountPaid(request.amountPaid())
                .changeAmount(changeAmount)
                .paymentMethod(method)
                .referenceNumber(request.referenceNumber() != null ? request.referenceNumber().trim() : null)
                .checkNumber(request.checkNumber() != null ? request.checkNumber().trim() : null)
                .draweeBank(request.draweeBank() != null ? request.draweeBank().trim() : null)
                .fundClusterCode(clusterCode)
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
                .fundClusterCode(clusterCode)
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

    @Transactional(readOnly = true)
    public EodRcdReportDto generateEodRcdReport(Long cashierUserId, String reportDateStr) {
        User cashier = userRepository.findById(cashierUserId)
                .orElseThrow(() -> new EntityNotFoundException("Cashier user not found: " + cashierUserId));

        List<CashierReceipt> allReceipts = receiptRepository.findByCashierUserId(cashierUserId);

        BigDecimal totalCollected = allReceipts.stream()
                .filter(r -> r.getStatus() == CashierReceipt.ReceiptStatus.VALID)
                .map(CashierReceipt::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CashierReceiptDto> dtos = allReceipts.stream().map(this::mapToReceiptDto).toList();

        java.util.Map<String, List<CashierReceipt>> byCluster = allReceipts.stream()
                .filter(r -> r.getStatus() == CashierReceipt.ReceiptStatus.VALID)
                .collect(java.util.stream.Collectors.groupingBy(r -> r.getFundClusterCode() != null ? r.getFundClusterCode() : "FUND_164"));

        List<EodRcdFundClusterSummaryDto> fundSummaries = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, List<CashierReceipt>> entry : byCluster.entrySet()) {
            String clusterCode = entry.getKey();
            List<CashierReceipt> clusterReceipts = entry.getValue();
            BigDecimal clusterTotal = clusterReceipts.stream()
                    .map(CashierReceipt::getAmountPaid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            String clusterName = switch (clusterCode) {
                case "FUND_101" -> "Fund 101 - Regular Agency Fund (GAA Subsidy)";
                case "FUND_164" -> "Fund 164 - Special Trust Fund (Tuition & TOSF Income)";
                case "FUND_184" -> "Fund 184 - Revolving Fund / IGP";
                default -> clusterCode + " - Other Receipts";
            };
            fundSummaries.add(new EodRcdFundClusterSummaryDto(
                    clusterCode,
                    clusterName,
                    clusterTotal,
                    clusterReceipts.size()
            ));
        }

        if (fundSummaries.isEmpty()) {
            fundSummaries.add(new EodRcdFundClusterSummaryDto(
                    "FUND_164",
                    "Fund 164 - Special Trust Fund (Tuition & TOSF Income)",
                    BigDecimal.ZERO,
                    0
            ));
        }

        return new EodRcdReportDto(
                cashier.getId(),
                cashier.getUsername(),
                reportDateStr != null ? reportDateStr : java.time.LocalDate.now().toString(),
                totalCollected,
                dtos.size(),
                fundSummaries,
                dtos
        );
    }

    @Transactional
    public LinkBizWebhookResponse processLinkBizPayment(LinkBizWebhookRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("LinkBiz request payload cannot be null");
        }

        // Idempotency check: see if receipt already created for this bank reference number
        List<CashierReceipt> existing = receiptRepository.findAll().stream()
                .filter(r -> request.bankReferenceNumber() != null && request.bankReferenceNumber().equalsIgnoreCase(r.getReferenceNumber()))
                .toList();
        if (!existing.isEmpty()) {
            CashierReceipt r = existing.get(0);
            log.info("LinkBiz payment webhook duplicate received for bankReferenceNumber {}. Returning existing receipt {}.", request.bankReferenceNumber(), r.getOrNumber());
            return new LinkBizWebhookResponse("SUCCESS", "Payment already processed", r.getOrNumber(), Instant.now().toString());
        }

        StudentProfile student = studentProfileRepository.findByStudentNumber(request.studentNumber())
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found for student number: " + request.studentNumber()));

        List<StudentAssessmentInvoice> invoices = invoiceRepository.findByStudentProfileId(student.getId());
        StudentAssessmentInvoice invoice = invoices.isEmpty() ? null : invoices.get(0);

        User systemCashier = userRepository.findAll().stream()
                .filter(u -> u.getRoles() != null && (u.getRoles().contains(com.sdt.web_app.entities.authentication.Roles.CASHIER) || u.getRoles().contains(com.sdt.web_app.entities.authentication.Roles.ADMIN)))
                .findFirst()
                .orElseGet(() -> userRepository.findAll().stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("No system or cashier user available")));

        String orNumber = (orBookletService != null)
                ? orBookletService.consumeNextOrNumber(systemCashier.getId())
                : "OR-" + Year.now().getValue() + "-" + String.format("%05d", receiptRepository.countTotalReceipts() + 1);

        CashierReceipt receipt = CashierReceipt.builder()
                .orNumber(orNumber)
                .studentProfile(student)
                .assessmentInvoice(invoice)
                .amountTendered(request.transactionAmount())
                .amountPaid(request.transactionAmount())
                .changeAmount(BigDecimal.ZERO)
                .paymentMethod(CashierReceipt.PaymentMethod.LINKBIZ)
                .referenceNumber(request.bankReferenceNumber())
                .remarks("LandBank Link.BizPortal e-Payment Webhook Ref: " + request.bankReferenceNumber())
                .status(CashierReceipt.ReceiptStatus.VALID)
                .cashierUser(systemCashier)
                .issuedAt(Instant.now())
                .fundClusterCode("FUND_164")
                .build();

        CashierReceipt savedReceipt = receiptRepository.save(receipt);

        if (invoice != null) {
            BigDecimal newTotalPaid = invoice.getTotalPaidAmount().add(request.transactionAmount()).setScale(2, RoundingMode.HALF_UP);
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

        BigDecimal currentLedgerBalance = getCurrentLedgerBalance(student.getId());
        BigDecimal newLedgerBalance = currentLedgerBalance.subtract(request.transactionAmount()).setScale(2, RoundingMode.HALF_UP);

        StudentAccountLedger ledger = StudentAccountLedger.builder()
                .transactionNumber("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .studentProfile(student)
                .term(invoice != null ? invoice.getTerm() : null)
                .assessmentInvoice(invoice)
                .transactionType(StudentAccountLedger.TransactionType.PAYMENT)
                .description("e-Payment via LandBank Link.BizPortal (Ref: " + request.bankReferenceNumber() + ")")
                .debitAmount(BigDecimal.ZERO)
                .creditAmount(request.transactionAmount())
                .runningBalance(newLedgerBalance)
                .referenceNumber(savedReceipt.getOrNumber())
                .fundClusterCode("FUND_164")
                .createdByUser(systemCashier)
                .build();
        ledgerRepository.save(ledger);

        log.info("Processed LinkBiz payment OR {} for student {} amount {} bankRef {}",
                savedReceipt.getOrNumber(), student.getStudentNumber(), request.transactionAmount(), request.bankReferenceNumber());

        return new LinkBizWebhookResponse("SUCCESS", "Payment processed successfully", savedReceipt.getOrNumber(), Instant.now().toString());
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
        String studentName = sp != null ? sp.getFullName() : "Student #" + r.getId();

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
                r.getIssuedAt() != null ? r.getIssuedAt().toString() : Instant.now().toString(),
                r.getCheckNumber(),
                r.getDraweeBank(),
                r.getFundClusterCode() != null ? r.getFundClusterCode() : "FUND_164"
        );
    }
}
