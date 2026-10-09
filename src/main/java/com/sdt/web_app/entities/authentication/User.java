package com.sdt.web_app.entities.authentication;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sdt.web_app.entities.faculty.FacultyProfile;
import com.sdt.web_app.entities.institution.Department;
import com.sdt.web_app.entities.institution.Program;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;

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
@ToString
@BatchSize(size = 50)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50, updatable = false)
    private String username;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id")
    @ToString.Exclude
    @JsonIgnore
    private Department college;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    @ToString.Exclude
    @JsonIgnore
    private Program program;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private FacultyProfile facultyProfile;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    @Getter(AccessLevel.NONE)
    @ToString.Exclude
    @JsonIgnore
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING) // Maps enum values by name (e.g., "ADMIN") instead of ordinal
    @Column(name = "role", nullable = false, length = 30)
    @BatchSize(size = 50)
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
            if (this.roles == null) {
                this.roles = new HashSet<>();
            }
            try {
                this.roles.add(role);
            } catch (UnsupportedOperationException e) {
                this.roles = new HashSet<>(this.roles);
                this.roles.add(role);
            }
        }
    }

    public void removeRole(Roles role) {
        if (this.roles != null) {
            try {
                this.roles.remove(role);
            } catch (UnsupportedOperationException e) {
                this.roles = new HashSet<>(this.roles);
                this.roles.remove(role);
            }
        }
    }

    public void setRoles(Set<Roles> newRoles) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        try {
            this.roles.clear();
            if (newRoles != null) {
                this.roles.addAll(newRoles);
            }
        } catch (UnsupportedOperationException e) {
            this.roles = new HashSet<>(newRoles != null ? newRoles : Collections.emptySet());
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean hasRole(Roles role) {
        return this.roles.contains(role);
    }

    public void assignCollege(Department college) {
        this.college = college;
    }

    public void setCollege(Department college) {
        this.college = college;
    }

    public void assignProgram(Program program) {
        this.program = program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public FacultyProfile getFacultyProfile() {
        return this.facultyProfile;
    }

    public void setFacultyProfile(FacultyProfile facultyProfile) {
        this.facultyProfile = facultyProfile;
    }

    public void assignFacultyProfile(FacultyProfile facultyProfile) {
        this.facultyProfile = facultyProfile;
        if (facultyProfile != null && facultyProfile.getUser() != this) {
            facultyProfile.assignUser(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        User user = (User) o;
        return getUsername() != null && getUsername().equals(user.getUsername());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
