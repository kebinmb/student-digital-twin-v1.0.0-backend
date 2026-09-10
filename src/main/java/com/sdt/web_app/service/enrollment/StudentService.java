package com.sdt.web_app.service.enrollment;

import com.sdt.web_app.dto.enrollment.EnrollmentDtos.*;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.enrollment.StudentProfile.StudentClassification;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.CurriculumRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import com.sdt.web_app.service.security.AcademicScopeAssertionService;
import com.sdt.web_app.service.security.AcademicScopeContext;
import com.sdt.web_app.specifications.StudentProfileSpecifications;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final ProgramRepository programRepository;
    private final CurriculumRepository curriculumRepository;
    private final PasswordEncoder passwordEncoder;
    private final AcademicScopeAssertionService academicScopeAssertionService;

    @Transactional
    public StudentProfileResponse createStudent(CreateStudentRequest request) {
        String trimmedStudentNumber = request.studentNumber().trim();
        if (studentProfileRepository.existsByStudentNumber(trimmedStudentNumber)) {
            throw new IllegalStateException("Student with student number '" + trimmedStudentNumber + "' already exists.");
        }

        String trimmedUsername = request.username().trim();
        if (userRepository.existsByUsername(trimmedUsername)) {
            throw new IllegalStateException("User with username '" + trimmedUsername + "' already exists.");
        }

        String trimmedEmail = request.email().trim();
        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new IllegalStateException("User with email '" + trimmedEmail + "' already exists.");
        }

        Program program = programRepository.findById(request.programId())
                .orElseThrow(() -> new EntityNotFoundException("Program not found with ID: " + request.programId()));

        Curriculum curriculum = curriculumRepository.findById(request.curriculumId())
                .orElseThrow(() -> new EntityNotFoundException("Curriculum not found with ID: " + request.curriculumId()));

        if (!curriculum.getProgram().getId().equals(program.getId())) {
            throw new IllegalArgumentException("Curriculum " + curriculum.getCode() + " does not belong to Program " + program.getCode());
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            academicScopeAssertionService.validateProgramMutation(scope, program.getId());
        }

        StudentClassification classification;
        try {
            classification = StudentClassification.valueOf(request.classification().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid student classification: " + request.classification());
        }

        String rawPassword = (request.password() != null && !request.password().isBlank())
                ? request.password().trim()
                : "Student123!";

        User user = User.builder()
                .username(trimmedUsername)
                .email(trimmedEmail)
                .password(passwordEncoder.encode(rawPassword))
                .enabled(true)
                .build();
        user.addRole(Roles.STUDENT);
        User savedUser = userRepository.save(user);

        int yearLevel = (request.yearLevel() != null && request.yearLevel() > 0) ? request.yearLevel() : 1;

        StudentProfile profile = StudentProfile.builder()
                .user(savedUser)
                .studentNumber(trimmedStudentNumber)
                .program(program)
                .curriculum(curriculum)
                .yearLevel(yearLevel)
                .classification(classification)
                .enrollmentStatus(StudentProfile.EnrollmentStatus.REGULAR)
                .isGraduating(false)
                .totalUnitsEarned(BigDecimal.ZERO)
                .build();

        StudentProfile savedProfile = studentProfileRepository.save(profile);
        log.info("Registered new student: {} ({}) under curriculum {}", savedProfile.getStudentNumber(), classification, curriculum.getCode());

        return mapToProfileResponse(savedProfile);
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getStudentById(Long id) {
        StudentProfile profile = studentProfileRepository.findByIdWithProgramAndCurriculum(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            academicScopeAssertionService.validateStudentAccess(scope, profile);
        }

        return mapToProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public List<StudentSearchResultDto> searchStudents(String query) {
        return searchStudents(query, java.util.Optional.empty());
    }

    @Transactional(readOnly = true)
    public List<StudentSearchResultDto> searchStudents(String query, java.util.Optional<List<Long>> scopedProgramIds) {
        String cleanQuery = query != null ? query.trim() : "";
        Specification<StudentProfile> spec = StudentProfileSpecifications.searchKeyword(cleanQuery);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            if (scope.isDean()) {
                spec = spec.and(StudentProfileSpecifications.inCollege(scope.collegeId()));
            } else if (scope.isChairperson()) {
                spec = spec.and(StudentProfileSpecifications.inProgram(scope.programId()));
            } else if (scope.isFaculty()) {
                if (scope.assignedSectionIds().isEmpty()) {
                    return List.of();
                }
                spec = spec.and(StudentProfileSpecifications.enrolledInSections(scope.assignedSectionIds()));
            }
        }

        if (scopedProgramIds != null && scopedProgramIds.isPresent()) {
            List<Long> programIds = scopedProgramIds.get();
            if (programIds.isEmpty()) {
                return List.of();
            }
            spec = spec.and(StudentProfileSpecifications.inPrograms(programIds));
        }

        List<StudentProfile> profiles = studentProfileRepository.findAll(spec);

        return profiles.stream().map(sp -> new StudentSearchResultDto(
                sp.getId(),
                sp.getStudentNumber(),
                sp.getUser() != null ? sp.getUser().getUsername() : "Student " + sp.getStudentNumber(),
                sp.getProgram() != null ? sp.getProgram().getCode() : "BSIT",
                sp.getYearLevel(),
                sp.getEnrollmentStatus() != null ? sp.getEnrollmentStatus().name() : "REGULAR"
        )).toList();
    }

    private StudentProfileResponse mapToProfileResponse(StudentProfile sp) {
        return new StudentProfileResponse(
                sp.getId(),
                sp.getStudentNumber(),
                sp.getUser().getId(),
                sp.getUser().getUsername(),
                sp.getUser().getEmail(),
                sp.getProgram().getId(),
                sp.getProgram().getCode(),
                sp.getProgram().getName(),
                sp.getCurriculum().getId(),
                sp.getCurriculum().getCode(),
                sp.getClassification().name(),
                sp.getYearLevel(),
                sp.getEnrollmentStatus().name(),
                sp.isGraduating(),
                sp.getTotalUnitsEarned(),
                sp.getCumulativeGpa(),
                sp.getFinancialClearance() != null ? sp.getFinancialClearance().name() : "CLEARED",
                sp.getDepartmentalClearance() != null ? sp.getDepartmentalClearance().name() : "CLEARED"
        );
    }

    @Transactional
    public StudentProfileResponse updateClearance(Long studentId, UpdateClearanceRequest request) {
        StudentProfile student = studentProfileRepository.findByIdWithProgramAndCurriculum(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with ID: " + studentId));

        StudentProfile.ClearanceStatus finStatus = null;
        if (request.financialClearance() != null && !request.financialClearance().isBlank()) {
            finStatus = StudentProfile.ClearanceStatus.valueOf(request.financialClearance().trim().toUpperCase());
        }

        StudentProfile.ClearanceStatus deptStatus = null;
        if (request.departmentalClearance() != null && !request.departmentalClearance().isBlank()) {
            deptStatus = StudentProfile.ClearanceStatus.valueOf(request.departmentalClearance().trim().toUpperCase());
        }

        student.updateClearance(finStatus, deptStatus);
        StudentProfile saved = studentProfileRepository.save(student);
        log.info("Updated clearance for student {}: financial={}, departmental={}",
                saved.getStudentNumber(), saved.getFinancialClearance(), saved.getDepartmentalClearance());
        return mapToProfileResponse(saved);
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse getStudentByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }
        StudentProfile profile = studentProfileRepository.findByUserIdWithProgramAndCurriculum(userId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found for user ID: " + userId));
        return mapToProfileResponse(profile);
    }
}
