package com.sdt.web_app.service.security;

import com.sdt.web_app.config.CacheConfig;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentProfileL2CacheService {

    private final StudentProfileRepository studentProfileRepository;

    @Cacheable(value = CacheConfig.CACHE_STUDENT_PROFILE_BY_USER, key = "#userId", unless = "#result == null")
    public StudentProfile findByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        return studentProfileRepository.findByUserIdWithProgramAndCurriculum(userId).orElse(null);
    }

    @Cacheable(value = CacheConfig.CACHE_STUDENT_PROFILES, key = "#id", unless = "#result == null")
    public StudentProfile findById(Long id) {
        if (id == null) {
            return null;
        }
        return studentProfileRepository.findByIdWithProgramAndCurriculum(id).orElse(null);
    }

    @CacheEvict(value = CacheConfig.CACHE_STUDENT_PROFILE_BY_USER, key = "#userId")
    public void evictCacheByUserId(Long userId) {
        // Cache eviction helper for user profile updates
    }

    @CacheEvict(value = CacheConfig.CACHE_STUDENT_PROFILES, key = "#id")
    public void evictCacheById(Long id) {
        // Cache eviction helper for student profile updates
    }
}
