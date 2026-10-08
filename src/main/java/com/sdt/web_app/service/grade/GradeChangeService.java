package com.sdt.web_app.service.grade;

import com.sdt.web_app.dto.grade.GradeChangeDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.enrollment.GradeChangeRequest;
import com.sdt.web_app.entities.enrollment.StudentCourseGrade;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.grade.GradeSealingAudit;
import com.sdt.web_app.entities.institution.Course;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.EnrollmentCourseItemRepository;
import com.sdt.web_app.repositories.enrollment.GradeChangeRequestRepository;
import com.sdt.web_app.repositories.enrollment.StudentCourseGradeRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.grade.GradeSealingAuditRepository;
import com.sdt.web_app.repositories.institution.CourseRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.service.security.AcademicScopeAssertionService;
import com.sdt.web_app.service.security.AcademicScopeContext;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sdt.web_app.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GradeChangeService {

    private final GradeChangeRequestRepository requestRepository;
    private final StudentProfileRepository profileRepository;
    private final CourseRepository courseRepository;
    private final TermRepository termRepository;
    private final com.sdt.web_app.service.institution.TermService termService;
    private final UserRepository userRepository;
    private final StudentCourseGradeRepository gradeRepository;
    private final EnrollmentCourseItemRepository itemRepository;
    private final GradeSealingAuditRepository sealingAuditRepository;
    private final AcademicScopeAssertionService academicScopeAssertionService;
    private final com.sdt.web_app.config.WebSocketBroadcastService broadcastService;

    @Transactional
    public GradeChangeResponse submitRequest(CreateGradeChangeRequest request, Long requestedByUserId) {
        StudentProfile student = profileRepository.findById(request.studentId())
                .orElseThrow(() -> new EntityNotFoundException("Student not found: " + request.studentId()));

        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new EntityNotFoundException("Course not found: " + request.courseId()));

        Term term = termService.getTermById(request.termId());

        User requester = userRepository.findById(requestedByUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + requestedByUserId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            academicScopeAssertionService.validateStudentAccess(scope, student);
        }

        GradeChangeRequest entity = GradeChangeRequest.builder()
                .student(student)
                .course(course)
                .term(term)
                .previousGrade(request.previousGrade())
                .newGrade(request.newGrade())
                .reason(request.reason())
                .status(GradeChangeRequest.Status.PENDING)
                .requestedBy(requester)
                .build();

        GradeChangeRequest saved = requestRepository.save(entity);
        log.info("Grade change request #{} submitted by user {} for student {}", saved.getId(), requestedByUserId, student.getStudentNumber());
        if (broadcastService != null) {
            broadcastService.broadcast(com.sdt.web_app.config.WebSocketTopics.ADMIN_GRADES, new com.sdt.web_app.websocket.dto.GradeUpdateMessage(
                    student.getId(),
                    null,
                    course.getCode(),
                    null,
                    request.newGrade() != null ? request.newGrade().doubleValue() : null,
                    request.newGrade() != null ? request.newGrade().doubleValue() : null,
                    "CHANGE_REQUESTED",
                    java.time.Instant.now()
            ));
        }
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<GradeChangeResponse> getPendingRequests() {
        return requestRepository.findByStatusWithDetails(GradeChangeRequest.Status.PENDING).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_EQUITY_PROFILES, allEntries = true)
    public GradeChangeResponse approveRequest(Long requestId, Long approvedByUserId) {
        GradeChangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Grade change request not found: " + requestId));

        if (request.getStatus() != GradeChangeRequest.Status.PENDING) {
            throw new IllegalStateException("Grade change request " + requestId + " is not in PENDING state.");
        }

        User approver = userRepository.findById(approvedByUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + approvedByUserId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            academicScopeAssertionService.validateStudentAccess(scope, request.getStudent());
        }

        request.approve(approver);

        // Update permanent grade record
        StudentProfile student = request.getStudent();
        Course course = request.getCourse();
        String completionStatus = request.getNewGrade().compareTo(new BigDecimal("3.00")) <= 0 ? "PASSED"
                : (request.getNewGrade().compareTo(new BigDecimal("4.00")) == 0 ? "INCOMPLETE" : "FAILED");

        Long termId = request.getTerm() != null ? request.getTerm().getId() : null;
        StudentCourseGrade historicalGrade = (termId != null)
                ? gradeRepository.findByStudentIdAndCourseIdAndTermId(student.getId(), course.getId(), termId)
                        .orElseGet(() -> StudentCourseGrade.builder()
                                .student(student)
                                .course(course)
                                .term(request.getTerm())
                                .numericalGrade(request.getNewGrade())
                                .completionStatus(completionStatus)
                                .isCredited(true)
                                .build())
                : gradeRepository.findByStudentIdAndCourseId(student.getId(), course.getId())
                        .orElseGet(() -> StudentCourseGrade.builder()
                                .student(student)
                                .course(course)
                                .term(request.getTerm())
                                .numericalGrade(request.getNewGrade())
                                .completionStatus(completionStatus)
                                .isCredited(true)
                                .build());

        historicalGrade.updateGrade(request.getNewGrade(), completionStatus);
        gradeRepository.save(historicalGrade);

        // Update class section roster item (EnrollmentCourseItem) if present
        ClassSection auditSection = null;
        if (itemRepository != null) {
            List<EnrollmentCourseItem> rosterItems = itemRepository.findByStudentIdAndCourseId(student.getId(), course.getId());
            EnrollmentCourseItem.CompletionStatus itemStatus;
            if (request.getNewGrade().compareTo(new BigDecimal("3.00")) <= 0) {
                itemStatus = EnrollmentCourseItem.CompletionStatus.PASSED;
            } else if (request.getNewGrade().compareTo(new BigDecimal("4.00")) == 0) {
                itemStatus = EnrollmentCourseItem.CompletionStatus.INCOMPLETE;
            } else {
                itemStatus = EnrollmentCourseItem.CompletionStatus.FAILED;
            }

            for (EnrollmentCourseItem item : rosterItems) {
                if (termId == null || (item.getSection() != null && item.getSection().getTerm() != null && item.getSection().getTerm().getId().equals(termId))) {
                    item.updateGrade(request.getNewGrade(), itemStatus);
                    itemRepository.save(item);
                    if (item.getSection() != null) {
                        auditSection = item.getSection();
                    }
                }
            }
        }

        // Record Sealing Audit Ledger Correction Entry
        if (sealingAuditRepository != null && auditSection != null) {
            String hashSeed = "POST_SEAL_CORRECTION:" + auditSection.getSectionCode() + ":" + requestId + ":" + approvedByUserId + ":" + System.currentTimeMillis();
            String checksumHash = computeSha256(hashSeed);

            GradeSealingAudit audit = GradeSealingAudit.builder()
                    .section(auditSection)
                    .registrarUser(approver)
                    .studentRecordsSealed(1)
                    .sectionCode(auditSection.getSectionCode())
                    .courseCode(course.getCode())
                    .checksumHash(checksumHash)
                    .build();
            sealingAuditRepository.save(audit);
        }

        // Recalculate student GPA
        List<StudentCourseGrade> passedGrades = gradeRepository.findPassedGradesByStudentId(student.getId());
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

        student.updateProgress(totalUnits, gpa);
        profileRepository.save(student);

        GradeChangeRequest saved = requestRepository.save(request);
        log.info("Grade change request #{} APPROVED by user {}. Updated grade to {}", requestId, approvedByUserId, request.getNewGrade());
        if (broadcastService != null) {
            Long secId = auditSection != null ? auditSection.getId() : null;
            com.sdt.web_app.websocket.dto.GradeUpdateMessage msg = new com.sdt.web_app.websocket.dto.GradeUpdateMessage(
                    student.getId(),
                    secId,
                    course.getCode(),
                    null,
                    request.getNewGrade() != null ? request.getNewGrade().doubleValue() : null,
                    request.getNewGrade() != null ? request.getNewGrade().doubleValue() : null,
                    completionStatus,
                    java.time.Instant.now()
            );
            broadcastService.broadcast(com.sdt.web_app.config.WebSocketTopics.grades(student.getId()), msg);
            broadcastService.broadcast(com.sdt.web_app.config.WebSocketTopics.ADMIN_GRADES, msg);
        }
        return mapToResponse(saved);
    }

    @Transactional
    public GradeChangeResponse rejectRequest(Long requestId, Long rejectedByUserId) {
        GradeChangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Grade change request not found: " + requestId));

        if (request.getStatus() != GradeChangeRequest.Status.PENDING) {
            throw new IllegalStateException("Grade change request " + requestId + " is not in PENDING state.");
        }

        User rejector = userRepository.findById(rejectedByUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + rejectedByUserId));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            academicScopeAssertionService.validateStudentAccess(scope, request.getStudent());
        }

        request.reject(rejector);
        GradeChangeRequest saved = requestRepository.save(request);
        log.info("Grade change request #{} REJECTED by user {}", requestId, rejectedByUserId);
        if (broadcastService != null) {
            broadcastService.broadcast(com.sdt.web_app.config.WebSocketTopics.ADMIN_GRADES, new com.sdt.web_app.websocket.dto.GradeUpdateMessage(
                    request.getStudent().getId(),
                    null,
                    request.getCourse().getCode(),
                    null,
                    null,
                    null,
                    "CHANGE_REJECTED",
                    java.time.Instant.now()
            ));
        }
        return mapToResponse(saved);
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

    private GradeChangeResponse mapToResponse(GradeChangeRequest gcr) {
        String studentName = gcr.getStudent().getUser() != null ? gcr.getStudent().getUser().getUsername() : "Student #" + gcr.getStudent().getStudentNumber();
        return new GradeChangeResponse(
                gcr.getId(),
                gcr.getStudent().getId(),
                gcr.getStudent().getStudentNumber(),
                studentName,
                gcr.getCourse().getId(),
                gcr.getCourse().getCode(),
                gcr.getCourse().getTitle(),
                gcr.getTerm().getId(),
                gcr.getTerm().getTermType().name(),
                gcr.getPreviousGrade(),
                gcr.getNewGrade(),
                gcr.getReason(),
                gcr.getStatus().name(),
                gcr.getRequestedBy().getUsername(),
                gcr.getApprovedBy() != null ? gcr.getApprovedBy().getUsername() : null,
                gcr.getCreatedAt()
        );
    }
}

