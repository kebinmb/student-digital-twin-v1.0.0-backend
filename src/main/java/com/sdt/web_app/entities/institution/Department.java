package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.BatchSize;

import java.util.Objects;

@Entity
@Table(
        name = "departments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_campus_dept_code", columnNames = {"campus_id", "code"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"parentDepartment", "campus"})
@BatchSize(size = 50)
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campus_id", nullable = false, updatable = false)
    private Campus campus;

    @Column(nullable = false, length = 20, updatable = false)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DepartmentType type = DepartmentType.COLLEGE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_department_id")
    private Department parentDepartment;

    @Column(name = "dean_user_id")
    private Long deanUserId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public void updateInfo(String name, DepartmentType type) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Department name cannot be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Department type cannot be blank");
        }
        this.name = name;
        this.type = type;
    }

    public void assignParentDepartment(Department parent) {
        if (parent != null) {
            if (this == parent || (this.id != null && Objects.equals(this.id, parent.getId()))) {
                throw new IllegalArgumentException("A department cannot be its own parent");
            }
        }
        this.parentDepartment = parent;
    }

    public void assignDean(Long deanUserId) {
        this.deanUserId = deanUserId;
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
        if (!(o instanceof Department that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}