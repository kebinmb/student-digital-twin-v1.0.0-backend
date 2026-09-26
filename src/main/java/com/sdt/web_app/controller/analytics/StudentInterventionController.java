package com.sdt.web_app.controller.analytics;

import com.sdt.web_app.dto.analytics.AnalyticsDtos.*;
import com.sdt.web_app.dto.common.SliceResponse;
import com.sdt.web_app.service.analytics.StudentInterventionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics/interventions")
@RequiredArgsConstructor
public class StudentInterventionController {

    private final StudentInterventionService interventionService;

    @PostMapping("/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY', 'GUIDANCE')")
    public ResponseEntity<StudentInterventionDto> dispatchIntervention(@Valid @RequestBody DispatchInterventionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(interventionService.dispatchIntervention(request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY', 'GUIDANCE')")
    public ResponseEntity<StudentInterventionDto> updateInterventionStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateInterventionStatusRequest request) {
        return ResponseEntity.ok(interventionService.updateInterventionStatus(id, request));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY', 'GUIDANCE', 'STUDENT')")
    public ResponseEntity<List<StudentInterventionDto>> getStudentInterventions(@PathVariable("studentId") Long studentId) {
        return ResponseEntity.ok(interventionService.getInterventionsByStudent(studentId));
    }

    @GetMapping("/student/{studentId}/slice")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY', 'GUIDANCE', 'STUDENT')")
    public ResponseEntity<SliceResponse<StudentInterventionDto>> getStudentInterventionsSlice(
            @PathVariable("studentId") Long studentId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(interventionService.getInterventionsByStudentSlice(
                studentId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dispatchedAt"))));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY', 'GUIDANCE')")
    public ResponseEntity<Page<StudentInterventionDto>> getAllInterventions(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ResponseEntity.ok(interventionService.getAllInterventions(
                status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dispatchedAt"))));
    }
}
