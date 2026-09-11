package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.BatchSize;

import java.util.Objects;

@Entity
@Table(
        name = "curriculum_courses",
        uniqueConstraints = @UniqueConstraint(name = "uq_curriculum_course", columnNames = {"curriculum_id", "course_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"curriculum", "course"})
@BatchSize(size = 50)
public class CurriculumCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curriculum_id", nullable = false)
    private Curriculum curriculum;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "year_level", nullable = false)
    private int yearLevel;

    @Column(nullable = false, length = 20)
    private String semester; // '1ST_SEM', '2ND_SEM', 'SUMMER'

    @Column(name = "sequence_order", nullable = false)
    @Builder.Default
    private int sequenceOrder = 1;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String category = "PROFESSIONAL_MAJOR"; // GEN_ED, PROFESSIONAL_MAJOR, ELECTIVE, MANDATED

    public void relocatePosition(int yearLevel, String semester, int sequenceOrder) {
        this.yearLevel = yearLevel;
        this.semester = semester;
        this.sequenceOrder = sequenceOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CurriculumCourse that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}