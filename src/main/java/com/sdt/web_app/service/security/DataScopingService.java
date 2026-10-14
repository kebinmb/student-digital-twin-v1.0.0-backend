package com.sdt.web_app.service.security;

import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.repositories.institution.DepartmentRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataScopingService {

    private final SecurityUtils securityUtils;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final com.sdt.web_app.repositories.authentication.UserRepository userRepository;
    private final SecurityProfileCache securityProfileCache;
    private final CachedScopeReader cachedScopeReader;

    public Long resolveUserId(Authentication authentication) {
        return securityUtils.resolveUserId(authentication);
    }

    public boolean isOnlyFaculty(Authentication authentication) {
        if (authentication == null) return false;
        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return roles.contains("ROLE_FACULTY")
                && !roles.contains("ROLE_ADMIN")
                && !roles.contains("ROLE_SUPER_ADMIN")
                && !roles.contains("ROLE_REGISTRAR")
                && !roles.contains("ROLE_DEAN")
                && !roles.contains("ROLE_CHAIRPERSON");
    }

    public Optional<List<Long>> getScopedProgramIds(Authentication authentication) {
        if (authentication == null) {
            return Optional.empty();
        }

        Set<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // ADMIN, SUPER_ADMIN, REGISTRAR, CASHIER, and ACCOUNTANT have unrestricted institutional-wide scope
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_SUPER_ADMIN") || roles.contains("ROLE_REGISTRAR") || roles.contains("ROLE_CASHIER") || roles.contains("ROLE_ACCOUNTANT")) {
            return Optional.empty();
        }

        Long userId = securityUtils.resolveUserId(authentication);
        if (userId == null) {
            return Optional.empty();
        }

        // DEAN: Scoped to assigned College and child departments
        if (roles.contains("ROLE_DEAN")) {
            List<Department> colleges = new ArrayList<>(departmentRepository.findByDeanUserId(userId));
            if (userRepository != null) {
                userRepository.findById(userId).ifPresent(u -> {
                    if (u.getCollege() != null && colleges.stream().noneMatch(c -> c.getId().equals(u.getCollege().getId()))) {
                        colleges.add(u.getCollege());
                    } else if (u.getFacultyProfile() != null && u.getFacultyProfile().getCollege() != null
                            && colleges.stream().noneMatch(c -> c.getId().equals(u.getFacultyProfile().getCollege().getId()))) {
                        colleges.add(u.getFacultyProfile().getCollege());
                    }
                });
            }
            List<Long> deptIds = new ArrayList<>();
            for (Department college : colleges) {
                deptIds.add(college.getId());
                List<Department> children = departmentRepository.findByParentDepartmentId(college.getId());
                for (Department child : children) {
                    deptIds.add(child.getId());
                }
            }
            Set<Long> programIdSet = new java.util.LinkedHashSet<>();
            if (!deptIds.isEmpty()) {
                programRepository.findByDepartmentIdIn(deptIds).forEach(p -> programIdSet.add(p.getId()));
            }
            for (Department college : colleges) {
                programRepository.findByCollegeId(college.getId()).forEach(p -> programIdSet.add(p.getId()));
            }
            List<Long> programIds = new ArrayList<>(programIdSet);
            log.debug("DEAN user {} scoped to program IDs: {}", userId, programIds);
            return Optional.of(programIds);
        }

        // CHAIRPERSON: Scoped to assigned Program
        if (roles.contains("ROLE_CHAIRPERSON")) {
            List<Program> programs = new ArrayList<>(programRepository.findByChairpersonUserId(userId));
            if (userRepository != null) {
                userRepository.findById(userId).ifPresent(u -> {
                    if (u.getProgram() != null && programs.stream().noneMatch(p -> p.getId().equals(u.getProgram().getId()))) {
                        programs.add(u.getProgram());
                    } else if (u.getFacultyProfile() != null && u.getFacultyProfile().getProgram() != null
                            && programs.stream().noneMatch(p -> p.getId().equals(u.getFacultyProfile().getProgram().getId()))) {
                        programs.add(u.getFacultyProfile().getProgram());
                    }
                });
            }
            List<Long> programIds = programs.stream().map(Program::getId).toList();
            log.debug("CHAIRPERSON user {} scoped to program IDs: {}", userId, programIds);
            return Optional.of(programIds);
        }

        return Optional.empty();
    }
}
