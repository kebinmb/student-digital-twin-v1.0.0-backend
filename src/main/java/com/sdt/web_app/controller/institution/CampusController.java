package com.sdt.web_app.controller.institution;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.institution.CampusDtos.*;
import com.sdt.web_app.service.institution.CampusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/campuses")
@Validated
@RequiredArgsConstructor
public class CampusController {

    private final CampusService campusService;

    @Auditable(action = "CREATE_CAMPUS", entityName = "Campus")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampusResponse> createCampus(@Valid @RequestBody CreateCampusRequest request) {
        CampusResponse response = campusService.createCampus(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Auditable(action = "READ_ALL_CAMPUSES", entityName = "Campus")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'CASHIER', 'ACCOUNTANT')")
    public ResponseEntity<List<CampusResponse>> getAllCampuses() {
        return ResponseEntity.ok(campusService.getAllCampuses());
    }

    @Auditable(action = "READ_ACTIVE_CAMPUSES", entityName = "Campus")
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'CASHIER', 'ACCOUNTANT')")
    public ResponseEntity<List<CampusResponse>> getActiveCampuses() {
        return ResponseEntity.ok(campusService.getActiveCampuses());
    }

    @Auditable(action = "READ_CAMPUS", entityName = "Campus", entityId = "#id")
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'CASHIER', 'ACCOUNTANT')")
    public ResponseEntity<CampusResponse> getCampusById(@PathVariable Long id) {
        return ResponseEntity.ok(campusService.getCampusById(id));
    }

    @Auditable(action = "UPDATE_CAMPUS", entityName = "Campus", entityId = "#id")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampusResponse> updateCampus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCampusRequest request) {
        return ResponseEntity.ok(campusService.updateCampus(id, request));
    }

    @Auditable(action = "UPDATE_CAMPUS_STATUS", entityName = "Campus", entityId = "#id")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CampusResponse> toggleCampusStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        return ResponseEntity.ok(campusService.toggleCampusActive(id, active));
    }

    @Auditable(action = "DELETE_CAMPUS", entityName = "Campus", entityId = "#id")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCampus(@PathVariable Long id) {
        campusService.deleteCampus(id);
        return ResponseEntity.noContent().build();
    }
}
