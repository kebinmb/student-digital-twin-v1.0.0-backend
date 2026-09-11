package com.sdt.web_app.service.security;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.faculty.FacultyProfile;
import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.DepartmentType;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.faculty.FacultyProfileRepository;
import com.sdt.web_app.repositories.institution.DepartmentRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
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
public class AcademicScopeAssertionService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final ClassSectionRepository classSectionRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final SecurityUtils securityUtils;

    public AcademicScopeContext assertAndResolveScope(Authentication authentication) {
        if (authentication == null) {
            throw new AccessDeniedException("Authentication is required.");
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        Long userId = securityUtils.resolveUserId(authentication);
        if (userId == null) {
            throw new AccessDeniedException("Unable to resolve authenticated user identity.");
        }

        // 1. ADMIN and REGISTRAR: System-wide unrestricted academic scope
        if (authorities.contains("ROLE_ADMIN") || authorities.contains("ROLE_REGISTRAR")) {
            return AcademicScopeContext.unrestricted(userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException("User record not found."));

        // 2. DEAN: Mandatory assignment chain: Account -> college_id
        if (authorities.contains("ROLE_DEAN")) {
            Long collegeId = null;
            if (user.getCollege() != null) {
                collegeId = user.getCollege().getId();
            } else {
                List<Department> colleges = departmentRepository.findByDeanUserId(userId);
                if (!colleges.isEmpty()) {
                    collegeId = colleges.get(0).getId();
                }
            }

            if (collegeId == null) {
                log.warn("DEAN user {} is not linked to any College.", userId);
                throw new AccessDeniedException("Account not linked to an active College.");
            }

            // Resolve all programs under this college (both direct and child departments)
            List<Long> deptIds = new ArrayList<>();
            deptIds.add(collegeId);
            List<Department> children = departmentRepository.findByParentDepartmentId(collegeId);
            for (Department child : children) {
                deptIds.add(child.getId());
            }

            List<Program> programs = programRepository.findByDepartmentIdIn(deptIds);
            List<Long> allowedProgramIds = programs.stream().map(Program::getId).toList();

            log.debug("Resolved DEAN scope for user {}: collegeId={}, allowedPrograms={}", userId, collegeId, allowedProgramIds);
            return AcademicScopeContext.dean(userId, collegeId, allowedProgramIds);
        }

        // 3. CHAIRPERSON: Mandatory assignment chain: Account -> college_id AND program_id
        if (authorities.contains("ROLE_CHAIRPERSON")) {
            Long collegeId = user.getCollege() != null ? user.getCollege().getId() : null;
            Long programId = user.getProgram() != null ? user.getProgram().getId() : null;

            if (programId == null) {
                Optional<Program> progOpt = programRepository.findFirstByChairpersonUserId(userId);
                if (progOpt.isPresent()) {
                    Program p = progOpt.get();
                    programId = p.getId();
                    if (collegeId == null) {
                        collegeId = resolveProgramCollegeId(p);
                    }
                }
            } else if (collegeId == null) {
                Program p = programRepository.findById(programId).orElse(null);
                if (p != null) {
                    collegeId = resolveProgramCollegeId(p);
                }
            }

            if (collegeId == null || programId == null) {
                log.warn("CHAIRPERSON user {} is missing college or program linkage: collegeId={}, programId={}",
                        userId, collegeId, programId);
                throw new AccessDeniedException("Account not linked to an active College and Program.");
            }

            // Verify program actually belongs to the assigned college
            Program assignedProg = programRepository.findById(programId).orElse(null);
            if (assignedProg != null) {
                Long actualCollegeId = resolveProgramCollegeId(assignedProg);
                if (actualCollegeId != null && !actualCollegeId.equals(collegeId)) {
                    log.warn("CHAIRPERSON user {} has mismatched collegeId {} vs program's collegeId {}",
                            userId, collegeId, actualCollegeId);
                    throw new AccessDeniedException("Account not linked to an active College and Program.");
                }
            }

            log.debug("Resolved CHAIRPERSON scope for user {}: collegeId={}, programId={}", userId, collegeId, programId);
            return AcademicScopeContext.chairperson(userId, collegeId, programId);
        }

        // 4. FACULTY: Scoped to assigned load & sections
        if (authorities.contains("ROLE_FACULTY")) {
            FacultyProfile fp = user.getFacultyProfile() != null
                    ? user.getFacultyProfile()
                    : (facultyProfileRepository != null ? facultyProfileRepository.findByUserId(userId).orElse(null) : null);
            Long collegeId = user.getCollege() != null ? user.getCollege().getId() : (fp != null && fp.getCollege() != null ? fp.getCollege().getId() : null);
            Long programId = user.getProgram() != null ? user.getProgram().getId() : (fp != null && fp.getProgram() != null ? fp.getProgram().getId() : null);

            List<Long> assignedSections = classSectionRepository.findAssignedSectionIdsByInstructor(userId);

            log.debug("Resolved FACULTY scope for user {}: collegeId={}, programId={}, assignedSections={}",
                    userId, collegeId, programId, assignedSections);
            return AcademicScopeContext.faculty(userId, collegeId, programId, assignedSections);
        }

        return AcademicScopeContext.other(userId);
    }

    public Long resolveProgramCollegeId(Program program) {
        if (program == null) return null;
        if (program.getCollege() != null) {
            return program.getCollege().getId();
        }
        Department dept = program.getDepartment();
        if (dept == null) return null;
        if (dept.getType() == DepartmentType.COLLEGE) {
            return dept.getId();
        }
        if (dept.getParentDepartment() != null) {
            return dept.getParentDepartment().getId();
        }
        return dept.getId();
    }

    public void validateCollegeMutation(AcademicScopeContext scope, Long targetCollegeId) {
        if (scope.isUnrestricted()) return;
        if (scope.isDean() || scope.isChairperson()) {
            if (targetCollegeId == null || !scope.collegeId().equals(targetCollegeId)) {
                throw new AccessDeniedException("Access Denied: Target entity does not belong to your assigned College.");
            }
        }
    }

    public void validateProgramMutation(AcademicScopeContext scope, Long targetProgramId) {
        if (scope.isUnrestricted()) return;
        if (scope.isDean()) {
            if (targetProgramId == null || !scope.allowedProgramIds().contains(targetProgramId)) {
                throw new AccessDeniedException("Access Denied: Target program is outside your assigned College.");
            }
        } else if (scope.isChairperson()) {
            if (targetProgramId == null || !scope.programId().equals(targetProgramId)) {
                throw new AccessDeniedException("Access Denied: Target entity does not belong to your assigned Program.");
            }
        } else if (scope.isFaculty()) {
            throw new AccessDeniedException("Access Denied: Faculty cannot perform program modifications.");
        }
    }

    public void validateSectionAccess(AcademicScopeContext scope, ClassSection section) {
        if (scope.isUnrestricted()) return;
        if (section == null) {
            throw new AccessDeniedException("Access Denied: Section not found.");
        }

        if (scope.isDean()) {
            Long secCollegeId = resolveProgramCollegeId(section.getCurriculum().getProgram());
            if (secCollegeId == null || !scope.collegeId().equals(secCollegeId)) {
                throw new AccessDeniedException("Access Denied: Section does not belong to your assigned College.");
            }
        } else if (scope.isChairperson()) {
            Long secProgramId = section.getCurriculum().getProgram().getId();
            if (!scope.programId().equals(secProgramId)) {
                throw new AccessDeniedException("Access Denied: Section does not belong to your assigned Program.");
            }
        } else if (scope.isFaculty()) {
            boolean isAssigned = (section.getPrimaryInstructor() != null && scope.userId().equals(section.getPrimaryInstructor().getId()))
                    || (section.getSchedules() != null && section.getSchedules().stream()
                        .anyMatch(s -> s.getInstructor() != null && scope.userId().equals(s.getInstructor().getId())));
            if (!isAssigned) {
                throw new AccessDeniedException("Access Denied: Not assigned to this class section.");
            }
        }
    }

    public void validateStudentAccess(AcademicScopeContext scope, StudentProfile student) {
        if (scope.isUnrestricted()) return;
        if (student == null) {
            throw new AccessDeniedException("Access Denied: Student not found.");
        }

        if (scope.isDean()) {
            Long progCollegeId = resolveProgramCollegeId(student.getProgram());
            if (progCollegeId == null || !scope.collegeId().equals(progCollegeId)) {
                throw new AccessDeniedException("Access Denied: Student does not belong to your assigned College.");
            }
        } else if (scope.isChairperson()) {
            if (!scope.programId().equals(student.getProgram().getId())) {
                throw new AccessDeniedException("Access Denied: Student does not belong to your assigned Program.");
            }
        } else if (scope.isFaculty()) {
            if (scope.assignedSectionIds().isEmpty()) {
                throw new AccessDeniedException("Access Denied: Faculty has no assigned class sections.");
            }
            boolean isEnrolled = studentProfileRepository.existsEnrolledInSections(student.getId(), scope.assignedSectionIds());
            if (!isEnrolled) {
                throw new AccessDeniedException("Access Denied: Student is not enrolled in your assigned sections.");
            }
        }
    }
}
