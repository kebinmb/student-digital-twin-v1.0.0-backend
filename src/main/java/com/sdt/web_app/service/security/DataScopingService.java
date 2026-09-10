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

        // ADMIN and REGISTRAR have unrestricted institutional-wide scope
        if (roles.contains("ROLE_ADMIN") || roles.contains("ROLE_REGISTRAR")) {
            return Optional.empty();
        }

        Long userId = resolveUserId(authentication);
        if (userId == null) {
            return Optional.empty();
        }

        // DEAN: Scoped to assigned College and child departments
        if (roles.contains("ROLE_DEAN")) {
            List<Department> colleges = departmentRepository.findByDeanUserId(userId);
            List<Long> deptIds = new ArrayList<>();
            for (Department college : colleges) {
                deptIds.add(college.getId());
                List<Department> children = departmentRepository.findByParentDepartmentId(college.getId());
                for (Department child : children) {
                    deptIds.add(child.getId());
                }
            }
            List<Program> programs = deptIds.isEmpty() ? List.of() : programRepository.findByDepartmentIdIn(deptIds);
            List<Long> programIds = programs.stream().map(Program::getId).toList();
            log.debug("DEAN user {} scoped to program IDs: {}", userId, programIds);
            return Optional.of(programIds);
        }

        // CHAIRPERSON: Scoped to assigned Program
        if (roles.contains("ROLE_CHAIRPERSON")) {
            List<Program> programs = programRepository.findByChairpersonUserId(userId);
            List<Long> programIds = programs.stream().map(Program::getId).toList();
            log.debug("CHAIRPERSON user {} scoped to program IDs: {}", userId, programIds);
            return Optional.of(programIds);
        }

        return Optional.empty();
    }
}
