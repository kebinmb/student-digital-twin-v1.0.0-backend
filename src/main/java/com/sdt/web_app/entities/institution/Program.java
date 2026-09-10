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
@ToString(exclude = {"department", "college"})
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id")
    private Department college;

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

    @Column(name = "chairperson_user_id")
    private Long chairpersonUserId;

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

    public void assignChairperson(Long chairpersonUserId) {
        this.chairpersonUserId = chairpersonUserId;
    }

    public void assignCollege(Department college) {
        this.college = college;
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
