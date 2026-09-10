package com.sdt.web_app.service.authentication;

import com.sdt.web_app.dto.authentication.UserDtos.*;
import com.sdt.web_app.entities.authentication.Roles;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.exceptions.UserAlreadyExistsException;
import com.sdt.web_app.repositories.authentication.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDetailResponse createUser(CreateUserRequest request) {
        String trimmedUsername = request.username().trim();
        String trimmedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByUsername(trimmedUsername)) {
            throw new UserAlreadyExistsException("Username '" + trimmedUsername + "' is already in use.");
        }
        if (userRepository.existsByEmail(trimmedEmail)) {
            throw new UserAlreadyExistsException("Email '" + trimmedEmail + "' is already in use.");
        }

        Set<Roles> mappedRoles = parseRoles(request.roles());
        if (mappedRoles.isEmpty()) {
            throw new IllegalArgumentException("At least one valid role must be provided.");
        }

        User user = User.builder()
                .username(trimmedUsername)
                .email(trimmedEmail)
                .password(passwordEncoder.encode(request.password().trim()))
                .enabled(request.enabled() == null || request.enabled())
                .build();

        for (Roles role : mappedRoles) {
            user.addRole(role);
        }

        User savedUser = userRepository.save(user);
        log.info("Admin created new user account: id={}, username={}, roles={}",
                savedUser.getId(), savedUser.getUsername(), savedUser.getRoles());

        return mapToUserDetailResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserDetailResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserDetailResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserDetailResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
        return mapToUserDetailResponse(user);
    }

    @Transactional
    public UserDetailResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        if (request.email() != null && !request.email().isBlank()) {
            String newEmail = request.email().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new UserAlreadyExistsException("Email '" + newEmail + "' is already in use.");
            }
            user.updateEmail(newEmail);
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.updatePassword(passwordEncoder.encode(request.password().trim()));
        }

        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        if (request.roles() != null && !request.roles().isEmpty()) {
            Set<Roles> mappedRoles = parseRoles(request.roles());
            user.setRoles(mappedRoles);
        }

        User updatedUser = userRepository.save(user);
        log.info("Admin updated user account: id={}, username={}", updatedUser.getId(), updatedUser.getUsername());
        return mapToUserDetailResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
        userRepository.delete(user);
        log.info("Admin deleted user account: id={}, username={}", id, user.getUsername());
    }

    private Set<Roles> parseRoles(Set<String> roleNames) {
        Set<Roles> roles = new HashSet<>();
        if (roleNames == null) return roles;
        for (String name : roleNames) {
            String cleanName = name.trim().toUpperCase();
            if (cleanName.startsWith("ROLE_")) {
                cleanName = cleanName.substring(5);
            }
            try {
                roles.add(Roles.valueOf(cleanName));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role specified: " + name);
            }
        }
        return roles;
    }

    private UserDetailResponse mapToUserDetailResponse(User user) {
        Set<String> roleStrings = user.getRoles().stream()
                .map(Roles::name)
                .collect(Collectors.toSet());

        return new UserDetailResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                roleStrings,
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
