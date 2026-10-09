package com.sdt.web_app.controller.institution;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.entities.institution.InstitutionalOutcome;
import com.sdt.web_app.service.institution.InstitutionalOutcomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/curriculum/iilo")
@RequiredArgsConstructor
public class InstitutionalOutcomeController {

    private final InstitutionalOutcomeService service;

    @Auditable(action = "READ_ACTIVE_IILO", entityName = "InstitutionalOutcome")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY', 'REGISTRAR', 'STUDENT')")
    public ResponseEntity<List<InstitutionalOutcome>> getAllActiveOutcomes() {
        return ResponseEntity.ok(service.getAllActiveOutcomes());
    }

    @Auditable(action = "READ_IILO", entityName = "InstitutionalOutcome", entityId = "#id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'FACULTY', 'REGISTRAR', 'STUDENT')")
    public ResponseEntity<InstitutionalOutcome> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    public record CreateIiloRequest(String code, String statement, String description) {}

    @Auditable(action = "CREATE_IILO", entityName = "InstitutionalOutcome")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public ResponseEntity<InstitutionalOutcome> createOutcome(@RequestBody CreateIiloRequest request) {
        InstitutionalOutcome created = service.createOutcome(request.code(), request.statement(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
