package com.sdt.web_app.controller.grade;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.grade.GradeDtos.*;
import com.sdt.web_app.service.grade.GradeService;
import com.sdt.web_app.service.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sections")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;
    private final SecurityUtils securityUtils;

    @GetMapping("/{id}/roster")
    @PreAuthorize("@sectionSecurity.canAccessSection(#id, authentication)")
    public ResponseEntity<SectionRosterResponse> getSectionRoster(@PathVariable("id") Long id) {
        return ResponseEntity.ok(gradeService.getSectionRoster(id));
    }

    @Auditable(action = "SAVE_GRADES", entityName = "ClassSection", entityId = "#id")
    @PutMapping("/{id}/grades")
    @PreAuthorize("@sectionSecurity.canAccessSection(#id, authentication)")
    public ResponseEntity<GradeActionResponse> saveGrades(
            @PathVariable("id") Long id,
            @Valid @RequestBody SaveSectionGradesRequest request,
            Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(gradeService.saveGrades(id, request, actorUserId));
    }

    @Auditable(action = "VERIFY_GRADES", entityName = "ClassSection", entityId = "#id")
    @PostMapping("/{id}/grades/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN') and @sectionSecurity.canAccessSection(#id, authentication)")
    public ResponseEntity<GradeActionResponse> verifyGrades(
            @PathVariable("id") Long id,
            Authentication authentication) {
        Long approverUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(gradeService.verifyGrades(id, approverUserId));
    }

    @Auditable(action = "REJECT_GRADES", entityName = "ClassSection", entityId = "#id")
    @PostMapping("/{id}/grades/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN') and @sectionSecurity.canAccessSection(#id, authentication)")
    public ResponseEntity<GradeActionResponse> rejectGrades(
            @PathVariable("id") Long id,
            @RequestBody(required = false) RejectGradesRequest request,
            Authentication authentication) {
        Long approverUserId = securityUtils.resolveUserId(authentication);
        String reason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason()
                : "Returned by Dean for revision";
        return ResponseEntity.ok(gradeService.rejectGrades(id, reason, approverUserId));
    }

    @Auditable(action = "SEAL_GRADES", entityName = "ClassSection", entityId = "#id")
    @PostMapping("/{id}/grades/seal")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<GradeActionResponse> sealGrades(
            @PathVariable("id") Long id,
            Authentication authentication) {
        Long registrarUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(gradeService.sealGrades(id, registrarUserId));
    }

    @Auditable(action = "BATCH_VERIFY_GRADES", entityName = "ClassSection")
    @PostMapping("/batch/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<java.util.List<GradeActionResponse>> batchVerifyGrades(
            @RequestBody java.util.List<Long> sectionIds,
            Authentication authentication) {
        Long approverUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(gradeService.batchVerifyGrades(sectionIds, approverUserId));
    }

    @Auditable(action = "BATCH_SEAL_GRADES", entityName = "ClassSection")
    @PostMapping("/batch/seal")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<java.util.List<GradeActionResponse>> batchSealGrades(
            @RequestBody java.util.List<Long> sectionIds,
            Authentication authentication) {
        Long registrarUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(gradeService.batchSealGrades(sectionIds, registrarUserId));
    }
}
