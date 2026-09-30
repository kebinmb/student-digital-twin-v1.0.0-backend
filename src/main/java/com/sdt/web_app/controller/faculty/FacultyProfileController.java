package com.sdt.web_app.controller.faculty;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.faculty.FacultyDtos.*;
import com.sdt.web_app.service.faculty.FacultyProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FacultyProfileController {

    private final FacultyProfileService facultyProfileService;

    @Auditable(action = "CREATE_FACULTY_ACCOUNT", entityName = "FacultyProfile")
    @PostMapping("/faculty")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<FacultyProfileResponse> createFacultyAccount(@Valid @RequestBody CreateFacultyAccountRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(facultyProfileService.createFacultyAccount(request));
    }

    @Auditable(action = "READ_ALL_FACULTY", entityName = "FacultyProfile")
    @GetMapping("/faculty")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<?> getAllFaculty(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size) {
        if (page != null && size != null) {
            return ResponseEntity.ok(facultyProfileService.getAllFacultyProfiles(org.springframework.data.domain.PageRequest.of(page, size)));
        }
        return ResponseEntity.ok(facultyProfileService.getAllFacultyProfiles());
    }

    @Auditable(action = "READ_FACULTY_PROFILE", entityName = "FacultyProfile", entityId = "#userId")
    @GetMapping("/faculty/{userId}/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR') or @facultySecurity.isFacultySelf(#userId, authentication)")
    public ResponseEntity<FacultyProfileResponse> getFacultyProfile(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(facultyProfileService.getProfileByUserId(userId));
    }

    @Auditable(action = "UPDATE_FACULTY_PROFILE", entityName = "FacultyProfile", entityId = "#userId")
    @PutMapping("/faculty/{userId}/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<FacultyProfileResponse> updateFacultyProfile(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody UpdateFacultyProfileRequest request) {
        return ResponseEntity.ok(facultyProfileService.updateProfile(userId, request));
    }

    @Auditable(action = "GENERATE_CHED_E5_REPORT", entityName = "ChedE5Report", entityId = "#termId")
    @GetMapping("/reports/ched-e5")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<ChedE5ReportResponse> generateChedE5Report(@RequestParam("termId") Long termId) {
        return ResponseEntity.ok(facultyProfileService.generateChedE5Report(termId));
    }
}
