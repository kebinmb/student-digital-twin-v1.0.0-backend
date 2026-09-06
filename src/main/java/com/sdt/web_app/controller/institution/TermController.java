package com.sdt.web_app.controller.institution;

import com.sdt.web_app.dto.institution.TermDtos.*;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.service.institution.TermLifecycleService;
import com.sdt.web_app.service.institution.TermService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;
    private final TermLifecycleService termLifecycleService;

    @GetMapping
    public ResponseEntity<List<TermResponse>> getAllTerms() {
        List<TermResponse> responses = termService.getAllTerms().stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/academic-year/{academicYearId}")
    public ResponseEntity<List<TermResponse>> getTermsByAcademicYear(@PathVariable Long academicYearId) {
        List<TermResponse> responses = termService.getTermsByAcademicYear(academicYearId).stream()
                .map(this::mapToResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TermResponse> getTermById(@PathVariable Long id) {
        return ResponseEntity.ok(mapToResponse(termService.getTermById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<TermResponse> createTerm(@Valid @RequestBody CreateTermRequest request) {
        Term term = termService.createTerm(
                request.academicYearId(),
                request.termType(),
                request.startDate(),
                request.endDate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(term));
    }

    @PutMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<TermResponse> updateTermSchedule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTermScheduleRequest request
    ) {
        Term term = termService.updateTermSchedule(id, request.startDate(), request.endDate());
        return ResponseEntity.ok(mapToResponse(term));
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<TermResponse> activateTerm(@PathVariable Long id) {
        Term term = termLifecycleService.activateTerm(id);
        return ResponseEntity.ok(mapToResponse(term));
    }

    @PutMapping("/{id}/enrollment-window")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<TermResponse> toggleEnrollmentWindow(
            @PathVariable Long id,
            @RequestParam boolean open
    ) {
        Term term = open ? termLifecycleService.openEnrollment(id) : termLifecycleService.closeEnrollment(id);
        return ResponseEntity.ok(mapToResponse(term));
    }

    @PutMapping("/{id}/grading-window")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<TermResponse> toggleGradingWindow(
            @PathVariable Long id,
            @RequestParam boolean open
    ) {
        Term term = open ? termLifecycleService.openGrading(id) : termLifecycleService.lockGrading(id);
        return ResponseEntity.ok(mapToResponse(term));
    }

    @PutMapping("/{id}/add-drop-window")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<TermResponse> toggleAddDropWindow(
            @PathVariable Long id,
            @RequestParam boolean open
    ) {
        Term term = termLifecycleService.toggleAddDrop(id, open);
        return ResponseEntity.ok(mapToResponse(term));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Void> deleteTerm(@PathVariable Long id) {
        termService.deleteTerm(id);
        return ResponseEntity.noContent().build();
    }

    private TermResponse mapToResponse(Term t) {
        return new TermResponse(
                t.getId(),
                t.getAcademicYear().getId(),
                t.getAcademicYear().getCode(),
                t.getTermType(),
                t.getStartDate(),
                t.getEndDate(),
                t.getAcademicYear().isCurrent(),
                t.isActive(),
                t.isEnrollmentOpen(),
                t.isGradingOpen(),
                t.isAddDropOpen()
        );
    }
}
