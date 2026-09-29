package com.sdt.web_app.service.enrollment;

import com.sdt.web_app.dto.enrollment.EnrollmentDtos.*;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.enrollment.StudentProfile.StudentClassification;
import com.sdt.web_app.entities.institution.Curriculum;
import com.sdt.web_app.entities.institution.Program;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.institution.CurriculumRepository;
import com.sdt.web_app.repositories.institution.ProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProgramRepository programRepository;
    @Mock
    private CurriculumRepository curriculumRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private com.sdt.web_app.service.security.AcademicScopeAssertionService academicScopeAssertionService;
    @Mock
    private com.sdt.web_app.repositories.admission.AdmissionApplicationRepository admissionApplicationRepository;

    @InjectMocks
    private StudentService studentService;

    private Program program;
    private Curriculum curriculum;

    @BeforeEach
    void setUp() {
        program = Program.builder()
                .code("BSIT")
                .name("Bachelor of Science in Information Technology")
                .build();
        ReflectionTestUtils.setField(program, "id", 10L);

        curriculum = Curriculum.builder()
                .program(program)
                .code("BSIT-2026")
                .name("BSIT 2026 Revised")
                .effectiveAcademicYear("2026-2027")
                .status(Curriculum.Status.ACTIVE)
                .build();
        ReflectionTestUtils.setField(curriculum, "id", 20L);
    }

    @Test
    @DisplayName("Should successfully register a new freshman student")
    void createStudent_Freshman_Success() {
        CreateStudentRequest req = new CreateStudentRequest(
                "2026-IT-0099",
                "new_freshman",
                "freshman@example.com",
                "Password123!",
                10L,
                20L,
                "INCOMING_FIRST_YEAR",
                1
        );

        given(studentProfileRepository.existsByStudentNumber("2026-IT-0099")).willReturn(false);
        given(userRepository.existsByUsername("new_freshman")).willReturn(false);
        given(userRepository.existsByEmail("freshman@example.com")).willReturn(false);
        given(programRepository.findById(10L)).willReturn(Optional.of(program));
        given(curriculumRepository.findById(20L)).willReturn(Optional.of(curriculum));
        given(passwordEncoder.encode("Password123!")).willReturn("$hashedPassword");

        User savedUser = User.builder()
                .username("new_freshman")
                .email("freshman@example.com")
                .enabled(true)
                .build();
        savedUser.addRole(Roles.STUDENT);
        ReflectionTestUtils.setField(savedUser, "id", 100L);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        StudentProfile savedProfile = StudentProfile.builder()
                .user(savedUser)
                .studentNumber("2026-IT-0099")
                .program(program)
                .curriculum(curriculum)
                .yearLevel(1)
                .classification(StudentClassification.INCOMING_FIRST_YEAR)
                .enrollmentStatus(StudentProfile.EnrollmentStatus.REGULAR)
                .build();
        ReflectionTestUtils.setField(savedProfile, "id", 500L);
        given(studentProfileRepository.save(any(StudentProfile.class))).willReturn(savedProfile);

        StudentProfileResponse response = studentService.createStudent(req);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.studentNumber()).isEqualTo("2026-IT-0099");
        assertThat(response.classification()).isEqualTo("INCOMING_FIRST_YEAR");
        assertThat(response.programCode()).isEqualTo("BSIT");
        assertThat(response.curriculumCode()).isEqualTo("BSIT-2026");
        verify(userRepository).save(any(User.class));
        verify(studentProfileRepository).save(any(StudentProfile.class));
    }

    @Test
    @DisplayName("Should successfully register a new transferee student")
    void createStudent_Transferee_Success() {
        CreateStudentRequest req = new CreateStudentRequest(
                "2026-IT-0100",
                "new_transferee",
                "transferee@example.com",
                null,
                10L,
                20L,
                "TRANSFEREE",
                2
        );

        given(studentProfileRepository.existsByStudentNumber("2026-IT-0100")).willReturn(false);
        given(userRepository.existsByUsername("new_transferee")).willReturn(false);
        given(userRepository.existsByEmail("transferee@example.com")).willReturn(false);
        given(programRepository.findById(10L)).willReturn(Optional.of(program));
        given(curriculumRepository.findById(20L)).willReturn(Optional.of(curriculum));
        given(passwordEncoder.encode("Student123!")).willReturn("$defaultHashedPassword");

        User savedUser = User.builder().username("new_transferee").email("transferee@example.com").build();
        ReflectionTestUtils.setField(savedUser, "id", 101L);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        StudentProfile savedProfile = StudentProfile.builder()
                .user(savedUser)
                .studentNumber("2026-IT-0100")
                .program(program)
                .curriculum(curriculum)
                .yearLevel(2)
                .classification(StudentClassification.TRANSFEREE)
                .build();
        ReflectionTestUtils.setField(savedProfile, "id", 501L);
        given(studentProfileRepository.save(any(StudentProfile.class))).willReturn(savedProfile);

        StudentProfileResponse response = studentService.createStudent(req);

        assertThat(response).isNotNull();
        assertThat(response.classification()).isEqualTo("TRANSFEREE");
        assertThat(response.yearLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should reject student creation when student number already exists")
    void createStudent_DuplicateStudentNumber_ThrowsException() {
        CreateStudentRequest req = new CreateStudentRequest(
                "2026-IT-0001",
                "duplicate_user",
                "dup@example.com",
                null,
                10L,
                20L,
                "INCOMING_FIRST_YEAR",
                1
        );

        given(studentProfileRepository.existsByStudentNumber("2026-IT-0001")).willReturn(true);

        assertThatThrownBy(() -> studentService.createStudent(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Student with student number '2026-IT-0001' already exists");
    }

    @Test
    @DisplayName("Should reject student creation when classification is invalid")
    void createStudent_InvalidClassification_ThrowsException() {
        CreateStudentRequest req = new CreateStudentRequest(
                "2026-IT-0002",
                "user2",
                "u2@example.com",
                null,
                10L,
                20L,
                "INVALID_CLASS",
                1
        );

        given(studentProfileRepository.existsByStudentNumber("2026-IT-0002")).willReturn(false);
        given(userRepository.existsByUsername("user2")).willReturn(false);
        given(userRepository.existsByEmail("u2@example.com")).willReturn(false);
        given(programRepository.findById(10L)).willReturn(Optional.of(program));
        given(curriculumRepository.findById(20L)).willReturn(Optional.of(curriculum));

        assertThatThrownBy(() -> studentService.createStudent(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid student classification");
    }

    @Test
    @DisplayName("findExistingStudentProfileIdFromAdmissionAppId: Returns empty when admission app does not exist")
    void findExistingStudentProfileId_NotFound_ReturnsEmpty() {
        given(admissionApplicationRepository.findById(99L)).willReturn(Optional.empty());

        Optional<Long> result = studentService.findExistingStudentProfileIdFromAdmissionAppId(99L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findExistingStudentProfileIdFromAdmissionAppId: Returns student profile ID when matching by user email")
    void findExistingStudentProfileId_MatchByEmail_ReturnsId() {
        com.sdt.web_app.entities.admission.AdmissionApplication app = com.sdt.web_app.entities.admission.AdmissionApplication.builder()
                .applicationNumber("APP-2026-001")
                .email("freshman@example.com")
                .firstName("Juan")
                .lastName("Dela Cruz")
                .build();
        ReflectionTestUtils.setField(app, "id", 1L);

        User existingUser = User.builder().username("juan").email("freshman@example.com").build();
        ReflectionTestUtils.setField(existingUser, "id", 100L);

        StudentProfile existingProfile = StudentProfile.builder()
                .user(existingUser)
                .studentNumber("2026-0001")
                .build();
        ReflectionTestUtils.setField(existingProfile, "id", 500L);

        given(admissionApplicationRepository.findById(1L)).willReturn(Optional.of(app));
        given(userRepository.findByEmail("freshman@example.com")).willReturn(Optional.of(existingUser));
        given(studentProfileRepository.findByUserId(100L)).willReturn(Optional.of(existingProfile));

        Optional<Long> result = studentService.findExistingStudentProfileIdFromAdmissionAppId(1L);

        assertThat(result).isPresent().contains(500L);
    }

    @Test
    @DisplayName("Should successfully register a new student with full name details")
    void createStudent_WithNameDetails_Success() {
        CreateStudentRequest req = new CreateStudentRequest(
                "2026-IT-0101",
                "maria_clara",
                "maria@example.com",
                "Password123!",
                "Maria",
                "Santos",
                "Clara",
                null,
                10L,
                20L,
                "INCOMING_FIRST_YEAR",
                1,
                null
        );

        given(studentProfileRepository.existsByStudentNumber("2026-IT-0101")).willReturn(false);
        given(userRepository.existsByUsername("maria_clara")).willReturn(false);
        given(userRepository.existsByEmail("maria@example.com")).willReturn(false);
        given(programRepository.findById(10L)).willReturn(Optional.of(program));
        given(curriculumRepository.findById(20L)).willReturn(Optional.of(curriculum));
        given(passwordEncoder.encode("Password123!")).willReturn("$hashedPassword");

        User savedUser = User.builder()
                .username("maria_clara")
                .email("maria@example.com")
                .enabled(true)
                .build();
        ReflectionTestUtils.setField(savedUser, "id", 101L);
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        given(studentProfileRepository.save(any(StudentProfile.class))).willAnswer(inv -> {
            StudentProfile sp = inv.getArgument(0);
            ReflectionTestUtils.setField(sp, "id", 1001L);
            return sp;
        });

        StudentProfileResponse resp = studentService.createStudent(req);

        assertThat(resp).isNotNull();
        assertThat(resp.firstName()).isEqualTo("Maria");
        assertThat(resp.middleName()).isEqualTo("Santos");
        assertThat(resp.lastName()).isEqualTo("Clara");
        assertThat(resp.fullName()).isEqualTo("Maria Santos Clara");
    }
}
