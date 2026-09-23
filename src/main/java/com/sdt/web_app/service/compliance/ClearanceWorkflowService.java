package com.sdt.web_app.service.compliance;

import com.sdt.web_app.dto.compliance.ComplianceDtos.*;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.compliance.ClearanceRequest;
import com.sdt.web_app.entities.compliance.ClearanceSignoff;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.exceptions.ResourceNotFoundException;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.compliance.ClearanceRequestRepository;
import com.sdt.web_app.repositories.compliance.ClearanceSignoffRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import lombok.RequiredArgsConstructor;
import com.sdt.web_app.config.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClearanceWorkflowService {

    private final ClearanceRequestRepository clearanceRequestRepository;
    private final ClearanceSignoffRepository clearanceSignoffRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TermRepository termRepository;
    private final com.sdt.web_app.service.institution.TermService termService;
    private final UserRepository userRepository;

    private static final List<String> REQUIRED_DEPARTMENTS = List.of(
            "LIBRARY", "ACCOUNTING", "LABORATORY", "STUDENT_AFFAIRS", "DEAN"
    );

    @Transactional
    public ClearanceRequestDto initiateClearanceRequest(InitiateClearanceRequest request, Long actorUserId) {
        StudentProfile student = resolveStudentProfile(request.studentNumber(), request.studentProfileId());

        Term term = termService.getTermById(request.termId());

        clearanceRequestRepository.findByStudentProfileIdAndTermId(student.getId(), term.getId())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Clearance request already exists for this student and term.");
                });

        ClearanceRequest clearanceRequest = ClearanceRequest.builder()
                .studentProfile(student)
                .term(term)
                .purpose(request.purpose().toUpperCase())
                .overallStatus("PENDING")
                .build();

        for (String dept : REQUIRED_DEPARTMENTS) {
            ClearanceSignoff signoff = ClearanceSignoff.builder()
                    .clearanceRequest(clearanceRequest)
                    .departmentType(dept)
                    .signoffStatus("PENDING")
                    .build();
            clearanceRequest.getSignoffs().add(signoff);
        }

        ClearanceRequest saved = clearanceRequestRepository.save(clearanceRequest);
        return mapToRequestDto(saved);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_EQUITY_PROFILES, allEntries = true)
    public ClearanceSignoffDto processSignoff(Long signoffId, ProcessSignoffRequest request, Long actorUserId) {
        ClearanceSignoff signoff = clearanceSignoffRepository.findById(signoffId)
                .orElseThrow(() -> new ResourceNotFoundException("Clearance sign-off not found: " + signoffId));

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Actor user not found: " + actorUserId));

        validateUserDepartmentRole(actor, signoff.getDepartmentType());
        validateSequentialClearanceOrder(signoff.getClearanceRequest(), signoff.getDepartmentType());

        signoff.setSignoffStatus(request.signoffStatus().toUpperCase());
        signoff.setRemarks(request.remarks());
        signoff.setSignedByUser(actor);
        signoff.setSignedAt(LocalDateTime.now());

        ClearanceSignoff updated = clearanceSignoffRepository.save(signoff);

        // Check overall clearance completion and update status lifecycle
        ClearanceRequest clearanceRequest = signoff.getClearanceRequest();
        if (clearanceRequest.getSignoffs() != null) {
            for (ClearanceSignoff s : clearanceRequest.getSignoffs()) {
                if (s.getId() != null && s.getId().equals(signoff.getId())) {
                    s.setSignoffStatus(signoff.getSignoffStatus());
                    s.setRemarks(signoff.getRemarks());
                    s.setSignedByUser(signoff.getSignedByUser());
                    s.setSignedAt(signoff.getSignedAt());
                }
            }
        }

        boolean allApproved = clearanceRequest.getSignoffs() != null && !clearanceRequest.getSignoffs().isEmpty() &&
                clearanceRequest.getSignoffs().stream().allMatch(s -> "APPROVED".equalsIgnoreCase(s.getSignoffStatus()));
        boolean anyRejected = clearanceRequest.getSignoffs() != null &&
                clearanceRequest.getSignoffs().stream().anyMatch(s -> "REJECTED".equalsIgnoreCase(s.getSignoffStatus()));

        StudentProfile profile = clearanceRequest.getStudentProfile();

        if (allApproved) {
            clearanceRequest.setOverallStatus("CLEARED");
            if (profile != null) {
                profile.updateClearance(StudentProfile.ClearanceStatus.CLEARED, StudentProfile.ClearanceStatus.CLEARED);
                studentProfileRepository.save(profile);
            }
        } else if (anyRejected) {
            clearanceRequest.setOverallStatus("REJECTED");
            if (profile != null) {
                profile.updateClearance(profile.getFinancialClearance(), StudentProfile.ClearanceStatus.BLOCKED);
                studentProfileRepository.save(profile);
            }
        } else {
            clearanceRequest.setOverallStatus("PENDING");
            if (profile != null && profile.getDepartmentalClearance() != StudentProfile.ClearanceStatus.CLEARED) {
                profile.updateClearance(profile.getFinancialClearance(), StudentProfile.ClearanceStatus.PENDING);
                studentProfileRepository.save(profile);
            }
        }
        clearanceRequestRepository.save(clearanceRequest);

        return mapToSignoffDto(updated);
    }

    @Transactional(readOnly = true)
    public ClearanceRequestDto getClearanceByStudentAndTerm(Long studentProfileId, Long termId) {
        return getClearanceByStudentAndTerm(String.valueOf(studentProfileId), termId);
    }

    @Transactional(readOnly = true)
    public ClearanceRequestDto getClearanceByStudentAndTerm(String studentIdentifier, Long termId) {
        StudentProfile student = resolveStudentProfile(studentIdentifier);

        ClearanceRequest clearance = clearanceRequestRepository.findByStudentProfileIdAndTermId(student.getId(), termId)
                .orElseThrow(() -> new ResourceNotFoundException("No clearance request found for student and term."));
        return mapToRequestDto(clearance);
    }

    private StudentProfile resolveStudentProfile(String studentNumber, Long studentProfileId) {
        if (studentNumber != null && !studentNumber.isBlank()) {
            java.util.Optional<StudentProfile> byNum = studentProfileRepository.findByStudentNumber(studentNumber.trim());
            if (byNum.isPresent()) {
                return byNum.get();
            }
        }
        if (studentProfileId != null) {
            java.util.Optional<StudentProfile> byUserId = studentProfileRepository.findByUserId(studentProfileId);
            if (byUserId.isPresent()) {
                return byUserId.get();
            }
            java.util.Optional<StudentProfile> byId = studentProfileRepository.findById(studentProfileId);
            if (byId.isPresent()) {
                return byId.get();
            }
        }
        throw new ResourceNotFoundException("Student Profile not found: " + (studentNumber != null ? studentNumber : studentProfileId));
    }

    private StudentProfile resolveStudentProfile(String identifier) {
        if (identifier != null && !identifier.isBlank()) {
            java.util.Optional<StudentProfile> byNum = studentProfileRepository.findByStudentNumber(identifier.trim());
            if (byNum.isPresent()) {
                return byNum.get();
            }
            try {
                Long id = Long.parseLong(identifier.trim());
                java.util.Optional<StudentProfile> byUserId = studentProfileRepository.findByUserId(id);
                if (byUserId.isPresent()) {
                    return byUserId.get();
                }
                java.util.Optional<StudentProfile> byId = studentProfileRepository.findById(id);
                if (byId.isPresent()) {
                    return byId.get();
                }
            } catch (NumberFormatException ignored) {}
        }
        throw new ResourceNotFoundException("Student Profile not found for identifier: " + identifier);
    }

    @Transactional(readOnly = true)
    public ClearanceRequestDto getClearanceById(Long requestId) {
        ClearanceRequest clearance = clearanceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Clearance request not found: " + requestId));
        return mapToRequestDto(clearance);
    }

    @Transactional(readOnly = true)
    public List<ClearanceSignoffDto> getPendingSignoffsByDepartment(String departmentType) {
        return clearanceSignoffRepository.findByDepartmentTypeAndSignoffStatus(departmentType.toUpperCase(), "PENDING")
                .stream()
                .map(this::mapToSignoffDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClearanceStudentSuggestionDto> getClearanceStudentSuggestions(String query) {
        String searchPattern = query != null ? query.trim().toLowerCase() : "";
        return clearanceRequestRepository.findAll().stream()
                .filter(cr -> {
                    if (searchPattern.isEmpty()) return true;
                    String num = cr.getStudentProfile() != null && cr.getStudentProfile().getStudentNumber() != null
                            ? cr.getStudentProfile().getStudentNumber().toLowerCase() : "";
                    String name = cr.getStudentProfile() != null && cr.getStudentProfile().getUser() != null
                            ? cr.getStudentProfile().getUser().getUsername().toLowerCase() : "";
                    return num.contains(searchPattern) || name.contains(searchPattern);
                })
                .map(cr -> new ClearanceStudentSuggestionDto(
                        cr.getStudentProfile().getId(),
                        cr.getStudentProfile().getStudentNumber(),
                        cr.getStudentProfile().getUser() != null ? cr.getStudentProfile().getUser().getUsername() : "N/A",
                        cr.getStudentProfile().getProgram() != null ? cr.getStudentProfile().getProgram().getCode() : "N/A",
                        cr.getOverallStatus(),
                        cr.getPurpose()
                ))
                .distinct()
                .limit(20)
                .toList();
    }

    private ClearanceRequestDto mapToRequestDto(ClearanceRequest entity) {
        List<ClearanceSignoffDto> signoffDtos = entity.getSignoffs().stream()
                .map(this::mapToSignoffDto)
                .toList();

        String termName = entity.getTerm().getTermType() != null ? entity.getTerm().getTermType().name() : "Term " + entity.getTerm().getId();

        return new ClearanceRequestDto(
                entity.getId(),
                entity.getStudentProfile().getId(),
                entity.getStudentProfile().getStudentNumber(),
                entity.getStudentProfile().getUser() != null ? entity.getStudentProfile().getUser().getUsername() : "N/A",
                entity.getTerm().getId(),
                termName,
                entity.getPurpose(),
                entity.getOverallStatus(),
                entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : java.time.LocalDateTime.now().toString(),
                signoffDtos
        );
    }

    private ClearanceSignoffDto mapToSignoffDto(ClearanceSignoff entity) {
        return new ClearanceSignoffDto(
                entity.getId(),
                entity.getClearanceRequest().getId(),
                entity.getDepartmentType(),
                entity.getSignoffStatus(),
                entity.getRemarks(),
                entity.getSignedByUser() != null ? entity.getSignedByUser().getId() : null,
                entity.getSignedByUser() != null ? entity.getSignedByUser().getUsername() : null,
                entity.getSignedAt() != null ? entity.getSignedAt().toString() : null
        );
    }

    private void validateUserDepartmentRole(User actor, String departmentType) {
        if (actor.getRoles() == null || actor.getRoles().isEmpty()) {
            throw new AccessDeniedException("User has no assigned roles for clearance sign-off.");
        }

        Set<String> roleNames = actor.getRoles().stream()
                .map(Roles::name)
                .collect(Collectors.toSet());

        if (roleNames.contains("ADMIN") || roleNames.contains("SUPER_ADMIN") || roleNames.contains("REGISTRAR")) {
            return; // Master admin override
        }

        String dept = departmentType != null ? departmentType.toUpperCase() : "";
        boolean isAuthorized = switch (dept) {
            case "ACCOUNTING", "CASHIER" -> roleNames.contains("CASHIER") || roleNames.contains("ACCOUNTANT");
            case "DEAN" -> roleNames.contains("DEAN");
            case "STUDENT_AFFAIRS", "OSAS" -> roleNames.contains("GUIDANCE") || roleNames.contains("STUDENT_AFFAIRS");
            case "LIBRARY" -> roleNames.contains("LIBRARY") || roleNames.contains("FACULTY");
            case "LABORATORY", "LAB" -> roleNames.contains("FACULTY") || roleNames.contains("CHAIRPERSON");
            case "REGISTRAR" -> roleNames.contains("REGISTRAR");
            default -> false;
        };

        if (!isAuthorized) {
            throw new AccessDeniedException("Your assigned role " + roleNames + " is not authorized to sign off for department: " + departmentType);
        }
    }

    private static final List<String> CLEARANCE_SEQUENCE = List.of(
            "LIBRARY", "ACCOUNTING", "LABORATORY", "STUDENT_AFFAIRS", "DEAN"
    );

    private void validateSequentialClearanceOrder(ClearanceRequest request, String currentDepartment) {
        if (request == null || request.getSignoffs() == null || currentDepartment == null) return;

        int currentIndex = CLEARANCE_SEQUENCE.indexOf(currentDepartment.toUpperCase());
        if (currentIndex <= 0) return;

        for (int i = 0; i < currentIndex; i++) {
            String requiredDept = CLEARANCE_SEQUENCE.get(i);
            boolean cleared = request.getSignoffs().stream()
                    .filter(s -> requiredDept.equalsIgnoreCase(s.getDepartmentType()))
                    .anyMatch(s -> "APPROVED".equalsIgnoreCase(s.getSignoffStatus()));

            if (!cleared) {
                throw new IllegalStateException(String.format(
                        "Sequential Clearance Violation: Cannot sign off for '%s' until '%s' has approved.",
                        currentDepartment, requiredDept));
            }
        }
    }
}
