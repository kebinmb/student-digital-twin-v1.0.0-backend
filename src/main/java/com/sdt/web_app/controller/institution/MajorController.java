package com.sdt.web_app.controller.institution;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.institution.MajorDtos.*;
import com.sdt.web_app.service.institution.MajorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/majors")
@Validated
@RequiredArgsConstructor
public class MajorController {

    private final MajorService majorService;

    @Auditable(action = "READ_MAJORS_BY_PROGRAM", entityName = "Major", entityId = "#programId")
    @GetMapping("/program/{programId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<List<MajorSummaryResponse>> getMajorsByProgram(
            @PathVariable Long programId,
            @RequestParam(name = "activeOnly", defaultValue = "false") boolean activeOnly) {
        if (activeOnly) {
            return ResponseEntity.ok(majorService.getActiveMajorsByProgram(programId));
        }
        return ResponseEntity.ok(majorService.getMajorsByProgram(programId));
    }

    @Auditable(action = "READ_MAJOR", entityName = "Major", entityId = "#id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    public ResponseEntity<MajorDetailResponse> getMajorById(@PathVariable Long id) {
        return ResponseEntity.ok(majorService.getMajorById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    @Auditable(action = "CREATE_MAJOR", entityName = "Major")
    public ResponseEntity<MajorDetailResponse> createMajor(@Valid @RequestBody CreateMajorRequest request) {
        MajorDetailResponse response = majorService.createMajor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    @Auditable(action = "UPDATE_MAJOR", entityName = "Major", entityId = "#id")
    public ResponseEntity<MajorDetailResponse> updateMajor(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMajorRequest request) {
        return ResponseEntity.ok(majorService.updateMajor(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON')")
    @Auditable(action = "DELETE_MAJOR", entityName = "Major", entityId = "#id")
    public ResponseEntity<Void> deleteMajor(@PathVariable Long id) {
        majorService.deleteMajor(id);
        return ResponseEntity.noContent().build();
    }
}
