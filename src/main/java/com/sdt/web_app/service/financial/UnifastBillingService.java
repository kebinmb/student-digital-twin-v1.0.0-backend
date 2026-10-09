package com.sdt.web_app.service.financial;

import com.sdt.web_app.dto.financial.FinancialDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.financial.StudentAssessmentInvoice;
import com.sdt.web_app.entities.financial.UnifastFheClaim;
import com.sdt.web_app.entities.financial.UnifastFheClaimItem;
import com.sdt.web_app.entities.institution.Campus;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.financial.StudentAssessmentInvoiceRepository;
import com.sdt.web_app.repositories.financial.UnifastFheClaimItemRepository;
import com.sdt.web_app.repositories.financial.UnifastFheClaimRepository;
import com.sdt.web_app.repositories.institution.CampusRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.entities.financial.StudentAccountLedger;
import com.sdt.web_app.repositories.financial.StudentAccountLedgerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnifastBillingService {

    private final UnifastFheClaimRepository claimRepository;
    private final UnifastFheClaimItemRepository claimItemRepository;
    private final StudentAssessmentInvoiceRepository invoiceRepository;
    private final StudentAccountLedgerRepository ledgerRepository;
    private final TermRepository termRepository;
    private final com.sdt.web_app.service.institution.TermService termService;
    private final CampusRepository campusRepository;
    private final UserRepository userRepository;

    @Transactional
    public UnifastFheClaimDto generateUnifastClaimBatch(CreateUnifastClaimRequest request, Long actorUserId) {
        Term term = termService.getTermById(request.termId());

        Campus campus = campusRepository.findById(request.campusId())
                .orElseThrow(() -> new EntityNotFoundException("Campus not found with ID: " + request.campusId()));

        User actorUser = userRepository.findById(actorUserId)
                .orElseThrow(() -> new EntityNotFoundException("Actor user not found with ID: " + actorUserId));

        List<StudentAssessmentInvoice> eligibleInvoices = invoiceRepository.findFheEligibleInvoicesByTerm(request.termId());

        String yearCode = (term.getAcademicYear() != null && term.getAcademicYear().getCode() != null) ? term.getAcademicYear().getCode().replaceAll("\\s+", "") : "AY2026";
        String batchNo = "UNIFAST-FHE-" + yearCode + "-" + String.format("%04d", claimRepository.count() + 1);

        UnifastFheClaim batch = UnifastFheClaim.builder()
                .claimBatchNumber(batchNo)
                .term(term)
                .campus(campus)
                .totalBeneficiaries(0)
                .totalTuitionClaimed(BigDecimal.ZERO)
                .totalTosfClaimed(BigDecimal.ZERO)
                .totalClaimAmount(BigDecimal.ZERO)
                .status(UnifastFheClaim.ClaimStatus.DRAFT)
                .createdByUser(actorUser)
                .createdAt(Instant.now())
                .build();

        UnifastFheClaim savedBatch = claimRepository.save(batch);

        BigDecimal totalTuition = BigDecimal.ZERO;
        BigDecimal totalTosf = BigDecimal.ZERO;
        BigDecimal totalClaim = BigDecimal.ZERO;
        List<UnifastFheClaimItem> claimItems = new ArrayList<>();

        for (StudentAssessmentInvoice inv : eligibleInvoices) {
            StudentProfile student = inv.getStudentProfile();
            BigDecimal units = (inv.getStudentEnrollment() != null && inv.getStudentEnrollment().getTotalCreditUnits() != null)
                    ? inv.getStudentEnrollment().getTotalCreditUnits()
                    : new BigDecimal("18.00");

            BigDecimal tuition = inv.getTotalTuitionFee();
            BigDecimal misc = inv.getTotalMiscFee();
            BigDecimal lab = inv.getTotalLabFee();
            BigDecimal claimAmount = inv.getFheSubsidyAmount();

            totalTuition = totalTuition.add(tuition);
            totalTosf = totalTosf.add(misc).add(lab);
            totalClaim = totalClaim.add(claimAmount);

            UnifastFheClaimItem item = UnifastFheClaimItem.builder()
                    .claimBatch(savedBatch)
                    .studentProfile(student)
                    .assessmentInvoice(inv)
                    .enrolledUnits(units)
                    .tuitionAmount(tuition)
                    .miscAmount(misc)
                    .labAmount(lab)
                    .totalClaimedAmount(claimAmount)
                    .verificationStatus("VERIFIED")
                    .build();

            claimItems.add(item);
        }

        if (!claimItems.isEmpty()) {
            claimItemRepository.saveAll(claimItems);
        }

        savedBatch.setTotalBeneficiaries(claimItems.size());
        savedBatch.setTotalTuitionClaimed(totalTuition.setScale(2, RoundingMode.HALF_UP));
        savedBatch.setTotalTosfClaimed(totalTosf.setScale(2, RoundingMode.HALF_UP));
        savedBatch.setTotalClaimAmount(totalClaim.setScale(2, RoundingMode.HALF_UP));
        savedBatch.setClaimItems(claimItems);

        UnifastFheClaim updatedBatch = claimRepository.save(savedBatch);

        log.info("Generated UniFAST FHE Claim Batch {} for term {} campus {}: {} beneficiaries, total claim={}",
                batchNo, yearCode, campus.getCode(), claimItems.size(), totalClaim);

        return mapToClaimDto(updatedBatch);
    }

    @Transactional
    public UnifastFheClaimItemDto disallowClaimItem(Long itemId, DisallowClaimItemRequest request, Long actorUserId) {
        UnifastFheClaimItem item = claimItemRepository.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("UniFAST claim item not found with ID: " + itemId));

        item.setVerificationStatus("DISQUALIFIED");
        UnifastFheClaimItem savedItem = claimItemRepository.save(item);

        StudentProfile student = item.getStudentProfile();
        UnifastFheClaim claimBatch = item.getClaimBatch();

        // Reversals on Student Account Ledger
        if (student != null) {
            List<StudentAccountLedger> latest = ledgerRepository.findLatestByStudentProfileId(student.getId());
            BigDecimal currentBalance = latest.isEmpty() ? BigDecimal.ZERO : latest.get(0).getRunningBalance();
            BigDecimal newBalance = currentBalance.add(item.getTotalClaimedAmount()).setScale(2, RoundingMode.HALF_UP);

            User actor = actorUserId != null ? userRepository.findById(actorUserId).orElse(null) : null;

            StudentAccountLedger disallowLedger = StudentAccountLedger.builder()
                    .transactionNumber("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .studentProfile(student)
                    .assessmentInvoice(item.getAssessmentInvoice())
                    .term(claimBatch != null ? claimBatch.getTerm() : null)
                    .transactionType(StudentAccountLedger.TransactionType.CHARGE)
                    .description("UniFAST Audit Disallowance Reversal: " + (request != null && request.reason() != null ? request.reason() : "Disallowed by COA/UniFAST Audit"))
                    .debitAmount(item.getTotalClaimedAmount())
                    .creditAmount(BigDecimal.ZERO)
                    .runningBalance(newBalance)
                    .referenceNumber(claimBatch != null ? claimBatch.getClaimBatchNumber() : "DISALLOW-" + itemId)
                    .createdByUser(actor)
                    .build();
            ledgerRepository.save(disallowLedger);

            // Update student invoice outstanding balance & status
            if (item.getAssessmentInvoice() != null) {
                StudentAssessmentInvoice invoice = item.getAssessmentInvoice();
                invoice.setOutstandingBalance(invoice.getOutstandingBalance().add(item.getTotalClaimedAmount()).setScale(2, RoundingMode.HALF_UP));
                invoice.setStatus(StudentAssessmentInvoice.InvoiceStatus.UNPAID);
                invoiceRepository.save(invoice);
            }
        }

        // Recalculate parent batch totals
        if (claimBatch != null) {
            List<UnifastFheClaimItem> activeItems = claimItemRepository.findByClaimBatchId(claimBatch.getId()).stream()
                    .filter(i -> !"DISQUALIFIED".equals(i.getVerificationStatus()))
                    .toList();

            BigDecimal tuition = activeItems.stream().map(UnifastFheClaimItem::getTuitionAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal tosf = activeItems.stream().map(i -> i.getMiscAmount().add(i.getLabAmount())).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal total = activeItems.stream().map(UnifastFheClaimItem::getTotalClaimedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

            claimBatch.setTotalBeneficiaries(activeItems.size());
            claimBatch.setTotalTuitionClaimed(tuition.setScale(2, RoundingMode.HALF_UP));
            claimBatch.setTotalTosfClaimed(tosf.setScale(2, RoundingMode.HALF_UP));
            claimBatch.setTotalClaimAmount(total.setScale(2, RoundingMode.HALF_UP));
            claimRepository.save(claimBatch);
        }

        log.warn("UniFAST claim item {} disallowed for student profile {}. Reason: {}",
                itemId, student != null ? student.getStudentNumber() : "N/A", request != null ? request.reason() : "Audit Disallowance");

        return mapToClaimItemDto(savedItem);
    }

    @Transactional(readOnly = true)
    public List<UnifastFheClaimDto> getClaimsByTerm(Long termId) {
        return claimRepository.findByTermId(termId).stream()
                .map(this::mapToClaimDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnifastFheClaimDto getClaimBatch(Long claimBatchId) {
        UnifastFheClaim claim = claimRepository.findById(claimBatchId)
                .orElseThrow(() -> new EntityNotFoundException("UniFAST FHE claim batch not found: " + claimBatchId));
        return mapToClaimDto(claim);
    }

    @Transactional(readOnly = true)
    public String exportForm2Csv(Long claimBatchId) {
        UnifastFheClaim claim = claimRepository.findById(claimBatchId)
                .orElseThrow(() -> new EntityNotFoundException("UniFAST FHE claim batch not found: " + claimBatchId));

        List<UnifastFheClaimItem> items = claimItemRepository.findByClaimBatchId(claimBatchId);

        StringBuilder csv = new StringBuilder();
        csv.append("Seq No,Student ID,Learner Reference No,Last Name,First Name,Middle Name,Extension Name,Sex,Program Code,Program Name,Year Level,Academic Units,Tuition Fee,Athletic Fee,Computer Fee,Cultural Fee,Development Fee,Admission Fee,Guidance Fee,Handbook Fee,Laboratory Fee,Library Fee,Medical Dental Fee,Registration Fee,School ID Fee,Total TOSF,Total FHE Amount,Remarks\n");

        int seqNo = 1;
        for (UnifastFheClaimItem item : items) {
            StudentProfile sp = item.getStudentProfile();
            String studentNumber = sp != null ? sp.getStudentNumber() : "N/A";
            String lrn = "N/A";
            String lastName = (sp != null && sp.getLastName() != null) ? sp.getLastName() : (sp != null && sp.getUser() != null ? sp.getUser().getUsername() : "N/A");
            String firstName = (sp != null && sp.getFirstName() != null) ? sp.getFirstName() : "N/A";
            String middleName = (sp != null && sp.getMiddleName() != null) ? sp.getMiddleName() : "";
            String extName = (sp != null && sp.getSuffix() != null) ? sp.getSuffix() : "";
            String sex = "M/F";
            String progCode = (sp != null && sp.getProgram() != null) ? sp.getProgram().getCode() : "N/A";
            String progName = (sp != null && sp.getProgram() != null) ? sp.getProgram().getName() : "N/A";
            int yearLevel = sp != null ? sp.getYearLevel() : 1;
            BigDecimal units = item.getEnrolledUnits() != null ? item.getEnrolledUnits() : BigDecimal.ZERO;

            BigDecimal tuitionFee = item.getTuitionAmount() != null ? item.getTuitionAmount() : BigDecimal.ZERO;
            BigDecimal labFee = item.getLabAmount() != null ? item.getLabAmount() : BigDecimal.ZERO;
            BigDecimal misc = item.getMiscAmount() != null ? item.getMiscAmount() : BigDecimal.ZERO;

            BigDecimal baseAthletic = new BigDecimal("100.00");
            BigDecimal baseComputer = new BigDecimal("250.00");
            BigDecimal baseCultural = new BigDecimal("50.00");
            BigDecimal baseDev = new BigDecimal("150.00");
            BigDecimal baseAdmission = (yearLevel == 1) ? new BigDecimal("100.00") : BigDecimal.ZERO;
            BigDecimal baseGuidance = new BigDecimal("50.00");
            BigDecimal baseHandbook = (yearLevel == 1) ? new BigDecimal("50.00") : BigDecimal.ZERO;
            BigDecimal baseLibrary = new BigDecimal("150.00");
            BigDecimal baseMedical = new BigDecimal("100.00");
            BigDecimal baseReg = new BigDecimal("100.00");
            BigDecimal baseId = (yearLevel == 1) ? new BigDecimal("50.00") : BigDecimal.ZERO;

            BigDecimal baseTotalMisc = baseAthletic.add(baseComputer).add(baseCultural).add(baseDev)
                    .add(baseAdmission).add(baseGuidance).add(baseHandbook).add(baseLibrary)
                    .add(baseMedical).add(baseReg).add(baseId);

            BigDecimal athleticFee, computerFee, culturalFee, devFee, admissionFee;
            BigDecimal guidanceFee, handbookFee, libraryFee, medicalFee, regFee, idFee;

            if (misc.compareTo(BigDecimal.ZERO) > 0 && baseTotalMisc.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ratio = misc.divide(baseTotalMisc, 6, RoundingMode.HALF_UP);
                athleticFee = baseAthletic.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                computerFee = baseComputer.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                culturalFee = baseCultural.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                devFee = baseDev.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                admissionFee = baseAdmission.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                guidanceFee = baseGuidance.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                handbookFee = baseHandbook.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                libraryFee = baseLibrary.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                medicalFee = baseMedical.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                idFee = baseId.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                BigDecimal miscSubtotal = athleticFee.add(computerFee).add(culturalFee).add(devFee)
                        .add(admissionFee).add(guidanceFee).add(handbookFee).add(libraryFee)
                        .add(medicalFee).add(idFee);
                regFee = misc.subtract(miscSubtotal).setScale(2, RoundingMode.HALF_UP);
            } else {
                athleticFee = BigDecimal.ZERO.setScale(2);
                computerFee = BigDecimal.ZERO.setScale(2);
                culturalFee = BigDecimal.ZERO.setScale(2);
                devFee = BigDecimal.ZERO.setScale(2);
                admissionFee = BigDecimal.ZERO.setScale(2);
                guidanceFee = BigDecimal.ZERO.setScale(2);
                handbookFee = BigDecimal.ZERO.setScale(2);
                libraryFee = BigDecimal.ZERO.setScale(2);
                medicalFee = BigDecimal.ZERO.setScale(2);
                regFee = BigDecimal.ZERO.setScale(2);
                idFee = BigDecimal.ZERO.setScale(2);
            }

            BigDecimal totalTosf = misc.add(labFee).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalFheAmount = tuitionFee.add(totalTosf).setScale(2, RoundingMode.HALF_UP);
            String remarks = item.getVerificationStatus() != null ? item.getVerificationStatus() : "VERIFIED";

            csv.append(seqNo++).append(",")
                    .append(escapeCsv(studentNumber)).append(",")
                    .append(escapeCsv(lrn)).append(",")
                    .append(escapeCsv(lastName)).append(",")
                    .append(escapeCsv(firstName)).append(",")
                    .append(escapeCsv(middleName)).append(",")
                    .append(escapeCsv(extName)).append(",")
                    .append(escapeCsv(sex)).append(",")
                    .append(escapeCsv(progCode)).append(",")
                    .append(escapeCsv(progName)).append(",")
                    .append(yearLevel).append(",")
                    .append(units.setScale(2, RoundingMode.HALF_UP)).append(",")
                    .append(tuitionFee.setScale(2, RoundingMode.HALF_UP)).append(",")
                    .append(athleticFee).append(",")
                    .append(computerFee).append(",")
                    .append(culturalFee).append(",")
                    .append(devFee).append(",")
                    .append(admissionFee).append(",")
                    .append(guidanceFee).append(",")
                    .append(handbookFee).append(",")
                    .append(labFee.setScale(2, RoundingMode.HALF_UP)).append(",")
                    .append(libraryFee).append(",")
                    .append(medicalFee).append(",")
                    .append(regFee).append(",")
                    .append(idFee).append(",")
                    .append(totalTosf).append(",")
                    .append(totalFheAmount).append(",")
                    .append(escapeCsv(remarks))
                    .append("\n");
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private UnifastFheClaimDto mapToClaimDto(UnifastFheClaim c) {
        List<UnifastFheClaimItemDto> itemDtos = (c.getClaimItems() != null ? c.getClaimItems() : List.<UnifastFheClaimItem>of()).stream()
                .map(this::mapToClaimItemDto)
                .toList();

        return new UnifastFheClaimDto(
                c.getId(),
                c.getClaimBatchNumber(),
                c.getTerm() != null ? c.getTerm().getId() : null,
                c.getTerm() != null ? (c.getTerm().getAcademicYear() != null ? c.getTerm().getAcademicYear().getCode() : "AY") + " " + c.getTerm().getTermType() : "N/A",
                c.getCampus() != null ? c.getCampus().getId() : null,
                c.getCampus() != null ? c.getCampus().getName() : "N/A",
                c.getTotalBeneficiaries(),
                c.getTotalTuitionClaimed(),
                c.getTotalTosfClaimed(),
                c.getTotalClaimAmount(),
                c.getStatus() != null ? c.getStatus().name() : "DRAFT",
                c.getCreatedByUser() != null ? c.getCreatedByUser().getUsername() : "System",
                c.getCreatedAt() != null ? c.getCreatedAt().toString() : Instant.now().toString(),
                itemDtos
        );
    }

    private UnifastFheClaimItemDto mapToClaimItemDto(UnifastFheClaimItem i) {
        StudentProfile sp = i.getStudentProfile();
        String studentName = sp != null ? sp.getFullName() : "Student #" + i.getId();
        String progCode = (sp != null && sp.getProgram() != null) ? sp.getProgram().getCode() : "N/A";

        return new UnifastFheClaimItemDto(
                i.getId(),
                i.getClaimBatch() != null ? i.getClaimBatch().getId() : null,
                sp != null ? sp.getId() : null,
                sp != null ? sp.getStudentNumber() : "N/A",
                studentName,
                progCode,
                i.getEnrolledUnits(),
                i.getTuitionAmount(),
                i.getMiscAmount(),
                i.getLabAmount(),
                i.getTotalClaimedAmount(),
                i.getVerificationStatus()
        );
    }
}

