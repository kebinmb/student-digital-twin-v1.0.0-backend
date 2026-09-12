package com.sdt.web_app.service.financial;

import com.sdt.web_app.dto.financial.FinancialDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.enrollment.StudentEnrollment;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.financial.FeeTemplate;
import com.sdt.web_app.entities.financial.StudentAccountLedger;
import com.sdt.web_app.entities.financial.StudentAssessmentInvoice;
import com.sdt.web_app.entities.institution.AcademicYear;
import com.sdt.web_app.entities.institution.Campus;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.StudentEnrollmentRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.financial.FeeTemplateRepository;
import com.sdt.web_app.repositories.financial.StudentAccountLedgerRepository;
import com.sdt.web_app.repositories.financial.StudentAssessmentInvoiceRepository;
import com.sdt.web_app.repositories.institution.AcademicYearRepository;
import com.sdt.web_app.repositories.institution.CampusRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeeAssessmentService {

    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentAssessmentInvoiceRepository invoiceRepository;
    private final StudentAccountLedgerRepository ledgerRepository;
    private final FeeTemplateRepository feeTemplateRepository;
    private final AcademicYearRepository academicYearRepository;
    private final CampusRepository campusRepository;
    private final UserRepository userRepository;

    @Transactional
    public FeeTemplateDto createFeeTemplate(CreateFeeTemplateRequest request, Long actorUserId) {
        AcademicYear ay = academicYearRepository.findById(request.academicYearId())
                .orElseThrow(() -> new EntityNotFoundException("Academic year not found: " + request.academicYearId()));

        Campus campus = null;
        if (request.campusId() != null) {
            campus = campusRepository.findById(request.campusId())
                    .orElseThrow(() -> new EntityNotFoundException("Campus not found: " + request.campusId()));
        }

        FeeTemplate template = FeeTemplate.builder()
                .name(request.name().trim())
                .academicYear(ay)
                .campus(campus)
                .tuitionPerUnit(request.tuitionPerUnit())
                .labFeePerUnit(request.labFeePerUnit())
                .miscellaneousFlatFee(request.miscellaneousFlatFee())
                .athleticFlatFee(request.athleticFlatFee())
                .active(true)
                .build();

        FeeTemplate saved = feeTemplateRepository.save(template);
        log.info("Created fee template ID {} for AY {} by user {}", saved.getId(), ay.getCode(), actorUserId);

        return mapToFeeTemplateDto(saved);
    }

    @Transactional(readOnly = true)
    public FeeTemplateDto getActiveFeeTemplate(Long academicYearId) {
        FeeTemplate template = feeTemplateRepository.findFirstByAcademicYearIdAndActiveTrueOrderByCreatedAtDesc(academicYearId)
                .orElseGet(this::createDefaultFeeTemplate);
        return mapToFeeTemplateDto(template);
    }

    @Transactional
    public StudentAssessmentInvoiceDto assessEnrollment(Long enrollmentId, Long actorUserId) {
        StudentEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException("Student enrollment not found with ID: " + enrollmentId));

        StudentProfile student = enrollment.getStudent();
        Term term = enrollment.getTerm();

        // Check if invoice already exists
        StudentAssessmentInvoice existingInvoice = invoiceRepository.findByStudentEnrollmentId(enrollmentId).orElse(null);

        FeeTemplate template = null;
        if (term.getAcademicYear() != null) {
            template = feeTemplateRepository.findFirstByAcademicYearIdAndActiveTrueOrderByCreatedAtDesc(term.getAcademicYear().getId()).orElse(null);
        }
        if (template == null) {
            template = createDefaultFeeTemplate();
        }

        // Calculate course lecture & lab units
        BigDecimal totalLecUnits = BigDecimal.ZERO;
        BigDecimal totalLabUnits = BigDecimal.ZERO;

        if (enrollment.getItems() != null) {
            for (EnrollmentCourseItem item : enrollment.getItems()) {
                if (item.getSection() != null && item.getSection().getCourse() != null) {
                    BigDecimal lec = item.getSection().getCourse().getLectureUnits();
                    BigDecimal lab = item.getSection().getCourse().getLabUnits();
                    if (lec != null) totalLecUnits = totalLecUnits.add(lec);
                    if (lab != null) totalLabUnits = totalLabUnits.add(lab);
                }
            }
        }

        BigDecimal tuitionFee = totalLecUnits.multiply(template.getTuitionPerUnit()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal labFee = totalLabUnits.multiply(template.getLabFeePerUnit()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal miscFee = template.getMiscellaneousFlatFee().add(template.getAthleticFlatFee()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grossAssessment = tuitionFee.add(labFee).add(miscFee).setScale(2, RoundingMode.HALF_UP);

        // Evaluate RA 10931 FHE Eligibility
        boolean isFheEligible = student.getEnrollmentStatus() == null ||
                student.getEnrollmentStatus() == StudentProfile.EnrollmentStatus.REGULAR;

        BigDecimal fheSubsidy = isFheEligible ? grossAssessment : BigDecimal.ZERO;
        BigDecimal netAssessed = grossAssessment.subtract(fheSubsidy).setScale(2, RoundingMode.HALF_UP);
        StudentAssessmentInvoice.InvoiceStatus status = isFheEligible ? StudentAssessmentInvoice.InvoiceStatus.FHE_COVERED : StudentAssessmentInvoice.InvoiceStatus.UNPAID;

        StudentAssessmentInvoice invoice;
        String yearCode = (term.getAcademicYear() != null && term.getAcademicYear().getCode() != null) ? term.getAcademicYear().getCode() : "AY2026";
        if (existingInvoice != null) {
            invoice = existingInvoice;
            invoice.setTotalTuitionFee(tuitionFee);
            invoice.setTotalLabFee(labFee);
            invoice.setTotalMiscFee(miscFee);
            invoice.setTotalGrossAssessment(grossAssessment);
            invoice.setFheSubsidyAmount(fheSubsidy);
            invoice.setNetAssessedAmount(netAssessed);
            invoice.setOutstandingBalance(netAssessed);
            invoice.setStatus(status);
            invoice.setFheEligible(isFheEligible);
        } else {
            String invoiceNo = "INV-" + yearCode.replaceAll("\\s+", "") + "-" + String.format("%05d", enrollmentId);
            invoice = StudentAssessmentInvoice.builder()
                    .invoiceNumber(invoiceNo)
                    .studentEnrollment(enrollment)
                    .studentProfile(student)
                    .term(term)
                    .totalTuitionFee(tuitionFee)
                    .totalLabFee(labFee)
                    .totalMiscFee(miscFee)
                    .totalGrossAssessment(grossAssessment)
                    .fheSubsidyAmount(fheSubsidy)
                    .netAssessedAmount(netAssessed)
                    .totalPaidAmount(BigDecimal.ZERO)
                    .outstandingBalance(netAssessed)
                    .status(status)
                    .fheEligible(isFheEligible)
                    .build();
        }

        StudentAssessmentInvoice savedInvoice = invoiceRepository.save(invoice);

        // Record Ledger Entries
        User actorUser = actorUserId != null ? userRepository.findById(actorUserId).orElse(null) : null;
        BigDecimal currentBalance = getCurrentLedgerBalance(student.getId());

        // 1. Ledger Charge
        BigDecimal balanceAfterCharge = currentBalance.add(grossAssessment).setScale(2, RoundingMode.HALF_UP);
        StudentAccountLedger chargeLedger = StudentAccountLedger.builder()
                .transactionNumber("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .studentProfile(student)
                .term(term)
                .assessmentInvoice(savedInvoice)
                .transactionType(StudentAccountLedger.TransactionType.CHARGE)
                .description("Tuition & TOSF Assessment (" + yearCode + " " + term.getTermType() + ")")
                .debitAmount(grossAssessment)
                .creditAmount(BigDecimal.ZERO)
                .runningBalance(balanceAfterCharge)
                .referenceNumber(savedInvoice.getInvoiceNumber())
                .createdByUser(actorUser)
                .build();
        ledgerRepository.save(chargeLedger);

        // 2. FHE Subsidy Ledger (if eligible)
        if (isFheEligible && fheSubsidy.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal balanceAfterSubsidy = balanceAfterCharge.subtract(fheSubsidy).setScale(2, RoundingMode.HALF_UP);
            StudentAccountLedger subsidyLedger = StudentAccountLedger.builder()
                    .transactionNumber("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .studentProfile(student)
                    .term(term)
                    .assessmentInvoice(savedInvoice)
                    .transactionType(StudentAccountLedger.TransactionType.FHE_SUBSIDY)
                    .description("RA 10931 Universal Access FHE Subsidy Credit")
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(fheSubsidy)
                    .runningBalance(balanceAfterSubsidy)
                    .referenceNumber(savedInvoice.getInvoiceNumber())
                    .createdByUser(actorUser)
                    .build();
            ledgerRepository.save(subsidyLedger);
        }

        log.info("Assessed tuition for enrollment ID {} student {} invoice {}: gross={}, fhe={}, net={}",
                enrollmentId, student.getStudentNumber(), savedInvoice.getInvoiceNumber(), grossAssessment, fheSubsidy, netAssessed);

        return mapToInvoiceDto(savedInvoice);
    }

    @Transactional(readOnly = true)
    public StudentAssessmentInvoiceDto getInvoiceByEnrollmentId(Long enrollmentId) {
        StudentAssessmentInvoice invoice = invoiceRepository.findByStudentEnrollmentId(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assessment invoice not found for enrollment ID: " + enrollmentId));
        return mapToInvoiceDto(invoice);
    }

    @Transactional(readOnly = true)
    public StudentAssessmentInvoiceDto getInvoiceByStudentAndTerm(Long studentProfileId, Long termId) {
        StudentAssessmentInvoice invoice = invoiceRepository.findByStudentProfileIdAndTermId(studentProfileId, termId)
                .orElseThrow(() -> new EntityNotFoundException("Assessment invoice not found for student " + studentProfileId + " and term " + termId));
        return mapToInvoiceDto(invoice);
    }

    @Transactional(readOnly = true)
    public List<StudentAccountLedgerDto> getStudentLedgerHistory(Long studentProfileId) {
        return ledgerRepository.findByStudentProfileIdOrderByIdAsc(studentProfileId).stream()
                .map(this::mapToLedgerDto)
                .toList();
    }

    private BigDecimal getCurrentLedgerBalance(Long studentProfileId) {
        List<StudentAccountLedger> latest = ledgerRepository.findLatestByStudentProfileId(studentProfileId);
        if (latest.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return latest.get(0).getRunningBalance();
    }

    private FeeTemplate createDefaultFeeTemplate() {
        return FeeTemplate.builder()
                .name("Standard Institutional Fee Template")
                .tuitionPerUnit(new BigDecimal("250.00"))
                .labFeePerUnit(new BigDecimal("500.00"))
                .miscellaneousFlatFee(new BigDecimal("1500.00"))
                .athleticFlatFee(new BigDecimal("300.00"))
                .active(true)
                .build();
    }

    private FeeTemplateDto mapToFeeTemplateDto(FeeTemplate t) {
        return new FeeTemplateDto(
                t.getId(),
                t.getName(),
                t.getAcademicYear() != null ? t.getAcademicYear().getId() : null,
                t.getCampus() != null ? t.getCampus().getId() : null,
                t.getTuitionPerUnit(),
                t.getLabFeePerUnit(),
                t.getMiscellaneousFlatFee(),
                t.getAthleticFlatFee(),
                t.isActive()
        );
    }

    private StudentAssessmentInvoiceDto mapToInvoiceDto(StudentAssessmentInvoice i) {
        StudentProfile sp = i.getStudentProfile();
        String studentName = (sp != null && sp.getUser() != null) ? sp.getUser().getUsername() : "Student #" + (sp != null ? sp.getStudentNumber() : i.getId());

        return new StudentAssessmentInvoiceDto(
                i.getId(),
                i.getInvoiceNumber(),
                i.getStudentEnrollment() != null ? i.getStudentEnrollment().getId() : null,
                sp != null ? sp.getId() : null,
                sp != null ? sp.getStudentNumber() : "N/A",
                studentName,
                i.getTerm() != null ? i.getTerm().getId() : null,
                i.getTerm() != null ? (i.getTerm().getAcademicYear() != null ? i.getTerm().getAcademicYear().getCode() : "AY") + " " + i.getTerm().getTermType() : "N/A",
                i.getTotalTuitionFee(),
                i.getTotalLabFee(),
                i.getTotalMiscFee(),
                i.getTotalGrossAssessment(),
                i.getFheSubsidyAmount(),
                i.getScholarshipDiscountAmount(),
                i.getNetAssessedAmount(),
                i.getTotalPaidAmount(),
                i.getOutstandingBalance(),
                i.getStatus() != null ? i.getStatus().name() : "UNPAID",
                i.isFheEligible()
        );
    }

    private StudentAccountLedgerDto mapToLedgerDto(StudentAccountLedger l) {
        return new StudentAccountLedgerDto(
                l.getId(),
                l.getTransactionNumber(),
                l.getStudentProfile() != null ? l.getStudentProfile().getId() : null,
                l.getTerm() != null ? l.getTerm().getId() : null,
                l.getAssessmentInvoice() != null ? l.getAssessmentInvoice().getId() : null,
                l.getTransactionType() != null ? l.getTransactionType().name() : "CHARGE",
                l.getTransactionDate() != null ? l.getTransactionDate().toString() : Instant.now().toString(),
                l.getDescription(),
                l.getDebitAmount(),
                l.getCreditAmount(),
                l.getRunningBalance(),
                l.getReferenceNumber()
        );
    }
}
