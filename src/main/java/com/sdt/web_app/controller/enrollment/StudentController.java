package com.sdt.web_app.controller.enrollment;

import com.sdt.web_app.dto.enrollment.EnrollmentDtos.StudentSearchResultDto;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentProfileRepository studentProfileRepository;

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEAN', 'CHAIRPERSON', 'REGISTRAR', 'FACULTY', 'STUDENT')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<StudentSearchResultDto>> searchStudents(
            @RequestParam(value = "query", required = false, defaultValue = "") String query) {
        
        List<StudentProfile> profiles = studentProfileRepository.searchStudents(query != null ? query.trim() : "");
        
        List<StudentSearchResultDto> results = profiles.stream().map(sp -> new StudentSearchResultDto(
                sp.getId(),
                sp.getStudentNumber(),
                sp.getUser() != null ? sp.getUser().getUsername() : "Student " + sp.getStudentNumber(),
                sp.getProgram() != null ? sp.getProgram().getCode() : "BSIT",
                sp.getYearLevel(),
                sp.getEnrollmentStatus() != null ? sp.getEnrollmentStatus().name() : "REGULAR"
        )).toList();

        return ResponseEntity.ok(results);
    }
}
