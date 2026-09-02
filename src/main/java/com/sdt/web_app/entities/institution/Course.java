package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 30, updatable = false)
    private String code;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "lecture_units", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal lectureUnits = BigDecimal.ZERO;

    @Column(name = "lab_units", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal labUnits = BigDecimal.ZERO;

    @Column(name = "credit_units", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal creditUnits = BigDecimal.ZERO;

    @Column(name = "contact_hours_lec", nullable = false)
    @Builder.Default
    private int contactHoursLec = 0;

    @Column(name = "contact_hours_lab", nullable = false)
    @Builder.Default
    private int contactHoursLab = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    public void updateCourseDetails(String title, BigDecimal lecUnits, BigDecimal labUnits, int contactLec, int contactLab, String description) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Course title cannot be blank");
        }
        if (lecUnits == null || labUnits == null) {
            throw new IllegalArgumentException("Units cannot be null");
        }
        this.title = title;
        this.lectureUnits = lecUnits;
        this.labUnits = labUnits;
        this.creditUnits = lecUnits.add(labUnits);
        this.contactHoursLec = contactLec;
        this.contactHoursLab = contactLab;
        this.description = description;
    }

    public void activate() { this.isActive = true; }
    public void deactivate() { this.isActive = false; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course course)) return false;
        return code != null && Objects.equals(code, course.code);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}