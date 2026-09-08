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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        return mapToProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public List<StudentSearchResultDto> searchStudents(String query) {
        List<StudentProfile> profiles = studentProfileRepository.searchStudents(query != null ? query.trim() : "");
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
                sp.getCumulativeGpa()
        );
    }
}
