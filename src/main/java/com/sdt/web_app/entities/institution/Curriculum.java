package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(name = "curricula")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = "program")
public class Curriculum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(unique = true, nullable = false, length = 30, updatable = false)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "effective_academic_year", nullable = false, length = 20)
    private String effectiveAcademicYear;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public void updateCurriculumInfo(String name, String effectiveAcademicYear) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Curriculum name cannot be blank");
        }
        if (effectiveAcademicYear == null || effectiveAcademicYear.isBlank()) {
            throw new IllegalArgumentException("Effective academic year cannot be blank");
        }
        this.name = name;
        this.effectiveAcademicYear = effectiveAcademicYear;
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
        if (!(o instanceof Curriculum curriculum)) return false;
        return code != null && Objects.equals(code, curriculum.code);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
