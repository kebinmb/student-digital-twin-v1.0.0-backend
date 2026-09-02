package com.sdt.web_app.entities.institution;


import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "programs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = "department")
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(unique = true, nullable = false, length = 20, updatable = false)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String major;

    @Column(name = "degree_level", nullable = false, length = 30)
    @Builder.Default
    private String degreeLevel = "UNDERGRADUATE";

    @Column(name = "ched_cmo_reference", length = 100)
    private String chedCmoReference;

    @Column(name = "government_permit", length = 100)
    private String governmentPermit;

    @Column(name = "total_units_required", nullable = false)
    private int totalUnitsRequired;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public void updateProgramInfo(String name, String major, String cmoRef, String permit, int totalUnits) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Program name cannot be blank");
        }
        this.name = name;
        this.major = major;
        this.chedCmoReference = cmoRef;
        this.governmentPermit = permit;
        this.totalUnitsRequired = totalUnits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Program program)) return false;
        return code != null && Objects.equals(code, program.code);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
