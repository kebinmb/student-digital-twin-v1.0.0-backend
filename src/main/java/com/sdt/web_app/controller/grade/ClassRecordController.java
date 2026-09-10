package com.sdt.web_app.controller.grade;

import com.sdt.web_app.dto.grade.ClassRecordDtos.*;
import com.sdt.web_app.service.grade.ClassRecordService;
import com.sdt.web_app.service.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/class-records")
@RequiredArgsConstructor
@Validated
public class ClassRecordController {

    private final ClassRecordService classRecordService;
    private final SecurityUtils securityUtils;

    @GetMapping("/sections/{sectionId}/config")
    @PreAuthorize("@sectionSecurity.canAccessSection(#sectionId, authentication)")
    public ResponseEntity<SectionGradingConfigResponse> getGradingConfig(@PathVariable("sectionId") Long sectionId) {
        return ResponseEntity.ok(classRecordService.getGradingConfig(sectionId));
    }

    @PutMapping("/sections/{sectionId}/config")
    @PreAuthorize("@sectionSecurity.canAccessSection(#sectionId, authentication) and !hasRole('REGISTRAR')")
    public ResponseEntity<SectionGradingConfigResponse> updateGradingConfig(
            @PathVariable("sectionId") Long sectionId,
            @Valid @RequestBody UpdateSectionGradingConfigRequest request,
            Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(classRecordService.updateGradingConfig(sectionId, request, actorUserId));
    }

    @PostMapping("/sections/{sectionId}/items")
    @PreAuthorize("@sectionSecurity.canAccessSection(#sectionId, authentication) and !hasRole('REGISTRAR')")
    public ResponseEntity<ClassRecordItemDto> addAssessmentItem(
            @PathVariable("sectionId") Long sectionId,
            @Valid @RequestBody CreateClassRecordItemRequest request,
            Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        ClassRecordItemDto response = classRecordService.addAssessmentItem(sectionId, request, actorUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("@sectionSecurity.canAccessItem(#itemId, authentication) and !hasRole('REGISTRAR')")
    public ResponseEntity<Void> deleteAssessmentItem(
            @PathVariable("itemId") Long itemId,
            Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        classRecordService.deleteAssessmentItem(itemId, actorUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sections/{sectionId}/matrix")
    @PreAuthorize("@sectionSecurity.canAccessSection(#sectionId, authentication)")
    public ResponseEntity<ClassRecordMatrixResponse> getScoreMatrix(@PathVariable("sectionId") Long sectionId) {
        return ResponseEntity.ok(classRecordService.getScoreMatrix(sectionId));
    }

    @PostMapping("/sections/{sectionId}/scores/batch")
    @PreAuthorize("@sectionSecurity.canAccessSection(#sectionId, authentication) and !hasRole('REGISTRAR')")
    public ResponseEntity<ClassRecordMatrixResponse> batchSaveScores(
            @PathVariable("sectionId") Long sectionId,
            @Valid @RequestBody BatchSaveScoresRequest request,
            Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(classRecordService.batchSaveScores(sectionId, request, actorUserId));
    }

    @PostMapping("/sections/{sectionId}/recalculate")
    @PreAuthorize("@sectionSecurity.canAccessSection(#sectionId, authentication) and !hasRole('REGISTRAR')")
    public ResponseEntity<ClassRecordMatrixResponse> recalculateAndSyncSectionGrades(
            @PathVariable("sectionId") Long sectionId,
            Authentication authentication) {
        Long actorUserId = securityUtils.resolveUserId(authentication);
        classRecordService.recalculateAndSyncSectionGrades(sectionId, actorUserId);
        return ResponseEntity.ok(classRecordService.getScoreMatrix(sectionId));
    }
}
