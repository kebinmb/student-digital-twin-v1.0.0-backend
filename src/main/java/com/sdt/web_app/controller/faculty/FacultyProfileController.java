package com.sdt.web_app.controller.faculty;

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

    @PostMapping("/faculty")
    @PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
    public ResponseEntity<FacultyProfileResponse> createFacultyAccount(@Valid @RequestBody CreateFacultyAccountRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(facultyProfileService.createFacultyAccount(request));
    }

    @GetMapping("/faculty")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR')")
    public ResponseEntity<List<FacultyProfileResponse>> getAllFaculty() {
        return ResponseEntity.ok(facultyProfileService.getAllFacultyProfiles());
    }

    @GetMapping("/faculty/{userId}/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR') or @facultySecurity.isFacultySelf(#userId, authentication)")
    public ResponseEntity<FacultyProfileResponse> getFacultyProfile(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(facultyProfileService.getProfileByUserId(userId));
    }

    @PutMapping("/faculty/{userId}/profile")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<FacultyProfileResponse> updateFacultyProfile(
            @PathVariable("userId") Long userId,
            @Valid @RequestBody UpdateFacultyProfileRequest request) {
        return ResponseEntity.ok(facultyProfileService.updateProfile(userId, request));
    }

    @GetMapping("/reports/ched-e5")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'REGISTRAR')")
    public ResponseEntity<ChedE5ReportResponse> generateChedE5Report(@RequestParam("termId") Long termId) {
        return ResponseEntity.ok(facultyProfileService.generateChedE5Report(termId));
    }
}
