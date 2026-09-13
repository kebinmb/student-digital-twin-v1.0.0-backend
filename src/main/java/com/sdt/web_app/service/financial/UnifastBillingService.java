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
        String studentName = (sp != null && sp.getUser() != null) ? sp.getUser().getUsername() : "Student #" + (sp != null ? sp.getStudentNumber() : i.getId());
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
