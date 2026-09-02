package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "campuses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
public class Campus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20, updatable = false)
    private String code;

    @Column(name = "ched_institutional_code", length = 20)
    private String chedInstitutionalCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String region = "REGION VI";

    @Column(name = "contact_number", length = 30)
    private String contactNumber;

    @Column(length = 100)
    private String email;

    @Column(name = "is_main", nullable = false)
    @Builder.Default
    private boolean isMain = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public void updateDetails(String name, String address, String contactNumber, String email, String chedInstitutionalCode) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Campus name cannot be blank.");
        }
        this.name = name;
        this.address = address;
        this.contactNumber = contactNumber;
        this.email = email;
        this.chedInstitutionalCode = chedInstitutionalCode;
    }

    public void markAsMainCampus() {
        this.isMain = true;
    }

    public void demoteFromMainCampus() {
        this.isMain = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Campus campus)) return false;
        return code != null && Objects.equals(code, campus.code);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
