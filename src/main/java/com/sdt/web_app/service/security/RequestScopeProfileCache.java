package com.sdt.web_app.service.security;

import com.sdt.web_app.entities.faculty.FacultyProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RequestScopeProfileCache {

    private final SecurityProfileCache securityProfileCache;

    public Optional<FacultyProfile> getFacultyProfile(Long userId) {
        return securityProfileCache.getFacultyProfile(userId);
    }
}
