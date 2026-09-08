package com.sdt.web_app.controller.grade;

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
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR') or @sectionSecurity.isInstructor(#id, authentication)")
    public ResponseEntity<SectionRosterResponse> getSectionRoster(@PathVariable("id") Long id) {
        return ResponseEntity.ok(gradeService.getSectionRoster(id));
    }

    @PutMapping("/{id}/grades")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON') or @sectionSecurity.isInstructor(#id, authentication)")
    public ResponseEntity<GradeActionResponse> saveGrades(
            @PathVariable("id") Long id,
            @Valid @RequestBody SaveSectionGradesRequest request,
            Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(gradeService.saveGrades(id, request, actorUserId));
    }

    @PostMapping("/{id}/grades/verify")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<GradeActionResponse> verifyGrades(
            @PathVariable("id") Long id,
            Authentication authentication) {
        Long approverUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(gradeService.verifyGrades(id, approverUserId));
    }

    @PostMapping("/{id}/grades/seal")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<GradeActionResponse> sealGrades(
            @PathVariable("id") Long id,
            Authentication authentication) {
        Long registrarUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(gradeService.sealGrades(id, registrarUserId));
    }
}
