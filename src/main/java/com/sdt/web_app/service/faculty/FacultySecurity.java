package com.sdt.web_app.service.faculty;

import com.sdt.web_app.entities.faculty.FacultyProfile;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.service.security.CachedScopeReader;
import com.sdt.web_app.service.security.SecurityProfileCache;
import com.sdt.web_app.service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("facultySecurity")
@RequiredArgsConstructor
public class FacultySecurity {

    private final SecurityUtils securityUtils;
    private final SecurityProfileCache securityProfileCache;
    private final CachedScopeReader cachedScopeReader;

    public boolean isFacultySelf(Long userId, Authentication authentication) {
        if (userId == null || authentication == null) {
            return false;
        }
        Long currentUserId = securityUtils.resolveUserId(authentication);
        return userId.equals(currentUserId);
    }

    public Optional<FacultyProfile> getFacultyProfile(Long userId) {
        return securityProfileCache.getFacultyProfile(userId);
    }

    public Optional<Term> getTerm(Long termId) {
        return cachedScopeReader.findById(termId);
    }
}
