package com.sdt.web_app.controller.compliance;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.common.SliceResponse;
import com.sdt.web_app.dto.compliance.EquityDtos.*;
import com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.EquityVerificationStatus;
import com.sdt.web_app.service.compliance.StudentEquityProfileService;
import com.sdt.web_app.service.security.SecurityUtils;
import com.sdt.web_app.utils.SortPropertyMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/compliance/equity-profiles", "/api/v1/equity-profiles"})
@RequiredArgsConstructor
public class StudentEquityProfileController {

    private final StudentEquityProfileService equityProfileService;
    private final SecurityUtils securityUtils;

    @Auditable(action = "READ_MY_EQUITY_PROFILE", entityName = "StudentEquityProfile")
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('student:equity:manage') or hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN', 'REGISTRAR')")
    public ResponseEntity<StudentEquityProfileDto> getMyEquityProfile(Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        StudentEquityProfileDto result = equityProfileService.getEquityProfileForUser(userId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "UPDATE_MY_EQUITY_PROFILE", entityName = "StudentEquityProfile")
    @PutMapping("/me")
    @PreAuthorize("hasAuthority('student:equity:manage') or hasAnyRole('STUDENT', 'ADMIN', 'SUPER_ADMIN', 'REGISTRAR')")
    public ResponseEntity<StudentEquityProfileDto> updateMyEquityProfile(
            @Valid @RequestBody UpdateStudentEquityProfileRequest request,
            Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        StudentEquityProfileDto result = equityProfileService.upsertEquityProfileForUser(userId, request);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "READ_STUDENT_EQUITY_PROFILE", entityName = "StudentEquityProfile", entityId = "#studentProfileId")
    @GetMapping("/student/{studentProfileId}")
    @PreAuthorize("hasAuthority('student:equity:manage') or hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'GUIDANCE', 'FACULTY', 'CASHIER', 'ACCOUNTANT')")
    public ResponseEntity<StudentEquityProfileDto> getEquityProfileByStudentProfileId(@PathVariable Long studentProfileId) {
        StudentEquityProfileDto result = equityProfileService.getEquityProfileByStudentProfileId(studentProfileId);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "VERIFY_EQUITY_PROFILE", entityName = "StudentEquityProfile", entityId = "#profileId")
    @PutMapping("/{profileId}/verify")
    @PreAuthorize("hasAuthority('student:equity:manage') or hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'GUIDANCE')")
    public ResponseEntity<StudentEquityProfileDto> verifyEquityProfile(
            @PathVariable Long profileId,
            @Valid @RequestBody VerifyEquityProfileRequest request,
            Authentication authentication) {
        Long verifierUserId = securityUtils.resolveUserId(authentication);
        StudentEquityProfileDto result = equityProfileService.verifyEquityProfile(profileId, verifierUserId, request);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "SEARCH_EQUITY_PROFILES", entityName = "StudentEquityProfile")
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('student:equity:manage') or hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'GUIDANCE')")
    public ResponseEntity<Page<StudentEquityProfileDto>> searchEquityProfiles(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EquityVerificationStatus status,
            @RequestParam(required = false) Boolean is4ps,
            @RequestParam(required = false) Boolean isIp,
            @RequestParam(required = false) Boolean isPwd,
            @RequestParam(required = false) Boolean isGida,
            @RequestParam(required = false) Boolean isFirstGen,
            @RequestParam(required = false) Boolean isSoloParent,
            @RequestParam(required = false) Boolean isFarmerFisherfolk,
            @RequestParam(required = false) Boolean isBottom40,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Pageable pageable = SortPropertyMapper.createEquityProfilePageable(page, size, sortBy, sortDir);
        Page<StudentEquityProfileDto> result = equityProfileService.searchEquityProfiles(
                search, status, is4ps, isIp, isPwd, isGida, isFirstGen, isSoloParent, isFarmerFisherfolk, isBottom40, pageable);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "SEARCH_EQUITY_PROFILES_SLICE", entityName = "StudentEquityProfile")
    @GetMapping("/search-slice")
    @PreAuthorize("hasAuthority('student:equity:manage') or hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'GUIDANCE')")
    public ResponseEntity<SliceResponse<StudentEquityProfileDto>> searchEquityProfilesSlice(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EquityVerificationStatus status,
            @RequestParam(required = false) Boolean is4ps,
            @RequestParam(required = false) Boolean isIp,
            @RequestParam(required = false) Boolean isPwd,
            @RequestParam(required = false) Boolean isGida,
            @RequestParam(required = false) Boolean isFirstGen,
            @RequestParam(required = false) Boolean isSoloParent,
            @RequestParam(required = false) Boolean isFarmerFisherfolk,
            @RequestParam(required = false) Boolean isBottom40,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Pageable pageable = SortPropertyMapper.createEquityProfilePageable(page, size, sortBy, sortDir);
        SliceResponse<StudentEquityProfileDto> result = equityProfileService.searchEquityProfilesSlice(
                search, status, is4ps, isIp, isPwd, isGida, isFirstGen, isSoloParent, isFarmerFisherfolk, isBottom40, pageable);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "READ_EQUITY_STATISTICS", entityName = "StudentEquityProfile")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('student:equity:manage') or hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'GUIDANCE', 'ACCOUNTANT')")
    public ResponseEntity<EquityStatisticsSummaryDto> getEquityStatisticsSummary() {
        EquityStatisticsSummaryDto result = equityProfileService.getEquityStatisticsSummary();
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "SEARCH_APPLICANT_EQUITY", entityName = "ApplicantEquityAudit")
    @GetMapping("/admission-applicants")
    @PreAuthorize("hasAuthority('student:equity:manage') or hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'GUIDANCE')")
    public ResponseEntity<Page<ApplicantEquityAuditDto>> searchAdmissionApplicants(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) Boolean is4ps,
            @RequestParam(required = false) Boolean isIp,
            @RequestParam(required = false) Boolean isPwd,
            @RequestParam(required = false) Boolean isSoloParent,
            @RequestParam(required = false) Boolean isFarmerFisherfolk,
            @RequestParam(required = false) Boolean isBottom40,
            @RequestParam(required = false) Boolean isGida,
            @RequestParam(required = false) Boolean isFirstGen,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "examScore") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Pageable pageable = SortPropertyMapper.createAdmissionApplicationPageable(page, size, sortBy, sortDir);
        Page<ApplicantEquityAuditDto> result = equityProfileService.searchPostExamApplicantsForEquityAudit(
                search, status, is4ps, isIp, isPwd, isSoloParent, isFarmerFisherfolk, isBottom40, isGida, isFirstGen, pageable);
        return ResponseEntity.ok(result);
    }

    @Auditable(action = "READ_APPLICANT_EQUITY_STATS", entityName = "ApplicantEquityAudit")
    @GetMapping("/admission-applicants/statistics")
    @PreAuthorize("hasAuthority('student:equity:manage') or hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'GUIDANCE')")
    public ResponseEntity<ApplicantEquityStatsDto> getAdmissionApplicantEquityStats() {
        ApplicantEquityStatsDto stats = equityProfileService.getPostExamApplicantEquityStatistics();
        return ResponseEntity.ok(stats);
    }

    @Auditable(action = "READ_APPLICANT_EQUITY_DOSSIER", entityName = "ApplicantEquityAudit", entityId = "#applicationNumber")
    @GetMapping("/admission-applicants/{applicationNumber}")
    @PreAuthorize("hasAuthority('student:equity:manage') or hasAnyRole('ADMIN', 'SUPER_ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'GUIDANCE')")
    public ResponseEntity<ApplicantEquityAuditDto> getAdmissionApplicantDossier(@PathVariable String applicationNumber) {
        ApplicantEquityAuditDto result = equityProfileService.getApplicantEquityDossier(applicationNumber);
        return ResponseEntity.ok(result);
    }
}
