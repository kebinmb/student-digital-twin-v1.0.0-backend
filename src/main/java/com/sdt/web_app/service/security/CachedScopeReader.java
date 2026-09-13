package com.sdt.web_app.service.security;

import com.sdt.web_app.entities.faculty.FacultyProfile;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.service.institution.TermService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CachedScopeReader {

    public static final String CACHED_FACULTY_PROFILE_ATTR = "SDT_CACHED_FACULTY_PROFILE";

    private final SecurityProfileCache securityProfileCache;
    private final TermService termService;

    @Cacheable(value = "facultyProfiles", key = "#userId", unless = "#result == null")
    public Optional<FacultyProfile> findByUserId(Long userId) {
        return securityProfileCache.getFacultyProfile(userId);
    }

    @Cacheable(value = "terms", key = "#termId", unless = "#result == null")
    public Optional<Term> findById(Long termId) {
        if (termId == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(termService.getTermById(termId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
