package com.sdt.web_app.service.faculty;

import com.sdt.web_app.dto.faculty.FacultyDtos.*;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.faculty.FacultyProfile;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.scheduling.ClassSchedule;
import com.sdt.web_app.entities.scheduling.FacultyWorkload;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.faculty.FacultyProfileRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.repositories.scheduling.ClassScheduleRepository;
import com.sdt.web_app.repositories.scheduling.FacultyWorkloadRepository;
import com.sdt.web_app.exceptions.UserAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.repositories.institution.DepartmentRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import com.sdt.web_app.service.security.AcademicScopeAssertionService;
import com.sdt.web_app.service.security.AcademicScopeContext;
import com.sdt.web_app.specifications.FacultyProfileSpecifications;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacultyProfileService {

    private final FacultyProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final TermRepository termRepository;
    private final com.sdt.web_app.service.institution.TermService termService;
    private final FacultyWorkloadRepository workloadRepository;
    private final ClassScheduleRepository scheduleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AcademicScopeAssertionService academicScopeAssertionService;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final com.sdt.web_app.config.WebSocketBroadcastService broadcastService;

    @Transactional
    @CacheEvict(value = "facultyProfileByUser", allEntries = true)
    public FacultyProfileResponse createFacultyAccount(CreateFacultyAccountRequest request) {
        String trimmedUsername = request.username().trim();
        String trimmedEmail = request.email().trim().toLowerCase();
        String trimmedFacultyId = request.facultyIdNumber().trim();

        if (userRepository.existsByUsername(trimmedUsername)) {
            throw new UserAlreadyExistsException("Username '" + trimmedUsername + "' is already in use.");
        }
        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new UserAlreadyExistsException("Email '" + trimmedEmail + "' is already in use.");
        }
        if (profileRepository.existsByFacultyIdNumber(trimmedFacultyId)) {
            throw new IllegalStateException("Faculty ID number '" + trimmedFacultyId + "' is already assigned.");
        }

        String rawPassword = (request.password() != null && !request.password().isBlank())
                ? request.password().trim()
                : "Faculty123!";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Department assignedCollege = null;
        Program assignedProgram = null;
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            if (scope.isDean() && departmentRepository != null) {
                assignedCollege = departmentRepository.findById(scope.collegeId()).orElse(null);
                if (request.programId() != null && programRepository != null) {
                    Program prog = programRepository.findById(request.programId()).orElse(null);
                    if (prog != null) {
                        Long progCollegeId = academicScopeAssertionService.resolveProgramCollegeId(prog);
                        if (progCollegeId != null && progCollegeId.equals(scope.collegeId())) {
                            assignedProgram = prog;
                        } else {
                            throw new AccessDeniedException("Access Denied: Program is outside your assigned College.");
                        }
                    }
                }
            } else if (scope.isChairperson()) {
                if (departmentRepository != null) {
                    assignedCollege = departmentRepository.findById(scope.collegeId()).orElse(null);
                }
                if (programRepository != null) {
                    assignedProgram = programRepository.findById(scope.programId()).orElse(null);
                }
            } else {
                // ADMIN or REGISTRAR: System-wide unrestricted scope
                if (request.collegeId() != null && departmentRepository != null) {
                    assignedCollege = departmentRepository.findById(request.collegeId())
                            .orElseThrow(() -> new EntityNotFoundException("College not found with ID: " + request.collegeId()));
                }
                if (request.programId() != null && programRepository != null) {
                    assignedProgram = programRepository.findById(request.programId())
                            .orElseThrow(() -> new EntityNotFoundException("Program not found with ID: " + request.programId()));
                }
                if (assignedProgram != null && assignedCollege == null && academicScopeAssertionService != null && departmentRepository != null) {
                    Long progCollegeId = academicScopeAssertionService.resolveProgramCollegeId(assignedProgram);
                    if (progCollegeId != null) {
                        assignedCollege = departmentRepository.findById(progCollegeId).orElse(null);
                    }
                }
                if (assignedCollege != null && assignedProgram != null && academicScopeAssertionService != null) {
                    Long progCollegeId = academicScopeAssertionService.resolveProgramCollegeId(assignedProgram);
                    if (progCollegeId != null && !progCollegeId.equals(assignedCollege.getId())) {
                        throw new IllegalArgumentException("Selected program does not belong to the selected college.");
                    }
                }
            }
        } else {
            if (request.collegeId() != null && departmentRepository != null) {
                assignedCollege = departmentRepository.findById(request.collegeId()).orElse(null);
            }
            if (request.programId() != null && programRepository != null) {
                assignedProgram = programRepository.findById(request.programId()).orElse(null);
            }
        }

        User user = User.builder()
                .username(trimmedUsername)
                .email(trimmedEmail)
                .password(passwordEncoder.encode(rawPassword))
                .college(assignedCollege)
                .program(assignedProgram)
                .enabled(true)
                .build();
        user.addRole(Roles.FACULTY);
        User savedUser = userRepository.save(user);

        FacultyProfile.HighestDegree degree = FacultyProfile.HighestDegree.valueOf(request.highestDegree().trim().toUpperCase());
        FacultyProfile.AcademicRank rank = FacultyProfile.AcademicRank.valueOf(request.academicRank().trim().toUpperCase());
        FacultyProfile.EmploymentStatus status = FacultyProfile.EmploymentStatus.valueOf(request.employmentStatus().trim().toUpperCase());

        FacultyProfile profile = FacultyProfile.builder()
                .user(savedUser)
                .college(assignedCollege)
                .program(assignedProgram)
                .facultyIdNumber(trimmedFacultyId)
                .firstName(request.firstName() != null && !request.firstName().isBlank() ? request.firstName().trim() : null)
                .middleName(request.middleName() != null && !request.middleName().isBlank() ? request.middleName().trim() : null)
                .lastName(request.lastName() != null && !request.lastName().isBlank() ? request.lastName().trim() : null)
                .suffix(request.suffix() != null && !request.suffix().isBlank() ? request.suffix().trim() : null)
                .highestDegree(degree)
                .academicRank(rank)
                .prcLicenseNo(request.prcLicenseNo() != null ? request.prcLicenseNo().trim() : null)
                .employmentStatus(status)
                .isTenured(request.isTenured())
                .build();

        savedUser.assignFacultyProfile(profile);
        FacultyProfile savedProfile = profileRepository.save(profile);
        log.info("Provisioned new faculty account: {} ({}) for user ID {}",
                savedProfile.getFacultyIdNumber(), savedUser.getUsername(), savedUser.getId());
        broadcastFacultyProfile(savedProfile);

        return mapToProfileResponse(savedProfile);
    }

    @Transactional(readOnly = true)
    public List<FacultyProfileResponse> getAllFacultyProfiles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            if (scope != null && scope.isDean()) {
                return profileRepository.findAll(FacultyProfileSpecifications.inCollege(scope.collegeId())).stream()
                        .map(this::mapToProfileResponse).toList();
            } else if (scope != null && scope.isChairperson()) {
                return profileRepository.findAll(FacultyProfileSpecifications.inProgram(scope.programId())).stream()
                        .map(this::mapToProfileResponse).toList();
            }
        }
        return profileRepository.findAllWithUser().stream()
                .map(this::mapToProfileResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<FacultyProfileResponse> getAllFacultyProfiles(org.springframework.data.domain.Pageable pageable) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            if (scope != null && scope.isDean()) {
                return profileRepository.findAll(FacultyProfileSpecifications.inCollege(scope.collegeId()), pageable)
                        .map(this::mapToProfileResponse);
            } else if (scope != null && scope.isChairperson()) {
                return profileRepository.findAll(FacultyProfileSpecifications.inProgram(scope.programId()), pageable)
                        .map(this::mapToProfileResponse);
            }
        }
        return profileRepository.findAll((Specification<FacultyProfile>) null, pageable)
                .map(this::mapToProfileResponse);
    }

    @Transactional
    @Cacheable(value = "facultyProfileByUser", key = "#userId")
    public FacultyProfileResponse getProfileByUserId(Long userId) {
        FacultyProfile profile = profileRepository.findByUserIdWithUser(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
                    FacultyProfile defaultProfile = FacultyProfile.builder()
                            .user(user)
                            .facultyIdNumber("FAC-" + user.getId())
                            .highestDegree(FacultyProfile.HighestDegree.BACHELORS)
                            .academicRank(FacultyProfile.AcademicRank.INSTRUCTOR_I)
                            .employmentStatus(FacultyProfile.EmploymentStatus.FULL_TIME)
                            .isTenured(false)
                            .build();
                    return profileRepository.save(defaultProfile);
                });

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            if (scope != null && scope.isDean()) {
                Long targetCollegeId = profile.getCollege() != null ? profile.getCollege().getId() : (profile.getUser() != null && profile.getUser().getCollege() != null ? profile.getUser().getCollege().getId() : null);
                academicScopeAssertionService.validateCollegeMutation(scope, targetCollegeId);
            } else if (scope != null && scope.isChairperson()) {
                Long targetProgramId = profile.getProgram() != null ? profile.getProgram().getId() : (profile.getUser() != null && profile.getUser().getProgram() != null ? profile.getUser().getProgram().getId() : null);
                academicScopeAssertionService.validateProgramMutation(scope, targetProgramId);
            } else if (scope != null && scope.isFaculty() && !scope.userId().equals(userId)) {
                throw new AccessDeniedException("Access Denied: Faculty can only view their own profile.");
            }
        }

        return mapToProfileResponse(profile);
    }

    @Transactional
    @org.springframework.cache.annotation.Caching(evict = {
            @CacheEvict(value = "facultyProfiles", key = "#userId"),
            @CacheEvict(value = "facultyProfileByUser", key = "#userId")
    })
    public FacultyProfileResponse updateProfile(Long userId, UpdateFacultyProfileRequest request) {
        FacultyProfile profile = profileRepository.findByUserIdWithUser(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
                    FacultyProfile defaultProfile = FacultyProfile.builder()
                            .user(user)
                            .facultyIdNumber("FAC-" + user.getId())
                            .build();
                    return profileRepository.save(defaultProfile);
                });

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser") && academicScopeAssertionService != null) {
            AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(auth);
            if (scope != null && scope.isDean()) {
                Long targetCollegeId = profile.getCollege() != null ? profile.getCollege().getId() : (profile.getUser().getCollege() != null ? profile.getUser().getCollege().getId() : null);
                academicScopeAssertionService.validateCollegeMutation(scope, targetCollegeId);
            } else if (scope != null && scope.isChairperson()) {
                Long targetProgramId = profile.getProgram() != null ? profile.getProgram().getId() : (profile.getUser().getProgram() != null ? profile.getUser().getProgram().getId() : null);
                academicScopeAssertionService.validateProgramMutation(scope, targetProgramId);
            }
        }

        FacultyProfile.HighestDegree degree = FacultyProfile.HighestDegree.valueOf(request.highestDegree().trim().toUpperCase());
        FacultyProfile.AcademicRank rank = FacultyProfile.AcademicRank.valueOf(request.academicRank().trim().toUpperCase());
        FacultyProfile.EmploymentStatus status = FacultyProfile.EmploymentStatus.valueOf(request.employmentStatus().trim().toUpperCase());

        profile.updateCredentials(degree, rank, request.prcLicenseNo(), status, request.isTenured());
        if (request.firstName() != null || request.middleName() != null || request.lastName() != null || request.suffix() != null) {
            profile.updateName(
                    request.firstName() != null ? request.firstName().trim() : profile.getFirstName(),
                    request.middleName() != null ? request.middleName().trim() : profile.getMiddleName(),
                    request.lastName() != null ? request.lastName().trim() : profile.getLastName(),
                    request.suffix() != null ? request.suffix().trim() : profile.getSuffix()
            );
        }

        if (request.collegeId() != null && departmentRepository != null) {
            Department college = departmentRepository.findById(request.collegeId())
                    .orElseThrow(() -> new EntityNotFoundException("College not found with ID: " + request.collegeId()));
            profile.assignCollege(college);
            if (profile.getUser() != null) {
                profile.getUser().assignCollege(college);
            }
        }

        if (request.programId() != null && programRepository != null) {
            Program program = programRepository.findById(request.programId())
                    .orElseThrow(() -> new EntityNotFoundException("Program not found with ID: " + request.programId()));
            profile.assignProgram(program);
            if (profile.getUser() != null) {
                profile.getUser().assignProgram(program);
            }
        }

        FacultyProfile saved = profileRepository.save(profile);
        log.info("Updated faculty credentials for user ID {}", userId);
        broadcastFacultyProfile(saved);

        return mapToProfileResponse(saved);
    }

    @Transactional(readOnly = true)
    public ChedE5ReportResponse generateChedE5Report(Long termId) {
        Term term = termService.getTermById(termId);

        List<User> facultyUsers = userRepository.findAll().stream()
                .filter(u -> u.isEnabled() && u.getRoles().stream()
                        .anyMatch(r -> r == Roles.FACULTY || r == Roles.CHAIRPERSON || r == Roles.DEAN))
                .toList();

        List<FacultyProfile> profiles = profileRepository.findAllWithUser();
        Map<Long, FacultyProfile> profileMap = profiles.stream()
                .collect(Collectors.toMap(fp -> fp.getUser().getId(), fp -> fp, (a, b) -> a));

        List<FacultyWorkload> workloads = workloadRepository.findByTermId(termId);
        Map<Long, FacultyWorkload> workloadMap = workloads.stream()
                .collect(Collectors.toMap(w -> w.getFaculty().getId(), w -> w, (a, b) -> a));

        List<ClassSchedule> termSchedules = scheduleRepository.findAll().stream()
                .filter(s -> s.getSection().getTerm().getId().equals(termId) && s.getInstructor() != null)
                .toList();
        Map<Long, List<ClassSchedule>> schedulesByFaculty = termSchedules.stream()
                .collect(Collectors.groupingBy(s -> s.getInstructor().getId()));

        List<ChedE5WorkloadSummaryDto> summaryList = new ArrayList<>();
        BigDecimal totalRegular = BigDecimal.ZERO;
        BigDecimal totalOverload = BigDecimal.ZERO;
        BigDecimal totalContact = BigDecimal.ZERO;

        for (User faculty : facultyUsers) {
            FacultyProfile fp = profileMap.get(faculty.getId());
            FacultyWorkload fw = workloadMap.get(faculty.getId());
            List<ClassSchedule> scheds = schedulesByFaculty.getOrDefault(faculty.getId(), List.of());

            List<String> assignedSectionCodes = scheds.stream()
                    .map(s -> s.getSection().getSectionCode())
                    .distinct()
                    .toList();

            BigDecimal regUnits = fw != null ? fw.getRegularUnits() : BigDecimal.ZERO;
            BigDecimal ovlUnits = fw != null ? fw.getOverloadUnits() : BigDecimal.ZERO;
            BigDecimal contactHours = fw != null ? fw.getTotalContactHours() : BigDecimal.ZERO;
            int preps = fw != null ? fw.getNumberOfPreparations() : 0;

            totalRegular = totalRegular.add(regUnits);
            totalOverload = totalOverload.add(ovlUnits);
            totalContact = totalContact.add(contactHours);

            summaryList.add(new ChedE5WorkloadSummaryDto(
                    faculty.getId(),
                    fp != null ? fp.getFacultyIdNumber() : "FAC-" + faculty.getId(),
                    fp != null && fp.getFullName() != null && !fp.getFullName().isBlank() ? fp.getFullName() : faculty.getUsername(),
                    faculty.getEmail(),
                    fp != null ? fp.getHighestDegree().name() : "BACHELORS",
                    fp != null ? fp.getAcademicRank().name() : "INSTRUCTOR_I",
                    fp != null ? fp.getPrcLicenseNo() : null,
                    fp != null ? fp.getEmploymentStatus().name() : "FULL_TIME",
                    fp != null && fp.isTenured(),
                    regUnits,
                    ovlUnits,
                    contactHours,
                    preps,
                    assignedSectionCodes
            ));
        }

        return new ChedE5ReportResponse(
                term.getId(),
                term.getAcademicYear().getCode() + " " + term.getTermType().name(),
                facultyUsers.size(),
                totalRegular,
                totalOverload,
                totalContact,
                summaryList
        );
    }

    private FacultyProfileResponse mapToProfileResponse(FacultyProfile fp) {
        Long collegeId = fp.getCollege() != null ? fp.getCollege().getId() : (fp.getUser() != null && fp.getUser().getCollege() != null ? fp.getUser().getCollege().getId() : null);
        String collegeCode = fp.getCollege() != null ? fp.getCollege().getCode() : (fp.getUser() != null && fp.getUser().getCollege() != null ? fp.getUser().getCollege().getCode() : null);
        String collegeName = fp.getCollege() != null ? fp.getCollege().getName() : (fp.getUser() != null && fp.getUser().getCollege() != null ? fp.getUser().getCollege().getName() : null);

        Long programId = fp.getProgram() != null ? fp.getProgram().getId() : (fp.getUser() != null && fp.getUser().getProgram() != null ? fp.getUser().getProgram().getId() : null);
        String programCode = fp.getProgram() != null ? fp.getProgram().getCode() : (fp.getUser() != null && fp.getUser().getProgram() != null ? fp.getUser().getProgram().getCode() : null);
        String programName = fp.getProgram() != null ? fp.getProgram().getName() : (fp.getUser() != null && fp.getUser().getProgram() != null ? fp.getUser().getProgram().getName() : null);

        return new FacultyProfileResponse(
                fp.getId(),
                fp.getUser().getId(),
                fp.getUser().getUsername(),
                fp.getUser().getEmail(),
                fp.getFacultyIdNumber(),
                fp.getFirstName(),
                fp.getMiddleName(),
                fp.getLastName(),
                fp.getSuffix(),
                fp.getFullName(),
                fp.getHighestDegree().name(),
                fp.getAcademicRank().name(),
                fp.getPrcLicenseNo(),
                fp.getEmploymentStatus().name(),
                fp.isTenured(),
                collegeId,
                collegeCode,
                collegeName,
                programId,
                programCode,
                programName
        );
    }

    private void broadcastFacultyProfile(FacultyProfile fp) {
        if (fp == null || broadcastService == null || fp.getUser() == null) return;
        Long facultyUserId = fp.getUser().getId();
        com.sdt.web_app.websocket.dto.FacultyWorkloadMessage msg = new com.sdt.web_app.websocket.dto.FacultyWorkloadMessage(
                facultyUserId,
                null,
                0.0,
                0,
                null,
                fp.getEmploymentStatus() != null ? fp.getEmploymentStatus().name() : "ACTIVE",
                java.time.Instant.now()
        );
        broadcastService.broadcast(com.sdt.web_app.config.WebSocketTopics.faculty(facultyUserId), msg);
    }
}
