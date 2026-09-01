package com.sdt.web_app.entities.authentication;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = "password")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50, updatable = false)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    @Getter(AccessLevel.NONE)
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING) // Maps enum values by name (e.g., "ADMIN") instead of ordinal
    @Column(name = "role", nullable = false, length = 30)
    @Builder.Default
    @Getter(AccessLevel.NONE)
    private Set<Roles> roles = new HashSet<>();

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    //Implement Auditable for better auditing

    public String getPasswordHash() {
        return this.password;
    }

    public void updatePassword(String newHashedPassword) {
        if (newHashedPassword == null || newHashedPassword.isBlank()) {
            throw new IllegalArgumentException("Password hash cannot be blank");
        }
        this.password = newHashedPassword;
    }

    public void updateEmail(String newEmail) {
        if (newEmail == null || !newEmail.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.email = newEmail;
    }

    public void disableAccount() {
        this.enabled = false;
    }

    public void enableAccount() {
        this.enabled = true;
    }

    public Set<Roles> getRoles() {
        return Collections.unmodifiableSet(this.roles);
    }

    // Strongly-typed helper methods
    public void addRole(Roles role) {
        if (role != null) {
            this.roles.add(role);
        }
    }

    public void removeRole(Roles role) {
        this.roles.remove(role);
    }

    public boolean hasRole(Roles role) {
        return this.roles.contains(role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return username != null && username.equals(user.username);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
