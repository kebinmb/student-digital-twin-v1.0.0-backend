package com.sdt.web_app.service.authentication;

import com.sdt.web_app.dto.authentication.UserDtos.*;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.exceptions.UserAlreadyExistsException;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.repositories.institution.DepartmentRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import com.sdt.web_app.repositories.faculty.FacultyProfileRepository;
import com.sdt.web_app.service.security.AcademicScopeAssertionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final AcademicScopeAssertionService academicScopeAssertionService;
    private final FacultyProfileRepository facultyProfileRepository;

    @Transactional
    public UserDetailResponse createUser(CreateUserRequest request) {
        String trimmedUsername = request.username().trim();
        String trimmedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByUsername(trimmedUsername)) {
            throw new UserAlreadyExistsException("Username '" + trimmedUsername + "' is already in use.");
        }
        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new UserAlreadyExistsException("Email '" + trimmedEmail + "' is already in use.");
        }

        Set<Roles> mappedRoles = parseRoles(request.roles());
        if (mappedRoles.isEmpty()) {
            throw new IllegalArgumentException("At least one valid role must be provided.");
        }

        Department college = null;
        if (request.collegeId() != null && departmentRepository != null) {
            college = departmentRepository.findById(request.collegeId())
                    .orElseThrow(() -> new EntityNotFoundException("College not found with ID: " + request.collegeId()));
        }

        Program program = null;
        if (request.programId() != null && programRepository != null) {
            program = programRepository.findById(request.programId())
                    .orElseThrow(() -> new EntityNotFoundException("Program not found with ID: " + request.programId()));
        }

        if (program != null && college == null && academicScopeAssertionService != null && departmentRepository != null) {
            Long progCollegeId = academicScopeAssertionService.resolveProgramCollegeId(program);
            if (progCollegeId != null) {
                college = departmentRepository.findById(progCollegeId).orElse(null);
            }
        }

        if (college != null && program != null && academicScopeAssertionService != null) {
            Long progCollegeId = academicScopeAssertionService.resolveProgramCollegeId(program);
            if (progCollegeId != null && !progCollegeId.equals(college.getId())) {
                throw new IllegalArgumentException("Selected program does not belong to the selected college.");
            }
        }

        validateRoleScoping(mappedRoles, college, program);

        User user = User.builder()
                .username(trimmedUsername)
                .email(trimmedEmail)
                .password(passwordEncoder.encode(request.password().trim()))
                .enabled(request.enabled() == null || request.enabled())
                .college(college)
                .program(program)
                .build();

        for (Roles role : mappedRoles) {
            user.addRole(role);
        }

        User savedUser = userRepository.save(user);
        log.info("Provisioned new user account: id={}, username={}, roles={}, collegeId={}, programId={}",
                savedUser.getId(), savedUser.getUsername(), savedUser.getRoles(),
                college != null ? college.getId() : null, program != null ? program.getId() : null);

        return mapToUserDetailResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserDetailResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserDetailResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
        return mapToUserDetailResponse(user);
    }

    @Transactional
    public UserDetailResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        if (request.email() != null && !request.email().isBlank()) {
            String newEmail = request.email().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new UserAlreadyExistsException("Email '" + newEmail + "' is already in use.");
            }
            user.updateEmail(newEmail);
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.updatePassword(passwordEncoder.encode(request.password().trim()));
        }

        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        if (request.roles() != null && !request.roles().isEmpty()) {
            Set<Roles> mappedRoles = parseRoles(request.roles());
            user.setRoles(mappedRoles);
        }

        if (Boolean.TRUE.equals(request.clearCollege())) {
            user.assignCollege(null);
        } else if (request.collegeId() != null && departmentRepository != null) {
            Department college = departmentRepository.findById(request.collegeId())
                    .orElseThrow(() -> new EntityNotFoundException("College not found with ID: " + request.collegeId()));
            user.assignCollege(college);
        }

        if (Boolean.TRUE.equals(request.clearProgram())) {
            user.assignProgram(null);
        } else if (request.programId() != null && programRepository != null) {
            Program program = programRepository.findById(request.programId())
                    .orElseThrow(() -> new EntityNotFoundException("Program not found with ID: " + request.programId()));
            user.assignProgram(program);
        }

        if (user.getProgram() != null && user.getCollege() == null && academicScopeAssertionService != null && departmentRepository != null) {
            Long progCollegeId = academicScopeAssertionService.resolveProgramCollegeId(user.getProgram());
            if (progCollegeId != null) {
                user.assignCollege(departmentRepository.findById(progCollegeId).orElse(null));
            }
        }

        if (user.getCollege() != null && user.getProgram() != null && academicScopeAssertionService != null) {
            Long progCollegeId = academicScopeAssertionService.resolveProgramCollegeId(user.getProgram());
            if (progCollegeId != null && !progCollegeId.equals(user.getCollege().getId())) {
                throw new IllegalArgumentException("Selected program does not belong to the selected college.");
            }
        }

        validateRoleScoping(user.getRoles(), user.getCollege(), user.getProgram());

        if (facultyProfileRepository != null) {
            facultyProfileRepository.findByUserId(user.getId()).ifPresent(fp -> {
                fp.assignCollege(user.getCollege());
                fp.assignProgram(user.getProgram());
                facultyProfileRepository.save(fp);
            });
        }

        User updatedUser = userRepository.save(user);
        log.info("Updated user account: id={}, username={}, collegeId={}, programId={}",
                updatedUser.getId(), updatedUser.getUsername(),
                updatedUser.getCollege() != null ? updatedUser.getCollege().getId() : null,
                updatedUser.getProgram() != null ? updatedUser.getProgram().getId() : null);
        return mapToUserDetailResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
        userRepository.delete(user);
        log.info("Admin deleted user account: id={}, username={}", id, user.getUsername());
    }

    private void validateRoleScoping(Set<Roles> roles, Department college, Program program) {
        if (roles == null) return;
        if (roles.contains(Roles.DEAN)) {
            if (college == null) {
                throw new IllegalArgumentException("College assignment is required for users with the DEAN role.");
            }
            if (program != null) {
                throw new IllegalArgumentException("DEAN cannot be assigned to a specific program. Only a college can be assigned.");
            }
        }
        if (roles.contains(Roles.CHAIRPERSON)) {
            if (college == null || program == null) {
                throw new IllegalArgumentException("Both College and Program assignments are required for users with the CHAIRPERSON role.");
            }
        }
    }

    private Set<Roles> parseRoles(Set<String> roleNames) {
        Set<Roles> roles = new HashSet<>();
        if (roleNames == null) return roles;
        for (String name : roleNames) {
            String cleanName = name.trim().toUpperCase();
            if (cleanName.startsWith("ROLE_")) {
                cleanName = cleanName.substring(5);
            }
            try {
                roles.add(Roles.valueOf(cleanName));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role specified: " + name);
            }
        }
        return roles;
    }

    private UserDetailResponse mapToUserDetailResponse(User user) {
        Set<String> roleStrings = user.getRoles().stream()
                .map(Roles::name)
                .collect(Collectors.toSet());

        Long collegeId = user.getCollege() != null ? user.getCollege().getId() : null;
        String collegeCode = user.getCollege() != null ? user.getCollege().getCode() : null;
        String collegeName = user.getCollege() != null ? user.getCollege().getName() : null;

        Long programId = user.getProgram() != null ? user.getProgram().getId() : null;
        String programCode = user.getProgram() != null ? user.getProgram().getCode() : null;
        String programName = user.getProgram() != null ? user.getProgram().getName() : null;

        return new UserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roleStrings,
                user.isEnabled(),
                user.getCreatedAt(),
                collegeId,
                collegeCode,
                collegeName,
                programId,
                programCode,
                programName
        );
    }
}
