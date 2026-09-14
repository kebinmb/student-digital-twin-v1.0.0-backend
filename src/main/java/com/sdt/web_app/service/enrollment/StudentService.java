package com.sdt.web_app.service.enrollment;

import com.sdt.web_app.dto.enrollment.EnrollmentDtos.*;
import com.sdt.web_app.entities.admission.AdmissionApplication;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.enrollment.StudentProfile.StudentClassification;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.repositories.admission.AdmissionApplicationRepository;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.CurriculumRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import com.sdt.web_app.entities.compliance.StudentEquityProfile;
import com.sdt.web_app.repositories.compliance.StudentEquityProfileRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sdt.web_app.service.security.StudentProfileL2CacheService;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentProfileRepository studentProfileRepository;
    private final AdmissionApplicationRepository admissionApplicationRepository;
    private final StudentEquityProfileRepository studentEquityProfileRepository;
    private final UserRepository userRepository;
    private final ProgramRepository programRepository;
    private final CurriculumRepository curriculumRepository;
    private final PasswordEncoder passwordEncoder;
    private final AcademicScopeAssertionService academicScopeAssertionService;
    private final StudentProfileL2CacheService studentProfileL2CacheService;

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

        AdmissionApplication app = null;
        if (request.admissionApplicationId() != null) {
            app = admissionApplicationRepository.findById(request.admissionApplicationId()).orElse(null);
        }
        if (app == null) {
            app = admissionApplicationRepository.findFirstByEmailIgnoreCase(trimmedEmail)
                    .or(() -> admissionApplicationRepository.findFirstByApplicationNumber(trimmedStudentNumber))
                    .orElse(null);
        }

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
                .admissionApplicationId(app != null ? app.getId() : null)
                .build();

        StudentProfile savedProfile = studentProfileRepository.save(profile);
        if (app != null) {
            app.markAsEnrolled(savedProfile.getId());
            admissionApplicationRepository.save(app);
            copyEquityProfileIfPresent(app, savedProfile);
        }
        log.info("Registered new student: {} ({}) under curriculum {}", savedProfile.getStudentNumber(), classification, curriculum.getCode());

        return mapToProfileResponse(savedProfile);
    }

    @Transactional
    public StudentProfileResponse createStudentFromAdmissionAppId(Long appId) {
        AdmissionApplication app = admissionApplicationRepository.findById(appId)
                .orElseThrow(() -> new EntityNotFoundException("Admission application not found with ID: " + appId));

        String email = app.getEmail().trim().toLowerCase();
        Optional<User> existingUser = userRepository.findByEmail(email);
        if (existingUser.isPresent()) {
            Optional<StudentProfile> existingProfile = studentProfileRepository.findByUserIdWithProgramAndCurriculum(existingUser.get().getId());
            if (existingProfile.isPresent()) {
                StudentProfile ep = existingProfile.get();
                if (ep.getAdmissionApplicationId() == null) {
                    ep.setAdmissionApplicationId(app.getId());
                    studentProfileRepository.save(ep);
                }
                if (!Boolean.TRUE.equals(app.isEnrolled())) {
                    app.markAsEnrolled(ep.getId());
                    admissionApplicationRepository.save(app);
                }
                return mapToProfileResponse(ep);
            }
        }

        String studentNumber = generateStudentNumberForAdmission();
        String baseUsername = email.contains("@") ? email.substring(0, email.indexOf("@")).replaceAll("[^a-zA-Z0-9_]", "") : app.getApplicationNumber().toLowerCase().replace("-", "_");
        if (baseUsername.isBlank()) {
            baseUsername = app.getApplicationNumber().toLowerCase().replace("-", "_");
        }
        String username = baseUsername;
        int suffixIndex = 1;
        while (userRepository.existsByUsername(username)) {
            username = baseUsername + "_" + suffixIndex++;
        }

        Program program = app.getTargetProgram();
        Curriculum curriculum = curriculumRepository.findByProgramId(program.getId()).stream()
                .findFirst()
                .orElseGet(() -> curriculumRepository.findAll().stream()
                        .filter(c -> c.getProgram().getId().equals(program.getId()))
                        .findFirst()
                        .orElseThrow(() -> new EntityNotFoundException("No curriculum configured for program: " + program.getCode())));

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("Student123!"))
                .enabled(true)
                .build();
        user.addRole(Roles.STUDENT);
        User savedUser = userRepository.save(user);

        StudentProfile profile = StudentProfile.builder()
                .user(savedUser)
                .studentNumber(studentNumber)
                .firstName(app.getFirstName())
                .middleName(app.getMiddleName())
                .lastName(app.getLastName())
                .suffix(app.getSuffix())
                .program(program)
                .curriculum(curriculum)
                .yearLevel(1)
                .classification(StudentClassification.INCOMING_FIRST_YEAR)
                .enrollmentStatus(StudentProfile.EnrollmentStatus.REGULAR)
                .isGraduating(false)
                .totalUnitsEarned(BigDecimal.ZERO)
                .admissionApplicationId(app.getId())
                .build();

        StudentProfile savedProfile = studentProfileRepository.save(profile);
        app.markAsEnrolled(savedProfile.getId());
        admissionApplicationRepository.save(app);

        copyEquityProfileIfPresent(app, savedProfile);

        log.info("Auto-provisioned student profile {} for incoming first year admission application {}", savedProfile.getStudentNumber(), app.getApplicationNumber());
        return mapToProfileResponse(savedProfile);
    }

    private void copyEquityProfileIfPresent(AdmissionApplication app, StudentProfile savedProfile) {
        if (studentEquityProfileRepository != null && (app.is4psBeneficiary() || app.isIndigenousPeople() 
                || app.isPersonWithDisability() || app.isSoloParent() || app.isRaisedBySoloParent()
                || app.isOrphan() || app.isGidaResident() || app.isFarmerFisherfolk()
                || app.isRebelReturneeFamily() || app.isBottom40IncomeBracket() || app.isFirstGenerationCollege())) {
            if (studentEquityProfileRepository.findByStudentProfileId(savedProfile.getId()).isEmpty()) {
                StudentEquityProfile.DisabilityType mappedDisabilityType = null;
                if (app.getDisabilityType() != null && !app.getDisabilityType().isBlank()) {
                    try {
                        mappedDisabilityType = StudentEquityProfile.DisabilityType.valueOf(app.getDisabilityType().trim().toUpperCase());
                    } catch (Exception ignored) {
                        mappedDisabilityType = StudentEquityProfile.DisabilityType.OTHER;
                    }
                }

                StudentEquityProfile.HouseholdIncomeBracket mappedIncomeBracket = StudentEquityProfile.HouseholdIncomeBracket.POOR_BELOW_10K;
                if (app.getMonthlyHouseholdIncomeBracket() != null && !app.getMonthlyHouseholdIncomeBracket().isBlank()) {
                    try {
                        mappedIncomeBracket = StudentEquityProfile.HouseholdIncomeBracket.valueOf(app.getMonthlyHouseholdIncomeBracket().trim().toUpperCase());
                    } catch (Exception ignored) {
                        mappedIncomeBracket = StudentEquityProfile.HouseholdIncomeBracket.POOR_BELOW_10K;
                    }
                }

                StudentEquityProfile equityProfile = StudentEquityProfile.builder()
                        .studentProfile(savedProfile)
                        .is4psBeneficiary(app.is4psBeneficiary())
                        .household4psIdNumber(app.getHousehold4psIdNumber())
                        .isIndigenousPeople(app.isIndigenousPeople())
                        .ipEthnicGroup(app.getIpEthnicGroup())
                        .ncipCertificateNumber(app.getNcipCertificateNumber())
                        .isPersonWithDisability(app.isPersonWithDisability())
                        .disabilityType(mappedDisabilityType)
                        .pwdIdNumber(app.getPwdIdNumber())
                        .isSoloParent(app.isSoloParent())
                        .isRaisedBySoloParent(app.isRaisedBySoloParent())
                        .soloParentIdNumber(app.getSoloParentIdNumber())
                        .isOrphan(app.isOrphan())
                        .isGidaResident(app.isGidaResident())
                        .gidaBarangayResidence(app.getGidaBarangayResidence())
                        .isFarmerFisherfolk(app.isFarmerFisherfolk())
                        .rsbsaRegistrationNumber(app.getRsbsaRegistrationNumber())
                        .isRebelReturneeFamily(app.isRebelReturneeFamily())
                        .certificateOfSurrenderNumber(app.getCertificateOfSurrenderNumber())
                        .isBottom40IncomeBracket(app.isBottom40IncomeBracket())
                        .monthlyHouseholdIncomeBracket(mappedIncomeBracket)
                        .isFirstGenerationCollege(app.isFirstGenerationCollege())
                        .verificationStatus(StudentEquityProfile.EquityVerificationStatus.SELF_DECLARED)
                        .build();

                studentEquityProfileRepository.save(equityProfile);
            }
        }
    }

    private String generateStudentNumberForAdmission() {
        int year = java.time.LocalDate.now().getYear();
        long count = studentProfileRepository.count() + 1;
        String candidate = String.format("%d-%04d", year, count);
        while (studentProfileRepository.existsByStudentNumber(candidate)) {
            count++;
            candidate = String.format("%d-%04d", year, count);
        }
        return candidate;
    }

    @Transactional
    public StudentProfileResponse getStudentById(Long id) {
        if (id != null && id < 0) {
            return createStudentFromAdmissionAppId(-id);
        }

        StudentProfile profile = Optional.ofNullable(studentProfileL2CacheService.findById(id))
                .orElseThrow(() -> new EntityNotFoundException("Student not found with ID: " + id));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            academicScopeAssertionService.validateStudentAccess(scope, profile);
        }

        return mapToProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public Optional<Long> findExistingStudentProfileIdFromAdmissionAppId(Long appId) {
        if (appId == null) {
            return Optional.empty();
        }
        Optional<AdmissionApplication> appOpt = admissionApplicationRepository.findById(appId);
        if (appOpt.isEmpty()) {
            return Optional.empty();
        }
        AdmissionApplication app = appOpt.get();
        if (app.getEmail() != null && !app.getEmail().isBlank()) {
            Optional<User> userOpt = userRepository.findByEmail(app.getEmail().trim().toLowerCase());
            if (userOpt.isPresent()) {
                Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(userOpt.get().getId());
                if (profileOpt.isPresent()) {
                    return Optional.of(profileOpt.get().getId());
                }
            }
        }
        Optional<StudentProfile> profileByNumber = studentProfileRepository.findByStudentNumber(app.getApplicationNumber());
        return profileByNumber.map(StudentProfile::getId);
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
        List<StudentSearchResultDto> results = new ArrayList<>(profiles.stream().map(sp -> new StudentSearchResultDto(
                sp.getId(),
                sp.getStudentNumber(),
                sp.getFullName() != null && !sp.getFullName().isBlank() ? sp.getFullName() : (sp.getUser() != null ? sp.getUser().getUsername() : "Student " + sp.getStudentNumber()),
                sp.getProgram() != null ? sp.getProgram().getCode() : "BSIT",
                sp.getYearLevel(),
                sp.getClassification() != null ? sp.getClassification().name() : (sp.getEnrollmentStatus() != null ? sp.getEnrollmentStatus().name() : "REGULAR")
        )).toList());

        // Also search Admission Applications for Incoming First Years
        boolean canSearchAdmissions = auth == null || auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_REGISTRAR") || a.getAuthority().equals("ROLE_DEAN") || a.getAuthority().equals("ROLE_CHAIRPERSON"));

        if (canSearchAdmissions) {
            List<AdmissionApplication> matchingApps = admissionApplicationRepository.searchKeyword(cleanQuery);
            for (AdmissionApplication app : matchingApps) {
                if (Boolean.TRUE.equals(app.isEnrolled()) || app.getApplicationStatus() == AdmissionApplication.ApplicationStatus.ENROLLED || app.getStudentProfileId() != null) {
                    continue;
                }
                String appEmail = app.getEmail() != null ? app.getEmail().trim().toLowerCase() : "";
                boolean alreadyExists = profiles.stream().anyMatch(p ->
                        (p.getAdmissionApplicationId() != null && p.getAdmissionApplicationId().equals(app.getId())) ||
                        p.getStudentNumber().equalsIgnoreCase(app.getApplicationNumber()) ||
                        (p.getUser() != null && p.getUser().getEmail() != null && p.getUser().getEmail().equalsIgnoreCase(appEmail))
                );

                if (!alreadyExists) {
                    String displayName = app.getFullName() != null && !app.getFullName().isBlank()
                            ? app.getFullName()
                            : (app.getFirstName() + " " + app.getLastName());
                    results.add(new StudentSearchResultDto(
                            -app.getId(),
                            app.getApplicationNumber(),
                            displayName,
                            app.getTargetProgram() != null ? app.getTargetProgram().getCode() : "BSIT",
                            1,
                            "INCOMING_FIRST_YEAR"
                    ));
                }
            }
        }

        return results;
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
        StudentProfile profile = Optional.ofNullable(studentProfileL2CacheService.findByUserId(userId))
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found for user ID: " + userId));
        return mapToProfileResponse(profile);
    }
}
