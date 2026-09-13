package com.sdt.web_app.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_CAMPUSES = "campuses";
    public static final String CACHE_DEPARTMENTS = "departments";
    public static final String CACHE_PROGRAMS = "programs";
    public static final String CACHE_PROGRAMS_BY_ID = "programsById";
    public static final String CACHE_COURSES = "courses";
    public static final String CACHE_TERMS = "terms";
    public static final String CACHE_TERMS_BY_ID = "termsById";
    public static final String CACHE_ACTIVE_TERMS = "activeTerms";
    public static final String CACHE_ACADEMIC_YEARS = "academicYears";
    public static final String CACHE_GRADING_SCALES = "gradingScales";
    public static final String CACHE_FEE_CATALOG = "feeCatalog";
    public static final String CACHE_EQUITY_PROFILES = "equityProfiles";
    public static final String CACHE_FACULTY_PROFILES = "facultyProfiles";
    public static final String CACHE_FACULTY_PROFILE_BY_USER = "facultyProfileByUser";
    public static final String CACHE_STUDENT_PROFILES = "studentProfiles";
    public static final String CACHE_STUDENT_PROFILE_BY_USER = "studentProfileByUser";
    public static final String CACHE_CURRICULA = "curricula";
    public static final String CACHE_CURRICULA_BY_PROGRAM = "curriculaByProgram";

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager() {
            private static final List<String> INITIAL_NAMES = List.of(
                    CACHE_CAMPUSES,
                    CACHE_DEPARTMENTS,
                    CACHE_PROGRAMS,
                    CACHE_PROGRAMS_BY_ID,
                    CACHE_COURSES,
                    CACHE_TERMS,
                    CACHE_TERMS_BY_ID,
                    CACHE_ACTIVE_TERMS,
                    CACHE_ACADEMIC_YEARS,
                    CACHE_GRADING_SCALES,
                    CACHE_FEE_CATALOG,
                    CACHE_EQUITY_PROFILES,
                    CACHE_FACULTY_PROFILES,
                    CACHE_FACULTY_PROFILE_BY_USER,
                    CACHE_STUDENT_PROFILES,
                    CACHE_STUDENT_PROFILE_BY_USER,
                    CACHE_CURRICULA,
                    CACHE_CURRICULA_BY_PROGRAM
            );

            @Override
            public java.util.Collection<String> getCacheNames() {
                java.util.Set<String> names = new java.util.LinkedHashSet<>(super.getCacheNames());
                names.addAll(INITIAL_NAMES);
                return names;
            }
        };
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    @Bean
    public org.springframework.boot.CommandLineRunner verifyCacheManager(CacheManager cm) {
        return args -> {
            org.slf4j.LoggerFactory.getLogger(CacheConfig.class)
                    .info("Initialized CacheManager: class={}, registeredCaches={}",
                            cm.getClass().getName(), cm.getCacheNames());
        };
    }
}
