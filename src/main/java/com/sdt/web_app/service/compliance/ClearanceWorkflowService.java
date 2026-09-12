package com.sdt.web_app.service.compliance;

import com.sdt.web_app.dto.compliance.ComplianceDtos.*;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClearanceWorkflowService {

    private final ClearanceRequestRepository clearanceRequestRepository;
    private final ClearanceSignoffRepository clearanceSignoffRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TermRepository termRepository;
    private final UserRepository userRepository;

    private static final List<String> REQUIRED_DEPARTMENTS = List.of(
            "LIBRARY", "ACCOUNTING", "LABORATORY", "STUDENT_AFFAIRS", "DEAN"
    );

    @Transactional
    public ClearanceRequestDto initiateClearanceRequest(InitiateClearanceRequest request, Long actorUserId) {
        StudentProfile student = studentProfileRepository.findById(request.studentProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Student Profile not found: " + request.studentProfileId()));

        Term term = termRepository.findById(request.termId())
                .orElseThrow(() -> new ResourceNotFoundException("Term not found: " + request.termId()));

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
    public ClearanceSignoffDto processSignoff(Long signoffId, ProcessSignoffRequest request, Long actorUserId) {
        ClearanceSignoff signoff = clearanceSignoffRepository.findById(signoffId)
                .orElseThrow(() -> new ResourceNotFoundException("Clearance sign-off not found: " + signoffId));

        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Actor user not found: " + actorUserId));

        signoff.setSignoffStatus(request.signoffStatus().toUpperCase());
        signoff.setRemarks(request.remarks());
        signoff.setSignedByUser(actor);
        signoff.setSignedAt(LocalDateTime.now());

        ClearanceSignoff updated = clearanceSignoffRepository.save(signoff);

        // Check overall clearance completion
        ClearanceRequest clearanceRequest = signoff.getClearanceRequest();
        boolean allApproved = clearanceRequest.getSignoffs().stream()
                .allMatch(s -> "APPROVED".equals(s.getSignoffStatus()));
        boolean anyRejected = clearanceRequest.getSignoffs().stream()
                .anyMatch(s -> "REJECTED".equals(s.getSignoffStatus()));

        if (allApproved) {
            clearanceRequest.setOverallStatus("CLEARED");
            StudentProfile profile = clearanceRequest.getStudentProfile();
            profile.updateClearance(StudentProfile.ClearanceStatus.CLEARED, StudentProfile.ClearanceStatus.CLEARED);
            studentProfileRepository.save(profile);
        } else if (anyRejected) {
            clearanceRequest.setOverallStatus("REJECTED");
        }
        clearanceRequestRepository.save(clearanceRequest);

        return mapToSignoffDto(updated);
    }

    @Transactional(readOnly = true)
    public ClearanceRequestDto getClearanceByStudentAndTerm(Long studentProfileId, Long termId) {
        ClearanceRequest clearance = clearanceRequestRepository.findByStudentProfileIdAndTermId(studentProfileId, termId)
                .orElseThrow(() -> new ResourceNotFoundException("No clearance request found for student and term."));
        return mapToRequestDto(clearance);
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
}
