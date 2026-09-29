package com.sdt.web_app.service.faculty;

import com.sdt.web_app.dto.faculty.FacultyDtos.*;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.faculty.FacultyProfile;
import com.sdt.web_app.entities.institution.AcademicYear;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.institution.TermType;
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
import static org.mockito.Mockito.lenient;
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
    private com.sdt.web_app.service.institution.TermService termService;
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
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
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
        Term mockTerm = Term.builder()
                .academicYear(com.sdt.web_app.entities.institution.AcademicYear.builder().code("AY 2026-2027").build())
                .termType(TermType.FIRST_SEM)
                .build();
        ReflectionTestUtils.setField(mockTerm, "id", 100L);
        lenient().when(termService.getTermById(any())).thenReturn(mockTerm);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should retrieve existing faculty profile")
    void getProfileByUserId_Existing_Success() {
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

    @Test
    @DisplayName("Should provision new faculty account with attached College and Program")
    void createFacultyAccount_WithCollegeAndProgram_Success() {
        com.sdt.web_app.entities.institution.Department ccs = com.sdt.web_app.entities.institution.Department.builder()
                .code("CCS")
                .name("College of Computer Studies")
                .build();
        ReflectionTestUtils.setField(ccs, "id", 100L);

        com.sdt.web_app.entities.institution.Program bscs = com.sdt.web_app.entities.institution.Program.builder()
                .code("BSCS")
                .name("BS Computer Science")
                .department(ccs)
                .college(ccs)
                .build();
        ReflectionTestUtils.setField(bscs, "id", 200L);

        CreateFacultyAccountRequest request = new CreateFacultyAccountRequest(
                "prof_it",
                "prof.it@example.com",
                "Secret123!",
                "FAC-2026-8888",
                "MASTERS",
                "ASSISTANT_PROFESSOR_I",
                "PRC-8888888",
                "FULL_TIME",
                false,
                100L,
                200L
        );

        given(userRepository.existsByUsername("prof_it")).willReturn(false);
        given(userRepository.existsByEmail("prof.it@example.com")).willReturn(false);
        given(profileRepository.existsByFacultyIdNumber("FAC-2026-8888")).willReturn(false);
        given(passwordEncoder.encode("Secret123!")).willReturn("encodedPassword");
        given(departmentRepository.findById(100L)).willReturn(Optional.of(ccs));
        given(programRepository.findById(200L)).willReturn(Optional.of(bscs));
        given(academicScopeAssertionService.resolveProgramCollegeId(bscs)).willReturn(100L);
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 888L);
            return u;
        });
        given(profileRepository.save(any(FacultyProfile.class))).willAnswer(inv -> {
            FacultyProfile fp = inv.getArgument(0);
            ReflectionTestUtils.setField(fp, "id", 2002L);
            return fp;
        });

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "admin", "n/a", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );
        given(academicScopeAssertionService.assertAndResolveScope(any())).willReturn(
                com.sdt.web_app.service.security.AcademicScopeContext.unrestricted(1L)
        );

        FacultyProfileResponse response = facultyService.createFacultyAccount(request);

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("prof_it");
        assertThat(response.collegeId()).isEqualTo(100L);
        assertThat(response.collegeCode()).isEqualTo("CCS");
        assertThat(response.programId()).isEqualTo(200L);
        assertThat(response.programCode()).isEqualTo("BSCS");
    }

    @Test
    @DisplayName("Should provision new faculty account with full name details")
    void createFacultyAccount_WithNameDetails_Success() {
        CreateFacultyAccountRequest request = new CreateFacultyAccountRequest(
                "prof_curie",
                "curie@example.com",
                "Secret123!",
                "FAC-2026-7777",
                "Marie",
                "Salomea",
                "Curie",
                "PhD",
                "DOCTORATE",
                "PROFESSOR_I",
                "PRC-7777777",
                "FULL_TIME",
                true,
                null,
                null
        );

        given(userRepository.existsByUsername("prof_curie")).willReturn(false);
        given(userRepository.existsByEmail("curie@example.com")).willReturn(false);
        given(profileRepository.existsByFacultyIdNumber("FAC-2026-7777")).willReturn(false);
        given(passwordEncoder.encode("Secret123!")).willReturn("encodedPassword");
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 777L);
            return u;
        });
        given(profileRepository.save(any(FacultyProfile.class))).willAnswer(inv -> {
            FacultyProfile fp = inv.getArgument(0);
            ReflectionTestUtils.setField(fp, "id", 3003L);
            return fp;
        });

        FacultyProfileResponse response = facultyService.createFacultyAccount(request);

        assertThat(response).isNotNull();
        assertThat(response.firstName()).isEqualTo("Marie");
        assertThat(response.middleName()).isEqualTo("Salomea");
        assertThat(response.lastName()).isEqualTo("Curie");
        assertThat(response.suffix()).isEqualTo("PhD");
        assertThat(response.fullName()).isEqualTo("Marie Salomea Curie PhD");
    }

    @Test
    @DisplayName("Should update faculty profile with name details")
    void updateFacultyProfile_WithNameDetails_Success() {
        UpdateFacultyProfileRequest request = new UpdateFacultyProfileRequest(
                "Albert",
                null,
                "Einstein",
                null,
                "DOCTORATE",
                "PROFESSOR_I",
                "PRC-9999999",
                "FULL_TIME",
                true,
                null,
                null
        );

        given(profileRepository.findByUserIdWithUser(42L)).willReturn(Optional.of(profile));
        given(profileRepository.save(any(FacultyProfile.class))).willAnswer(inv -> inv.getArgument(0));

        FacultyProfileResponse response = facultyService.updateProfile(42L, request);

        assertThat(response).isNotNull();
        assertThat(response.firstName()).isEqualTo("Albert");
        assertThat(response.lastName()).isEqualTo("Einstein");
        assertThat(response.fullName()).isEqualTo("Albert Einstein");
    }
}
