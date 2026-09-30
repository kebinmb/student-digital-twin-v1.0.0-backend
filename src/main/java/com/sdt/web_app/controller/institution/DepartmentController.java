package com.sdt.web_app.controller.institution;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.institution.DepartmentDtos.*;
import com.sdt.web_app.service.institution.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/departments")
@Validated
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @Auditable(action = "CREATE_DEPARTMENT", entityName = "Department")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody CreateDepartmentRequest request) {
        DepartmentResponse response = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Auditable(action = "READ_ALL_DEPARTMENTS", entityName = "Department")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @Auditable(action = "READ_DEPARTMENT", entityName = "Department", entityId = "#id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<DepartmentResponse> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @Auditable(action = "READ_DEPARTMENTS_BY_CAMPUS", entityName = "Department", entityId = "#campusId")
    @GetMapping("/campus/{campusId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<DepartmentResponse>> getDepartmentsByCampus(@PathVariable Long campusId) {
        return ResponseEntity.ok(departmentService.getDepartmentsByCampus(campusId));
    }

    @Auditable(action = "UPDATE_DEPARTMENT", entityName = "Department", entityId = "#id")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN')")
    public ResponseEntity<DepartmentResponse> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDepartmentRequest request) {
        return ResponseEntity.ok(departmentService.updateDepartment(id, request));
    }

    @Auditable(action = "DELETE_DEPARTMENT", entityName = "Department", entityId = "#id")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }
}
