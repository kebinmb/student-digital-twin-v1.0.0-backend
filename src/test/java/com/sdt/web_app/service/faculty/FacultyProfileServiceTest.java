package com.sdt.web_app.service.faculty;

import com.sdt.web_app.dto.faculty.FacultyDtos.*;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.faculty.FacultyProfile;
import com.sdt.web_app.entities.institution.AcademicYear;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.scheduling.FacultyWorkload;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.faculty.FacultyProfileRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.repositories.scheduling.ClassScheduleRepository;
import com.sdt.web_app.repositories.scheduling.FacultyWorkloadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FacultyProfileServiceTest {

    @Mock
    private FacultyProfileRepository profileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TermRepository termRepository;
    @Mock
    private FacultyWorkloadRepository workloadRepository;
    @Mock
    private ClassScheduleRepository scheduleRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    private com.sdt.web_app.service.security.AcademicScopeAssertionService academicScopeAssertionService;
    @Mock
    private com.sdt.web_app.repositories.institution.DepartmentRepository departmentRepository;
    @Mock
    private com.sdt.web_app.repositories.institution.ProgramRepository programRepository;

    @InjectMocks
    private FacultyProfileService facultyService;

    private User facultyUser;
    private FacultyProfile profile;

    @BeforeEach
    void setUp() {
        facultyUser = User.builder()
                .username("prof_einstein")
                .email("einstein@example.com")
                .build();
        facultyUser.addRole(Roles.FACULTY);
        ReflectionTestUtils.setField(facultyUser, "id", 42L);

        profile = FacultyProfile.builder()
                .user(facultyUser)
                .facultyIdNumber("FAC-2026-0042")
                .highestDegree(FacultyProfile.HighestDegree.DOCTORATE)
                .academicRank(FacultyProfile.AcademicRank.PROFESSOR_I)
                .prcLicenseNo("PRC-1234567")
                .employmentStatus(FacultyProfile.EmploymentStatus.FULL_TIME)
                .isTenured(true)
                .build();
        ReflectionTestUtils.setField(profile, "id", 1001L);
    }

    @Test
    @DisplayName("Should retrieve existing faculty profile")
    void getProfileByUserId_Existing_Success() {
        given(userRepository.findById(42L)).willReturn(Optional.of(facultyUser));
        given(profileRepository.findByUserIdWithUser(42L)).willReturn(Optional.of(profile));

        FacultyProfileResponse response = facultyService.getProfileByUserId(42L);

        assertThat(response).isNotNull();
        assertThat(response.facultyIdNumber()).isEqualTo("FAC-2026-0042");
        assertThat(response.highestDegree()).isEqualTo("DOCTORATE");
        assertThat(response.academicRank()).isEqualTo("PROFESSOR_I");
        assertThat(response.isTenured()).isTrue();
    }

    @Test
    @DisplayName("Should update faculty credentials and return updated profile")
    void updateProfile_Success() {
        UpdateFacultyProfileRequest request = new UpdateFacultyProfileRequest(
                "DOCTORATE",
                "PROFESSOR_VI",
                "PRC-9999999",
                "FULL_TIME",
                true
        );

        given(profileRepository.findByUserIdWithUser(42L)).willReturn(Optional.of(profile));
        given(profileRepository.save(any(FacultyProfile.class))).willReturn(profile);

        FacultyProfileResponse response = facultyService.updateProfile(42L, request);

        assertThat(response).isNotNull();
        assertThat(response.academicRank()).isEqualTo("PROFESSOR_VI");
        assertThat(response.prcLicenseNo()).isEqualTo("PRC-9999999");
        verify(profileRepository).save(profile);
    }

    @Test
    @DisplayName("Should generate CHED Form E-5 faculty workload report")
    void generateChedE5Report_Success() {
        AcademicYear ay = AcademicYear.builder().code("AY 2026-2027").build();
        Term term = Term.builder().academicYear(ay).termType(com.sdt.web_app.entities.institution.TermType.FIRST_SEM).build();
        ReflectionTestUtils.setField(term, "id", 10L);

        FacultyWorkload workload = FacultyWorkload.builder()
                .faculty(facultyUser)
                .term(term)
                .regularUnits(new BigDecimal("18.00"))
                .overloadUnits(new BigDecimal("3.00"))
                .totalContactHours(new BigDecimal("21.00"))
                .numberOfPreparations(2)
                .build();

        given(termRepository.findById(10L)).willReturn(Optional.of(term));
        given(userRepository.findAll()).willReturn(List.of(facultyUser));
        given(profileRepository.findAllWithUser()).willReturn(List.of(profile));
        given(workloadRepository.findByTermId(10L)).willReturn(List.of(workload));
        given(scheduleRepository.findAll()).willReturn(Collections.emptyList());

        ChedE5ReportResponse report = facultyService.generateChedE5Report(10L);

        assertThat(report).isNotNull();
        assertThat(report.totalFacultyCount()).isEqualTo(1);
        assertThat(report.totalRegularUnits()).isEqualByComparingTo("18.00");
        assertThat(report.totalOverloadUnits()).isEqualByComparingTo("3.00");
        assertThat(report.facultyWorkloads()).hasSize(1);
        assertThat(report.facultyWorkloads().get(0).facultyName()).isEqualTo("prof_einstein");
        assertThat(report.facultyWorkloads().get(0).highestDegree()).isEqualTo("DOCTORATE");
    }

    @Test
    @DisplayName("Should provision new faculty account and profile")
    void createFacultyAccount_Success() {
        CreateFacultyAccountRequest request = new CreateFacultyAccountRequest(
                "prof_new",
                "prof.new@example.com",
                "Secret123!",
                "FAC-2026-9999",
                "MASTERS",
                "ASSISTANT_PROFESSOR_I",
                "PRC-8888888",
                "FULL_TIME",
                false
        );

        given(userRepository.existsByUsername("prof_new")).willReturn(false);
        given(userRepository.existsByEmail("prof.new@example.com")).willReturn(false);
        given(profileRepository.existsByFacultyIdNumber("FAC-2026-9999")).willReturn(false);
        given(passwordEncoder.encode("Secret123!")).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 999L);
            return u;
        });
        given(profileRepository.save(any(FacultyProfile.class))).willAnswer(inv -> {
            FacultyProfile fp = inv.getArgument(0);
            ReflectionTestUtils.setField(fp, "id", 2001L);
            return fp;
        });

        FacultyProfileResponse response = facultyService.createFacultyAccount(request);

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("prof_new");
        assertThat(response.facultyIdNumber()).isEqualTo("FAC-2026-9999");
        assertThat(response.highestDegree()).isEqualTo("MASTERS");
        assertThat(response.academicRank()).isEqualTo("ASSISTANT_PROFESSOR_I");
    }

    @Test
    @DisplayName("Should retrieve all faculty profiles")
    void getAllFacultyProfiles_Success() {
        given(profileRepository.findAllWithUser()).willReturn(List.of(profile));

        List<FacultyProfileResponse> profiles = facultyService.getAllFacultyProfiles();

        assertThat(profiles).hasSize(1);
        assertThat(profiles.get(0).facultyIdNumber()).isEqualTo("FAC-2026-0042");
    }
}
