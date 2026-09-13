package com.sdt.web_app.service.security;

import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.faculty.FacultyProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityProfileCache {

    private final FacultyProfileL2CacheService l2CacheService;
    private final StudentProfileL2CacheService studentL2CacheService;

    @SuppressWarnings("unchecked")
    public Optional<FacultyProfile> getFacultyProfile(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        String attrName = "CACHED_FACULTY_" + userId;
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            Object cachedObj = attributes.getAttribute(attrName, RequestAttributes.SCOPE_REQUEST);
            if (cachedObj instanceof Optional<?>) {
                return (Optional<FacultyProfile>) cachedObj;
            }
            Object sdtObj = attributes.getAttribute("SDT_REQ_CACHED_FACULTY_" + userId, RequestAttributes.SCOPE_REQUEST);
            if (sdtObj instanceof Optional<?>) {
                return (Optional<FacultyProfile>) sdtObj;
            }
            Object legacyObj = attributes.getAttribute("SDT_CACHED_FACULTY_PROFILE", RequestAttributes.SCOPE_REQUEST);
            if (legacyObj instanceof FacultyProfile fp && fp.getUser() != null && userId.equals(fp.getUser().getId())) {
                return Optional.of(fp);
            }
        }

        FacultyProfile profile = l2CacheService.findByUserId(userId);
        Optional<FacultyProfile> profileOpt = Optional.ofNullable(profile);

        if (attributes != null) {
            attributes.setAttribute(attrName, profileOpt, RequestAttributes.SCOPE_REQUEST);
        }

        return profileOpt;
    }

    @SuppressWarnings("unchecked")
    public Optional<StudentProfile> getStudentProfile(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }

        String attrName = "CACHED_STUDENT_USER_" + userId;
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            Object cachedObj = attributes.getAttribute(attrName, RequestAttributes.SCOPE_REQUEST);
            if (cachedObj instanceof Optional<?>) {
                return (Optional<StudentProfile>) cachedObj;
            }
        }

        StudentProfile profile = studentL2CacheService.findByUserId(userId);
        Optional<StudentProfile> profileOpt = Optional.ofNullable(profile);

        if (attributes != null) {
            attributes.setAttribute(attrName, profileOpt, RequestAttributes.SCOPE_REQUEST);
        }

        return profileOpt;
    }

    @SuppressWarnings("unchecked")
    public Optional<StudentProfile> getStudentProfileById(Long studentId) {
        if (studentId == null) {
            return Optional.empty();
        }

        String attrName = "CACHED_STUDENT_ID_" + studentId;
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            Object cachedObj = attributes.getAttribute(attrName, RequestAttributes.SCOPE_REQUEST);
            if (cachedObj instanceof Optional<?>) {
                return (Optional<StudentProfile>) cachedObj;
            }
        }

        StudentProfile profile = studentL2CacheService.findById(studentId);
        Optional<StudentProfile> profileOpt = Optional.ofNullable(profile);

        if (attributes != null) {
            attributes.setAttribute(attrName, profileOpt, RequestAttributes.SCOPE_REQUEST);
        }

        return profileOpt;
    }
}
