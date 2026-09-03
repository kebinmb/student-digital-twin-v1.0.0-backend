package com.sdt.web_app.controller.institution;

import com.sdt.web_app.dto.institution.ProgramDtos.*;
import com.sdt.web_app.service.institution.ProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/programs")
@Validated
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<ProgramResponse>> getAllPrograms() {
        return ResponseEntity.ok(programService.getAllPrograms());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<ProgramResponse> getProgramById(@PathVariable Long id) {
        return ResponseEntity.ok(programService.getProgramById(id));
    }

    @GetMapping("/department/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<ProgramResponse>> getProgramsByDepartment(@PathVariable Long departmentId) {
        return ResponseEntity.ok(programService.getProgramsByDepartment(departmentId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<ProgramResponse> createProgram(@Valid @RequestBody CreateProgramRequest request) {
        ProgramResponse response = programService.createProgram(
                request.departmentId(),
                request.code(),
                request.name(),
                request.major(),
                request.degreeLevel(),
                request.totalUnitsRequired()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<ProgramResponse> updateProgram(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProgramRequest request) {
        ProgramResponse response = programService.updateProgram(
                id,
                request.name(),
                request.major(),
                request.cmoReference(),
                request.governmentPermit(),
                request.totalUnitsRequired()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public ResponseEntity<Void> deleteProgram(@PathVariable Long id) {
        programService.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/outcomes")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<ProgramOutcomeResponse>> getProgramOutcomes(@PathVariable Long id) {
        return ResponseEntity.ok(programService.getProgramOutcomes(id));
    }

    @PostMapping("/{id}/outcomes")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<ProgramOutcomeResponse> createProgramOutcome(
            @PathVariable Long id,
            @Valid @RequestBody CreateProgramOutcomeRequest request) {
        ProgramOutcomeResponse response = programService.createProgramOutcome(id, request.code(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/outcomes/{outcomeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<Void> deleteProgramOutcome(@PathVariable Long outcomeId) {
        programService.deleteProgramOutcome(outcomeId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{programId}/outcomes/{outcomeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<Void> deleteProgramOutcomeNested(
            @PathVariable Long programId,
            @PathVariable Long outcomeId) {
        programService.deleteProgramOutcome(outcomeId);
        return ResponseEntity.noContent().build();
    }
}
