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

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
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
    @DisplayName("Admin: Should update user email, roles, and password")
    void updateUser_Success() {
        UpdateUserRequest request = new UpdateUserRequest(
                "updated@example.com",
                "NewSecret123!",
                Set.of("DEAN"),
                false
        );

        given(userRepository.findById(10L)).willReturn(Optional.of(sampleUser));
        given(userRepository.existsByEmail("updated@example.com")).willReturn(false);
        given(passwordEncoder.encode("NewSecret123!")).willReturn("new_hashed_pass");
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        UserDetailResponse response = userService.updateUser(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("updated@example.com");
        assertThat(response.roles()).containsExactly("DEAN");
        assertThat(response.enabled()).isFalse();
    }

    @Test
    @DisplayName("Admin: Should delete user account")
    void deleteUser_Success() {
        given(userRepository.findById(10L)).willReturn(Optional.of(sampleUser));

        userService.deleteUser(10L);

        verify(userRepository).delete(sampleUser);
    }
}
