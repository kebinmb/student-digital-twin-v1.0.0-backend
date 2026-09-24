package com.sdt.web_app.controller.institution;

import com.sdt.web_app.dto.institution.AcademicYearDtos.*;
import com.sdt.web_app.service.institution.AcademicYearService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academic-years")
@Validated
@RequiredArgsConstructor
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<AcademicYearResponse> createAcademicYear(@Valid @RequestBody CreateAcademicYearRequest request) {
        AcademicYearResponse response = academicYearService.createAcademicYear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT', 'CASHIER', 'ACCOUNTANT')")
    public ResponseEntity<List<AcademicYearResponse>> getAllAcademicYears() {
        return ResponseEntity.ok(academicYearService.getAllAcademicYears());
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT', 'CASHIER', 'ACCOUNTANT')")
    public ResponseEntity<AcademicYearResponse> getCurrentAcademicYear() {
        return ResponseEntity.ok(academicYearService.getCurrentAcademicYear());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT', 'CASHIER', 'ACCOUNTANT')")
    public ResponseEntity<AcademicYearResponse> getAcademicYearById(@PathVariable Long id) {
        return ResponseEntity.ok(academicYearService.getAcademicYearById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<AcademicYearResponse> updateAcademicYear(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAcademicYearRequest request) {
        return ResponseEntity.ok(academicYearService.updateAcademicYear(id, request));
    }

    @PutMapping("/{id}/set-current")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<AcademicYearResponse> setCurrentAcademicYear(@PathVariable Long id) {
        return ResponseEntity.ok(academicYearService.setCurrentAcademicYear(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAcademicYear(@PathVariable Long id) {
        academicYearService.deleteAcademicYear(id);
        return ResponseEntity.noContent().build();
    }
}
