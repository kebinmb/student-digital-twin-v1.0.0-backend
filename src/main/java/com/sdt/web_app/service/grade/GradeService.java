package com.sdt.web_app.service.grade;

import com.sdt.web_app.dto.grade.GradeDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.enrollment.StudentCourseGrade;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.grade.GradeSealingAudit;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.EnrollmentCourseItemRepository;
import com.sdt.web_app.repositories.enrollment.StudentCourseGradeRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.grade.GradeSealingAuditRepository;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import com.sdt.web_app.service.security.AcademicScopeAssertionService;
import com.sdt.web_app.service.security.AcademicScopeContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradeService {

    private final ClassSectionRepository sectionRepository;
    private final EnrollmentCourseItemRepository itemRepository;
    private final StudentCourseGradeRepository gradeRepository;
    private final StudentProfileRepository profileRepository;
    private final AcademicScopeAssertionService academicScopeAssertionService;
    private final GradeSealingAuditRepository sealingAuditRepository;
    private final UserRepository userRepository;
    private final com.sdt.web_app.service.scheduling.SectionEventPublisherService sectionEventPublisherService;
    private final com.sdt.web_app.service.lms.StudentNotificationPublisherService studentNotificationPublisherService;
    private final com.sdt.web_app.service.compliance.ClearanceWorkflowService clearanceWorkflowService;

    @Transactional(readOnly = true)
    public SectionRosterResponse getSectionRoster(Long sectionId) {
        ClassSection section = sectionRepository.findByIdWithSchedules(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with ID: " + sectionId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            academicScopeAssertionService.validateSectionAccess(scope, section);
        }

        List<EnrollmentCourseItem> items = itemRepository.findBySectionIdWithStudentDetails(sectionId);

        List<RosterStudentDto> students = items.stream()
                .filter(item -> item.getCompletionStatus() != EnrollmentCourseItem.CompletionStatus.DROPPED)
                .map(item -> {
                    StudentProfile sp = item.getEnrollment().getStudent();
                    return new RosterStudentDto(
                            item.getId(),
                            sp.getId(),
                            sp.getStudentNumber(),
                            sp.getUser() != null ? sp.getUser().getUsername() : "Student " + sp.getStudentNumber(),
                            sp.getProgram() != null ? sp.getProgram().getCode() : "N/A",
                            sp.getYearLevel(),
                            item.getFinalNumericalGrade(),
                            item.getCompletionStatus().name()
                    );
                })
                .toList();

        String instructorName = section.getPrimaryInstructor() != null
                ? section.getPrimaryInstructor().getUsername()
                : (section.getSchedules().stream()
                        .filter(s -> s.getInstructor() != null)
                        .map(s -> s.getInstructor().getUsername())
                        .findFirst().orElse("Unassigned"));

        Long primaryInstructorId = section.getPrimaryInstructor() != null
                ? section.getPrimaryInstructor().getId()
                : (section.getSchedules().stream()
                        .filter(s -> s.getInstructor() != null)
                        .map(s -> s.getInstructor().getId())
                        .findFirst().orElse(null));

        Long updatedAtEpochMs = section.getUpdatedAt() != null ? section.getUpdatedAt().toEpochMilli() : null;
        return new SectionRosterResponse(
                section.getId(),
                section.getSectionCode(),
                section.getCourse().getId(),
                section.getCourse().getCode(),
                section.getCourse().getTitle(),
                section.getCourse().getCreditUnits(),
                section.getTerm().getId(),
                section.getTerm().getTermType().name(),
                section.getGradeStatus().name(),
                primaryInstructorId,
                instructorName,
                section.getEnrolledCount(),
                section.getMaxCapacity(),
                students,
                updatedAtEpochMs
        );
    }

    @Transactional
    public GradeActionResponse saveGrades(Long sectionId, SaveSectionGradesRequest request, Long actorUserId) {
        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with ID: " + sectionId));

        if (request.expectedUpdatedAtEpochMs() != null && section.getUpdatedAt() != null) {
            long currentUpdated = section.getUpdatedAt().toEpochMilli();
            if (currentUpdated > request.expectedUpdatedAtEpochMs()) {
                throw new org.springframework.dao.OptimisticLockingFailureException(
                        "Section grades were modified concurrently by another user. Please reload the gradebook before saving.");
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            academicScopeAssertionService.validateSectionAccess(scope, section);
        }

        if (section.getGradeStatus() != ClassSection.GradeStatus.DRAFT) {
            throw new IllegalStateException("Cannot update grades: section " + section.getSectionCode() + " is currently in " + section.getGradeStatus() + " status.");
        }

        int updatedCount = 0;
        if (request.grades() != null) {
            for (GradeEntryDto entry : request.grades()) {
                EnrollmentCourseItem item = itemRepository.findById(entry.enrollmentItemId())
                        .orElseThrow(() -> new EntityNotFoundException("Enrollment item not found: " + entry.enrollmentItemId()));

                if (!item.getSection().getId().equals(sectionId)) {
                    throw new IllegalArgumentException("Item " + entry.enrollmentItemId() + " does not belong to section " + sectionId);
                }

                EnrollmentCourseItem.CompletionStatus status = EnrollmentCourseItem.CompletionStatus.ENROLLED;
                if (entry.completionStatus() != null && !entry.completionStatus().isBlank()) {
                    try {
                        status = EnrollmentCourseItem.CompletionStatus.valueOf(entry.completionStatus().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown completion status '{}' for enrollment item {}, defaulting", entry.completionStatus(), entry.enrollmentItemId());
                        status = EnrollmentCourseItem.CompletionStatus.ENROLLED;
                    }
                } else if (entry.finalNumericalGrade() != null) {
                    if (entry.finalNumericalGrade().compareTo(new BigDecimal("3.00")) <= 0) {
                        status = EnrollmentCourseItem.CompletionStatus.PASSED;
                    } else if (entry.finalNumericalGrade().compareTo(new BigDecimal("4.00")) == 0) {
                        status = EnrollmentCourseItem.CompletionStatus.INCOMPLETE;
                    } else {
                        status = EnrollmentCourseItem.CompletionStatus.FAILED;
                    }
                }

                item.updateGrade(entry.finalNumericalGrade(), status);
                itemRepository.save(item);
                updatedCount++;
            }
        }

        if (request.submitForVerification()) {
            List<EnrollmentCourseItem> currentItems = itemRepository.findBySectionIdWithStudentDetails(sectionId);
            long incompleteCount = currentItems.stream()
                    .filter(item -> item.getCompletionStatus() != EnrollmentCourseItem.CompletionStatus.DROPPED)
                    .filter(item -> item.getFinalNumericalGrade() == null 
                            || item.getCompletionStatus() == EnrollmentCourseItem.CompletionStatus.ENROLLED 
                            || item.getCompletionStatus() == EnrollmentCourseItem.CompletionStatus.IN_PROGRESS)
                    .count();

            if (incompleteCount > 0) {
                throw new IllegalStateException("Cannot submit section grades: " + incompleteCount + " student(s) do not have final numerical grades assigned.");
            }

            section.updateGradeStatus(ClassSection.GradeStatus.SUBMITTED);
            sectionRepository.save(section);
            if (sectionEventPublisherService != null) {
                sectionEventPublisherService.publishGradeStatusEvent(
                        section.getTerm() != null ? section.getTerm().getId() : null,
                        section.getId(),
                        section.getSectionCode(),
                        "SUBMITTED"
                );
            }
            log.info("Section {} grades submitted for verification by user {}", section.getSectionCode(), actorUserId);
            return new GradeActionResponse(
                    section.getId(),
                    section.getSectionCode(),
                    section.getGradeStatus().name(),
                    updatedCount,
                    "Grades successfully submitted for Dean/Chairperson verification."
            );
        }

        section.preUpdate();
        sectionRepository.save(section);

        return new GradeActionResponse(
                section.getId(),
                section.getSectionCode(),
                section.getGradeStatus().name(),
                updatedCount,
                "Grades successfully saved as draft."
        );
    }

    @Transactional
    public GradeActionResponse verifyGrades(Long sectionId, Long approverUserId) {
        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with ID: " + sectionId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            academicScopeAssertionService.validateSectionAccess(scope, section);
        }

        if (section.getGradeStatus() != ClassSection.GradeStatus.SUBMITTED) {
            throw new IllegalStateException("Cannot verify grades: section must be in SUBMITTED status. Current status: " + section.getGradeStatus());
        }

        section.updateGradeStatus(ClassSection.GradeStatus.VERIFIED);
        sectionRepository.save(section);
        if (sectionEventPublisherService != null) {
            sectionEventPublisherService.publishGradeStatusEvent(
                    section.getTerm() != null ? section.getTerm().getId() : null,
                    section.getId(),
                    section.getSectionCode(),
                    "VERIFIED"
            );
        }
        log.info("Section {} grades verified by user {}", section.getSectionCode(), approverUserId);

        return new GradeActionResponse(
                section.getId(),
                section.getSectionCode(),
                section.getGradeStatus().name(),
                0,
                "Grades verified and endorsed for Registrar sealing."
        );
    }

    @Transactional
    public GradeActionResponse rejectGrades(Long sectionId, String reason, Long approverUserId) {
        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with ID: " + sectionId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            academicScopeAssertionService.validateSectionAccess(scope, section);
        }

        if (section.getGradeStatus() != ClassSection.GradeStatus.SUBMITTED) {
            throw new IllegalStateException("Cannot reject grades: section must be in SUBMITTED status. Current status: " + section.getGradeStatus());
        }

        section.updateGradeStatus(ClassSection.GradeStatus.DRAFT);
        sectionRepository.save(section);
        if (sectionEventPublisherService != null) {
            sectionEventPublisherService.publishGradeStatusEvent(
                    section.getTerm() != null ? section.getTerm().getId() : null,
                    section.getId(),
                    section.getSectionCode(),
                    "DRAFT"
            );
        }
        log.warn("Section {} grades rejected and returned to DRAFT by user {}. Reason: {}", section.getSectionCode(), approverUserId, reason);

        return new GradeActionResponse(
                section.getId(),
                section.getSectionCode(),
                section.getGradeStatus().name(),
                0,
                "Grades rejected by Dean/Chairperson and returned to Faculty for revision."
        );
    }

    @Transactional
    public GradeActionResponse sealGrades(Long sectionId, Long registrarUserId) {
        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with ID: " + sectionId));

        if (section.getGradeStatus() != ClassSection.GradeStatus.VERIFIED) {
            throw new IllegalStateException("Cannot seal grades: section must be in VERIFIED status before sealing. Current status: " + section.getGradeStatus());
        }

        List<EnrollmentCourseItem> items = itemRepository.findBySectionIdWithStudentDetails(sectionId);
        Set<StudentProfile> affectedStudents = new HashSet<>();
        Long termId = section.getTerm() != null ? section.getTerm().getId() : null;

        for (EnrollmentCourseItem item : items) {
            if (item.getCompletionStatus() == EnrollmentCourseItem.CompletionStatus.DROPPED) {
                continue;
            }

            StudentProfile sp = item.getEnrollment().getStudent();
            if (item.getFinalNumericalGrade() != null) {
                StudentCourseGrade historicalGrade = (termId != null)
                        ? gradeRepository.findByStudentIdAndCourseIdAndTermId(sp.getId(), section.getCourse().getId(), termId)
                                .orElseGet(() -> StudentCourseGrade.builder()
                                        .student(sp)
                                        .course(section.getCourse())
                                        .term(section.getTerm())
                                        .numericalGrade(item.getFinalNumericalGrade())
                                        .completionStatus(item.getCompletionStatus().name())
                                        .isCredited(false)
                                        .build())
                        : gradeRepository.findByStudentIdAndCourseId(sp.getId(), section.getCourse().getId())
                                .orElseGet(() -> StudentCourseGrade.builder()
                                        .student(sp)
                                        .course(section.getCourse())
                                        .term(section.getTerm())
                                        .numericalGrade(item.getFinalNumericalGrade())
                                        .completionStatus(item.getCompletionStatus().name())
                                        .isCredited(false)
                                        .build());

                historicalGrade.updateGrade(item.getFinalNumericalGrade(), item.getCompletionStatus().name());
                gradeRepository.save(historicalGrade);
                affectedStudents.add(sp);

                if (studentNotificationPublisherService != null) {
                    studentNotificationPublisherService.publishGradeReleasedEvent(
                            sp.getId(),
                            section.getId(),
                            section.getCourse() != null ? section.getCourse().getCode() : "",
                            section.getCourse() != null ? section.getCourse().getTitle() : "",
                            item.getFinalNumericalGrade().doubleValue(),
                            "SEALED"
                    );
                }
            }
        }

        // Recalculate units and GPA for each affected student
        for (StudentProfile sp : affectedStudents) {
            List<StudentCourseGrade> passedGrades = gradeRepository.findPassedGradesByStudentId(sp.getId());
            BigDecimal totalUnits = BigDecimal.ZERO;
            BigDecimal weightedGradeSum = BigDecimal.ZERO;

            for (StudentCourseGrade g : passedGrades) {
                BigDecimal units = g.getCourse().getCreditUnits();
                totalUnits = totalUnits.add(units);
                weightedGradeSum = weightedGradeSum.add(g.getNumericalGrade().multiply(units));
            }

            BigDecimal gpa = totalUnits.compareTo(BigDecimal.ZERO) > 0
                    ? weightedGradeSum.divide(totalUnits, 2, RoundingMode.HALF_UP)
                    : null;

            sp.updateProgress(totalUnits, gpa);
            profileRepository.save(sp);

            if (studentNotificationPublisherService != null) {
                studentNotificationPublisherService.publishStandingUpdatedEvent(
                        sp.getId(),
                        gpa != null ? gpa.doubleValue() : null,
                        totalUnits,
                        sp.getEnrollmentStatus() != null ? sp.getEnrollmentStatus().name() : "REGULAR"
                );
            }

            if (clearanceWorkflowService != null && termId != null) {
                try {
                    clearanceWorkflowService.cascadeGradeSealingToClearance(sp.getId(), termId);
                } catch (Exception ex) {
                    log.warn("Failed to cascade clearance for student {}: {}", sp.getId(), ex.getMessage());
                }
            }
        }

        section.updateGradeStatus(ClassSection.GradeStatus.SEALED);
        sectionRepository.save(section);
        if (sectionEventPublisherService != null) {
            sectionEventPublisherService.publishGradeStatusEvent(
                    section.getTerm() != null ? section.getTerm().getId() : null,
                    section.getId(),
                    section.getSectionCode(),
                    "SEALED"
            );
        }

        // Record Sealing Audit Ledger Entry
        if (userRepository != null && sealingAuditRepository != null) {
            User registrarUser = userRepository.findById(registrarUserId).orElse(null);
            if (registrarUser != null) {
                String hashSeed = section.getSectionCode() + ":" + registrarUserId + ":" + affectedStudents.size() + ":" + System.currentTimeMillis();
                String checksumHash = computeSha256(hashSeed);

                GradeSealingAudit audit = GradeSealingAudit.builder()
                        .section(section)
                        .registrarUser(registrarUser)
                        .studentRecordsSealed(affectedStudents.size())
                        .sectionCode(section.getSectionCode())
                        .courseCode(section.getCourse().getCode())
                        .checksumHash(checksumHash)
                        .build();
                sealingAuditRepository.save(audit);
            }
        }

        log.info("Section {} grades permanently SEALED into academic transcripts by registrar {}", section.getSectionCode(), registrarUserId);

        return new GradeActionResponse(
                section.getId(),
                section.getSectionCode(),
                section.getGradeStatus().name(),
                items.size(),
                "Grades officially sealed into permanent transcripts and prerequisite records updated."
        );
    }

    @Transactional
    public List<GradeActionResponse> batchVerifyGrades(List<Long> sectionIds, Long approverUserId) {
        if (sectionIds == null || sectionIds.isEmpty()) return List.of();
        return sectionIds.stream()
                .map(id -> {
                    try {
                        return verifyGrades(id, approverUserId);
                    } catch (Exception e) {
                        log.warn("Batch verify failed for section {}: {}", id, e.getMessage());
                        return new GradeActionResponse(id, "SECTION-" + id, "ERROR", 0, "Batch verify error: " + e.getMessage());
                    }
                })
                .toList();
    }

    @Transactional
    public List<GradeActionResponse> batchSealGrades(List<Long> sectionIds, Long registrarUserId) {
        if (sectionIds == null || sectionIds.isEmpty()) return List.of();
        return sectionIds.stream()
                .map(id -> {
                    try {
                        return sealGrades(id, registrarUserId);
                    } catch (Exception e) {
                        log.warn("Batch seal failed for section {}: {}", id, e.getMessage());
                        return new GradeActionResponse(id, "SECTION-" + id, "ERROR", 0, "Batch seal error: " + e.getMessage());
                    }
                })
                .toList();
    }

    private String computeSha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            return "HASH_ERROR";
        }
    }
}
