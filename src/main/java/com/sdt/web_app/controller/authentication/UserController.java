package com.sdt.web_app.controller.authentication;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.dto.authentication.UserDtos.*;
import com.sdt.web_app.service.authentication.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'REGISTRAR')")
public class UserController {

    private final UserService userService;

    @Auditable(action = "CREATE_USER", entityName = "User")
    @PostMapping
    public ResponseEntity<UserDetailResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserDetailResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Auditable(action = "READ_ALL_USERS", entityName = "User")
    @GetMapping
    public ResponseEntity<List<UserDetailResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Auditable(action = "READ_USER", entityName = "User", entityId = "#id")
    @GetMapping("/{id}")
    public ResponseEntity<UserDetailResponse> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Auditable(action = "UPDATE_USER", entityName = "User", entityId = "#id")
    @PutMapping("/{id}")
    public ResponseEntity<UserDetailResponse> updateUser(
            @PathVariable("id") Long id,
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @Auditable(action = "DELETE_USER", entityName = "User", entityId = "#id")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
