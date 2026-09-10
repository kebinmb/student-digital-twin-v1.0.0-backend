package com.sdt.web_app.service.authentication;

import com.sdt.web_app.dto.authentication.UserDtos.*;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.exceptions.UserAlreadyExistsException;
import com.sdt.web_app.repositories.authentication.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.sdt.web_app.repositories.institution.DepartmentRepository departmentRepository;

    @Mock
    private com.sdt.web_app.repositories.institution.ProgramRepository programRepository;

    @Mock
    private com.sdt.web_app.service.security.AcademicScopeAssertionService academicScopeAssertionService;

    @Mock
    private com.sdt.web_app.repositories.faculty.FacultyProfileRepository facultyProfileRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;
    private com.sdt.web_app.entities.institution.Department college;
    private com.sdt.web_app.entities.institution.Program program;

    @BeforeEach
    void setUp() {
        college = com.sdt.web_app.entities.institution.Department.builder()
                .code("CCS")
                .name("College of Computer Studies")
                .type(com.sdt.web_app.entities.institution.DepartmentType.COLLEGE)
                .build();
        ReflectionTestUtils.setField(college, "id", 100L);

        program = com.sdt.web_app.entities.institution.Program.builder()
                .code("BSCS")
                .name("BS Computer Science")
                .department(college)
                .college(college)
                .build();
        ReflectionTestUtils.setField(program, "id", 200L);

        sampleUser = User.builder()
                .username("test_user")
                .email("test@example.com")
                .password("encoded_hash")
                .enabled(true)
                .build();
        sampleUser.addRole(Roles.STUDENT);
        ReflectionTestUtils.setField(sampleUser, "id", 10L);
    }

    @Test
    @DisplayName("Admin: Should create new user account successfully")
    void createUser_Success() {
        CreateUserRequest request = new CreateUserRequest(
                "new_admin",
                "admin@new.edu",
                "Password123!",
                Set.of("ADMIN", "REGISTRAR"),
                true
        );

        given(userRepository.existsByUsername("new_admin")).willReturn(false);
        given(userRepository.existsByEmail("admin@new.edu")).willReturn(false);
        given(passwordEncoder.encode("Password123!")).willReturn("hashed_pass");
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 55L);
            return u;
        });

        UserDetailResponse response = userService.createUser(request);

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("new_admin");
        assertThat(response.email()).isEqualTo("admin@new.edu");
        assertThat(response.roles()).containsExactlyInAnyOrder("ADMIN", "REGISTRAR");
        assertThat(response.enabled()).isTrue();
    }

    @Test
    @DisplayName("Admin / Registrar: Should create user with attached College and Program")
    void createUser_WithCollegeAndProgram_Success() {
        CreateUserRequest request = new CreateUserRequest(
                "chair_user",
                "chair@chmsu.edu.ph",
                "Password123!",
                Set.of("CHAIRPERSON"),
                true,
                100L,
                200L
        );

        given(userRepository.existsByUsername("chair_user")).willReturn(false);
        given(userRepository.existsByEmail("chair@chmsu.edu.ph")).willReturn(false);
        given(passwordEncoder.encode("Password123!")).willReturn("hashed_pass");
        given(departmentRepository.findById(100L)).willReturn(Optional.of(college));
        given(programRepository.findById(200L)).willReturn(Optional.of(program));
        given(academicScopeAssertionService.resolveProgramCollegeId(program)).willReturn(100L);
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            ReflectionTestUtils.setField(u, "id", 77L);
            return u;
        });

        UserDetailResponse response = userService.createUser(request);

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("chair_user");
        assertThat(response.collegeId()).isEqualTo(100L);
        assertThat(response.collegeCode()).isEqualTo("CCS");
        assertThat(response.programId()).isEqualTo(200L);
        assertThat(response.programCode()).isEqualTo("BSCS");
    }

    @Test
    @DisplayName("Admin: Should reject DEAN creation if program is attached")
    void createUser_DeanWithProgram_ThrowsException() {
        CreateUserRequest request = new CreateUserRequest(
                "dean_user",
                "dean@chmsu.edu.ph",
                "Password123!",
                Set.of("DEAN"),
                true,
                100L,
                200L
        );

        given(userRepository.existsByUsername("dean_user")).willReturn(false);
        given(userRepository.existsByEmail("dean@chmsu.edu.ph")).willReturn(false);
        given(departmentRepository.findById(100L)).willReturn(Optional.of(college));
        given(programRepository.findById(200L)).willReturn(Optional.of(program));
        given(academicScopeAssertionService.resolveProgramCollegeId(program)).willReturn(100L);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DEAN cannot be assigned to a specific program");
    }

    @Test
    @DisplayName("Admin: Should reject create user when username already exists")
    void createUser_DuplicateUsername_ThrowsException() {
        CreateUserRequest request = new CreateUserRequest(
                "existing_user",
                "unique@new.edu",
                "Password123!",
                Set.of("FACULTY"),
                true
        );

        given(userRepository.existsByUsername("existing_user")).willReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    @DisplayName("Admin: Should update user email, roles, password, and attach college for DEAN")
    void updateUser_Success() {
        UpdateUserRequest request = new UpdateUserRequest(
                "updated@example.com",
                "NewSecret123!",
                Set.of("DEAN"),
                false,
                100L,
                null
        );

        given(userRepository.findById(10L)).willReturn(Optional.of(sampleUser));
        given(userRepository.existsByEmail("updated@example.com")).willReturn(false);
        given(passwordEncoder.encode("NewSecret123!")).willReturn("new_hashed_pass");
        given(departmentRepository.findById(100L)).willReturn(Optional.of(college));
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        UserDetailResponse response = userService.updateUser(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("updated@example.com");
        assertThat(response.roles()).containsExactly("DEAN");
        assertThat(response.enabled()).isFalse();
        assertThat(response.collegeId()).isEqualTo(100L);
        assertThat(response.collegeCode()).isEqualTo("CCS");
    }

    @Test
    @DisplayName("Admin: Should delete user account")
    void deleteUser_Success() {
        given(userRepository.findById(10L)).willReturn(Optional.of(sampleUser));

        userService.deleteUser(10L);

        verify(userRepository).delete(sampleUser);
    }
}
