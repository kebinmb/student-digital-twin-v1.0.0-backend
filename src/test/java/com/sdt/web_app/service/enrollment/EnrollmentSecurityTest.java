package com.sdt.web_app.service.enrollment;

import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.service.security.SecurityProfileCache;
import com.sdt.web_app.service.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EnrollmentSecurityTest {

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private SecurityProfileCache securityProfileCache;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private com.sdt.web_app.service.security.AcademicScopeAssertionService academicScopeAssertionService;

    @Mock
    private com.sdt.web_app.repositories.admission.AdmissionApplicationRepository admissionApplicationRepository;

    @InjectMocks
    private EnrollmentSecurity enrollmentSecurity;

    private StudentProfile ownedProfile;

    @BeforeEach
    void setUp() {
        ownedProfile = StudentProfile.builder()
                .studentNumber("2026-00001")
                .yearLevel(1)
                .build();
        ReflectionTestUtils.setField(ownedProfile, "id", 100L);
    }

    @Test
    @DisplayName("canAccessStudentAdvising: Admin role allows access to any student")
    void canAccessStudentAdvising_AdminAllowed() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        boolean result = enrollmentSecurity.canAccessStudentAdvising(auth, 999L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("canAccessStudentAdvising: Student role allows access only to own profile")
    void canAccessStudentAdvising_StudentOwnerAllowed() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "student1", "pass", List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        given(securityUtils.resolveUserId(auth)).willReturn(42L);
        given(securityProfileCache.getStudentProfile(42L)).willReturn(Optional.of(ownedProfile));

        boolean result = enrollmentSecurity.canAccessStudentAdvising(auth, 100L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("canAccessStudentAdvising: Student attempting IDOR on another student is rejected")
    void canAccessStudentAdvising_StudentIdorRejected() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "student1", "pass", List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        given(securityUtils.resolveUserId(auth)).willReturn(42L);
        given(securityProfileCache.getStudentProfile(42L)).willReturn(Optional.of(ownedProfile));

        boolean result = enrollmentSecurity.canAccessStudentAdvising(auth, 999L);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("canAccessStudentEnrollment: Student owner allowed to enlist/manage own enrollment")
    void canAccessStudentEnrollment_StudentOwnerAllowed() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "student1", "pass", List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        given(securityUtils.resolveUserId(auth)).willReturn(42L);
        given(securityProfileCache.getStudentProfile(42L)).willReturn(Optional.of(ownedProfile));

        boolean result = enrollmentSecurity.canAccessStudentEnrollment(auth, 100L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("canAccessStudentAdvising: Student role allows access when studentId is own userId")
    void canAccessStudentAdvising_StudentUserIdAllowed() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "student1", "pass", List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        given(securityUtils.resolveUserId(auth)).willReturn(42L);
        given(securityProfileCache.getStudentProfile(42L)).willReturn(Optional.of(ownedProfile));

        boolean result = enrollmentSecurity.canAccessStudentAdvising(auth, 42L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("canAccessStudentEnrollment: Student owner allowed to access when studentId is own userId")
    void canAccessStudentEnrollment_StudentUserIdAllowed() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "student1", "pass", List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        given(securityUtils.resolveUserId(auth)).willReturn(42L);
        given(securityProfileCache.getStudentProfile(42L)).willReturn(Optional.of(ownedProfile));

        boolean result = enrollmentSecurity.canAccessStudentEnrollment(auth, 42L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("canAccessStudentEnrollment: Student IDOR attempt to enlist for other student is rejected")
    void canAccessStudentEnrollment_StudentIdorRejected() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "student1", "pass", List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));
        given(securityUtils.resolveUserId(auth)).willReturn(42L);
        given(securityProfileCache.getStudentProfile(42L)).willReturn(Optional.of(ownedProfile));

        boolean result = enrollmentSecurity.canAccessStudentEnrollment(auth, 999L);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("canAccessStudentEnrollment: Admin allowed to access negative student ID (admission application)")
    void canAccessStudentEnrollment_NegativeId_AdminAllowed() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        boolean result = enrollmentSecurity.canAccessStudentEnrollment(auth, -1L);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("canAccessStudentAdvising: Admin allowed to access negative student ID (admission application)")
    void canAccessStudentAdvising_NegativeId_AdminAllowed() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "pass", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        boolean result = enrollmentSecurity.canAccessStudentAdvising(auth, -1L);

        assertThat(result).isTrue();
    }
}
