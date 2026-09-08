package com.sdt.web_app.service.enrollment;

import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component("enrollmentSecurity")
@RequiredArgsConstructor
@Slf4j
public class EnrollmentSecurity {

    private final StudentProfileRepository studentProfileRepository;
    private final SecurityUtils securityUtils;

    private static final Set<String> ADVISING_STAFF_ROLES = Set.of(
            "ROLE_ADMIN", "ROLE_REGISTRAR", "ROLE_DEAN", "ROLE_CHAIRPERSON", "ROLE_FACULTY"
    );

    private static final Set<String> ENLISTMENT_STAFF_ROLES = Set.of(
            "ROLE_ADMIN", "ROLE_REGISTRAR"
    );

    /**
     * Verifies if the authenticated caller can view advising for the given student.
     * Institutional staff can view any student; students can only view their own profile.
     */
    public boolean canAccessStudentAdvising(Authentication authentication, Long studentId) {
        if (authentication == null || studentId == null) {
            return false;
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        for (String staffRole : ADVISING_STAFF_ROLES) {
            if (authorities.contains(staffRole)) {
                return true;
            }
        }

        if (authorities.contains("ROLE_STUDENT")) {
            return isStudentOwner(authentication, studentId);
        }

        return false;
    }

    /**
     * Verifies if the authenticated caller can perform enlistment, drop, confirmation, or view enrollment.
     * Admin/Registrar can manage any student; students can only manage their own enrollment.
     */
    public boolean canAccessStudentEnrollment(Authentication authentication, Long studentId) {
        if (authentication == null || studentId == null) {
            return false;
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        for (String staffRole : ENLISTMENT_STAFF_ROLES) {
            if (authorities.contains(staffRole)) {
                return true;
            }
        }

        if (authorities.contains("ROLE_STUDENT")) {
            return isStudentOwner(authentication, studentId);
        }

        return false;
    }

    private boolean isStudentOwner(Authentication authentication, Long studentId) {
        Long userId = securityUtils.resolveUserId(authentication);
        if (userId == null) {
            log.warn("Could not resolve user ID for authenticated student principal: {}", authentication.getName());
            return false;
        }

        Optional<StudentProfile> profileOpt = studentProfileRepository.findByUserId(userId);
        if (profileOpt.isEmpty()) {
            log.warn("No StudentProfile found for user ID: {}", userId);
            return false;
        }

        boolean matches = profileOpt.get().getId().equals(studentId);
        if (!matches) {
            log.warn("IDOR check failed: User ID {} (Student Profile ID {}) attempted to access Student Profile ID {}",
                    userId, profileOpt.get().getId(), studentId);
        }
        return matches;
    }
}
