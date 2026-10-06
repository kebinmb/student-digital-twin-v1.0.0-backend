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
import java.util.Comparator;
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
    private final com.sdt.web_app.service.webhook.InstitutionalWebhookService institutionalWebhookService;

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

        // Evaluate RA 10931 FHE Eligibility & MRR Guard
        int yearsToComplete = (student.getProgram() != null && "GRADUATE".equalsIgnoreCase(student.getProgram().getDegreeLevel())) ? 2 : 4;
        int maxAllowedSemesters = 2 * (yearsToComplete + 1);
        long completedTermsCount = enrollmentRepository.findByStudentId(student.getId()).size();

        boolean isMrrExceeded = completedTermsCount >= maxAllowedSemesters;
        boolean isRegular = student.getEnrollmentStatus() == null ||
                student.getEnrollmentStatus() == StudentProfile.EnrollmentStatus.REGULAR;
        boolean isFheEligible = isRegular && !isMrrExceeded;

        if (isMrrExceeded) {
            log.info("Student {} exceeded Maximum Residency Rule (MRR) cap ({} enrolled terms vs {} max allowed). Transitioning to self-paying status.",
                    student.getStudentNumber(), completedTermsCount, maxAllowedSemesters);
        }

        // Evaluate Prior Term Academic Honor Roll Scholarship Discount
        String honorCategory = evaluatePriorTermHonorCategory(student.getId(), term.getId());
        BigDecimal scholarshipDiscount = BigDecimal.ZERO;
        String discountDescription = null;

        if ("PRESIDENTS_LIST".equals(honorCategory)) {
            scholarshipDiscount = tuitionFee; // 100% of tuition fee
            discountDescription = "President's List Academic Scholarship (100% Tuition Discount)";
        } else if ("DEANS_LIST".equals(honorCategory)) {
            scholarshipDiscount = tuitionFee.multiply(new BigDecimal("0.50")).setScale(2, RoundingMode.HALF_UP); // 50% of tuition fee
            discountDescription = "Dean's List Academic Scholarship (50% Tuition Discount)";
        }

        BigDecimal grossAfterDiscount = grossAssessment.subtract(scholarshipDiscount).max(BigDecimal.ZERO);
        BigDecimal fheSubsidy = isFheEligible ? grossAfterDiscount : BigDecimal.ZERO;
        BigDecimal netAssessed = grossAfterDiscount.subtract(fheSubsidy).setScale(2, RoundingMode.HALF_UP);

        StudentAssessmentInvoice.InvoiceStatus status;
        if (isFheEligible) {
            status = StudentAssessmentInvoice.InvoiceStatus.FHE_COVERED;
        } else if (netAssessed.compareTo(BigDecimal.ZERO) == 0) {
            status = StudentAssessmentInvoice.InvoiceStatus.PAID;
        } else {
            status = StudentAssessmentInvoice.InvoiceStatus.UNPAID;
        }

        StudentAssessmentInvoice invoice;
        String yearCode = (term.getAcademicYear() != null && term.getAcademicYear().getCode() != null) ? term.getAcademicYear().getCode() : "AY2026";
        if (existingInvoice != null) {
            invoice = existingInvoice;
            invoice.setTotalTuitionFee(tuitionFee);
            invoice.setTotalLabFee(labFee);
            invoice.setTotalMiscFee(miscFee);
            invoice.setTotalGrossAssessment(grossAssessment);
            invoice.setScholarshipDiscountAmount(scholarshipDiscount);
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
                    .scholarshipDiscountAmount(scholarshipDiscount)
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

        // 2. Academic Honor Scholarship Ledger (if applicable)
        BigDecimal balanceAfterDiscount = balanceAfterCharge;
        if (scholarshipDiscount.compareTo(BigDecimal.ZERO) > 0) {
            balanceAfterDiscount = balanceAfterCharge.subtract(scholarshipDiscount).setScale(2, RoundingMode.HALF_UP);
            StudentAccountLedger discountLedger = StudentAccountLedger.builder()
                    .transactionNumber("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .studentProfile(student)
                    .term(term)
                    .assessmentInvoice(savedInvoice)
                    .transactionType(StudentAccountLedger.TransactionType.DISCOUNT)
                    .description(discountDescription)
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(scholarshipDiscount)
                    .runningBalance(balanceAfterDiscount)
                    .referenceNumber("SCHOLARSHIP-" + savedInvoice.getInvoiceNumber())
                    .createdByUser(actorUser)
                    .build();
            ledgerRepository.save(discountLedger);

            if (institutionalWebhookService != null) {
                institutionalWebhookService.dispatchEvent(
                        "TUITION_DISCOUNT_APPLIED",
                        java.util.Map.of(
                                "studentId", student.getId(),
                                "studentNumber", student.getStudentNumber(),
                                "invoiceNumber", savedInvoice.getInvoiceNumber(),
                                "discountAmount", scholarshipDiscount,
                                "honorCategory", honorCategory != null ? honorCategory : "N/A"
                        )
                );
            }
        }

        // 3. FHE Subsidy Ledger (if eligible)
        if (isFheEligible && fheSubsidy.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal balanceAfterSubsidy = balanceAfterDiscount.subtract(fheSubsidy).setScale(2, RoundingMode.HALF_UP);
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

        log.info("Assessed tuition for enrollment ID {} student {} invoice {}: gross={}, scholarshipDiscount={}, fhe={}, net={}",
                enrollmentId, student.getStudentNumber(), savedInvoice.getInvoiceNumber(), grossAssessment, scholarshipDiscount, fheSubsidy, netAssessed);

        return mapToInvoiceDto(savedInvoice);
    }

    @Transactional
    public StudentAssessmentInvoiceDto adjustAssessmentForAddDrop(Long enrollmentId, Long actorUserId) {
        StudentEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException("Student enrollment not found with ID: " + enrollmentId));

        StudentAssessmentInvoice invoice = invoiceRepository.findByStudentEnrollmentId(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException("Assessment invoice not found for enrollment ID: " + enrollmentId));

        StudentProfile student = enrollment.getStudent();
        Term term = enrollment.getTerm();

        FeeTemplate template = null;
        if (term.getAcademicYear() != null) {
            template = feeTemplateRepository.findFirstByAcademicYearIdAndActiveTrueOrderByCreatedAtDesc(term.getAcademicYear().getId()).orElse(null);
        }
        if (template == null) {
            template = createDefaultFeeTemplate();
        }

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

        BigDecimal newTuitionFee = totalLecUnits.multiply(template.getTuitionPerUnit()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newLabFee = totalLabUnits.multiply(template.getLabFeePerUnit()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newMiscFee = template.getMiscellaneousFlatFee().add(template.getAthleticFlatFee()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newGrossAssessment = newTuitionFee.add(newLabFee).add(newMiscFee).setScale(2, RoundingMode.HALF_UP);

        BigDecimal oldGrossAssessment = invoice.getTotalGrossAssessment();
        BigDecimal difference = newGrossAssessment.subtract(oldGrossAssessment).setScale(2, RoundingMode.HALF_UP);

        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            User actorUser = actorUserId != null ? userRepository.findById(actorUserId).orElse(null) : null;
            BigDecimal currentBalance = getCurrentLedgerBalance(student.getId());
            BigDecimal newBalance = currentBalance.add(difference).setScale(2, RoundingMode.HALF_UP);

            StudentAccountLedger.TransactionType txnType = difference.compareTo(BigDecimal.ZERO) > 0 
                    ? StudentAccountLedger.TransactionType.CHARGE 
                    : StudentAccountLedger.TransactionType.ADJUSTMENT;

            BigDecimal debit = difference.compareTo(BigDecimal.ZERO) > 0 ? difference : BigDecimal.ZERO;
            BigDecimal credit = difference.compareTo(BigDecimal.ZERO) < 0 ? difference.abs() : BigDecimal.ZERO;

            StudentAccountLedger adjustmentLedger = StudentAccountLedger.builder()
                    .transactionNumber("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .studentProfile(student)
                    .term(term)
                    .assessmentInvoice(invoice)
                    .transactionType(txnType)
                    .description("Add/Drop Course Assessment Adjustment (" + (difference.compareTo(BigDecimal.ZERO) > 0 ? "+" : "") + difference + ")")
                    .debitAmount(debit)
                    .creditAmount(credit)
                    .runningBalance(newBalance)
                    .referenceNumber("ADJUST-" + invoice.getInvoiceNumber())
                    .createdByUser(actorUser)
                    .build();
            ledgerRepository.save(adjustmentLedger);

            invoice.setTotalTuitionFee(newTuitionFee);
            invoice.setTotalLabFee(newLabFee);
            invoice.setTotalMiscFee(newMiscFee);
            invoice.setTotalGrossAssessment(newGrossAssessment);

            if (invoice.isFheEligible()) {
                invoice.setFheSubsidyAmount(newGrossAssessment);
                invoice.setNetAssessedAmount(BigDecimal.ZERO);
                invoice.setOutstandingBalance(BigDecimal.ZERO);
            } else {
                BigDecimal newNet = newGrossAssessment.subtract(invoice.getTotalPaidAmount()).setScale(2, RoundingMode.HALF_UP);
                invoice.setNetAssessedAmount(newGrossAssessment);
                invoice.setOutstandingBalance(newNet.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : newNet);
            }
            invoiceRepository.save(invoice);
        }

        return mapToInvoiceDto(invoice);
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

    public String evaluatePriorTermHonorCategory(Long studentId, Long currentTermId) {
        if (studentId == null) return null;
        List<StudentEnrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        if (enrollments == null || enrollments.isEmpty()) return null;

        // Prior enrollments in descending order of ID
        List<StudentEnrollment> priorEnrollments = enrollments.stream()
                .filter(e -> e.getTerm() != null && (currentTermId == null || !e.getTerm().getId().equals(currentTermId)))
                .filter(e -> e.getItems() != null && !e.getItems().isEmpty())
                .sorted(Comparator.comparing(StudentEnrollment::getId).reversed())
                .toList();

        if (priorEnrollments.isEmpty()) return null;

        StudentEnrollment prior = priorEnrollments.get(0);
        BigDecimal totalUnits = BigDecimal.ZERO;
        BigDecimal weightedGradeSum = BigDecimal.ZERO;
        boolean hasFailingOrIncomplete = false;
        double maxGrade = 1.00;

        for (EnrollmentCourseItem item : prior.getItems()) {
            if (item.getCompletionStatus() == EnrollmentCourseItem.CompletionStatus.DROPPED) {
                continue;
            }
            if (item.getFinalNumericalGrade() == null) {
                hasFailingOrIncomplete = true;
                break;
            }
            double gradeVal = item.getFinalNumericalGrade().doubleValue();
            if (gradeVal > 3.00 || item.getCompletionStatus() == EnrollmentCourseItem.CompletionStatus.FAILED) {
                hasFailingOrIncomplete = true;
                break;
            }
            if (gradeVal > maxGrade) {
                maxGrade = gradeVal;
            }
            BigDecimal units = (item.getSection() != null && item.getSection().getCourse() != null && item.getSection().getCourse().getCreditUnits() != null)
                    ? item.getSection().getCourse().getCreditUnits()
                    : BigDecimal.valueOf(3.0);
            totalUnits = totalUnits.add(units);
            weightedGradeSum = weightedGradeSum.add(item.getFinalNumericalGrade().multiply(units));
        }

        // Philippine academic honors regular load minimum: at least 15 units, no INC or failing
        if (hasFailingOrIncomplete || totalUnits.compareTo(BigDecimal.valueOf(15.0)) < 0) {
            return null;
        }

        BigDecimal gpa = weightedGradeSum.divide(totalUnits, 2, RoundingMode.HALF_UP);
        double gpaVal = gpa.doubleValue();

        if (gpaVal <= 1.25 && maxGrade <= 1.50) {
            return "PRESIDENTS_LIST";
        } else if (gpaVal <= 1.75 && maxGrade <= 2.00) {
            return "DEANS_LIST";
        }
        return null;
    }
}
