package com.sdt.web_app.controller.institution;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.institution.CiloPiloMappingDtos.*;
import com.sdt.web_app.service.institution.CiloPiloMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cilo-pilo-mappings")
@Validated
@RequiredArgsConstructor
public class CiloPiloMappingController {

    private final CiloPiloMappingService mappingService;

    @Auditable(action = "SAVE_CILO_PILO_MAPPING", entityName = "CiloPiloMapping")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    public ResponseEntity<CiloPiloMappingResponse> createOrUpdateMapping(
            @Valid @RequestBody CreateCiloPiloMappingRequest request) {
        CiloPiloMappingResponse response = mappingService.createOrUpdateMapping(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<CiloPiloMappingResponse>> getMatrixMappings(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long programId) {
        return ResponseEntity.ok(mappingService.getMatrixMappings(courseId, programId));
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<CiloPiloMappingResponse>> getMappingsByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(mappingService.getMappingsByCourseId(courseId));
    }

    @GetMapping("/program/{programId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<CiloPiloMappingResponse>> getMappingsByProgram(@PathVariable Long programId) {
        return ResponseEntity.ok(mappingService.getMappingsByProgramId(programId));
    }

    @GetMapping("/course-outcome/{ciloId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<CiloPiloMappingResponse>> getMappingsByCourseOutcome(@PathVariable Long ciloId) {
        return ResponseEntity.ok(mappingService.getMappingsByCourseOutcomeId(ciloId));
    }

    @GetMapping("/program-outcome/{piloId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<CiloPiloMappingResponse>> getMappingsByProgramOutcome(@PathVariable Long piloId) {
        return ResponseEntity.ok(mappingService.getMappingsByProgramOutcomeId(piloId));
    }

    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<Void> deleteMapping(@PathVariable Long id) {
        mappingService.deleteMapping(id);
        return ResponseEntity.noContent().build();
    }
}
