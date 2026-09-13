package com.sdt.web_app.service.security;

import com.sdt.web_app.config.CacheConfig;
import com.sdt.web_app.entities.faculty.FacultyProfile;
import com.sdt.web_app.repositories.faculty.FacultyProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FacultyProfileL2CacheService {

    private final FacultyProfileRepository facultyProfileRepository;

    @Cacheable(value = CacheConfig.CACHE_FACULTY_PROFILES, key = "#userId", unless = "#result == null")
    public FacultyProfile findByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return facultyProfileRepository.findByUserIdWithUser(userId).orElse(null);
    }

    @CacheEvict(value = CacheConfig.CACHE_FACULTY_PROFILES, key = "#userId")
    public void evictCache(Long userId) {
        // Cache eviction helper for user profile updates
    }
}
