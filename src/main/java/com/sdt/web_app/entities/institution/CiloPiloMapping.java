package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(
        name = "cilo_pilo_mappings",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_cilo_pilo_map", columnNames = {"course_outcome_id", "program_outcome_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"courseOutcome", "programOutcome"})
public class CiloPiloMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_outcome_id", nullable = false)
    private CourseOutcome courseOutcome;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_outcome_id", nullable = false)
    private ProgramOutcome programOutcome;

    @Column(name = "mapping_type", nullable = false, length = 10)
    private String mappingType; // 'I', 'E', 'D'

    public void updateMappingType(String mappingType) {
        if (mappingType == null || mappingType.isBlank()) {
            throw new IllegalArgumentException("Mapping type cannot be blank");
        }
        this.mappingType = mappingType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CiloPiloMapping that)) return false;

        Long thisCiloId = courseOutcome != null ? courseOutcome.getId() : null;
        Long thatCiloId = that.courseOutcome != null ? that.courseOutcome.getId() : null;
        Long thisPiloId = programOutcome != null ? programOutcome.getId() : null;
        Long thatPiloId = that.programOutcome != null ? that.programOutcome.getId() : null;

        return Objects.equals(thisCiloId, thatCiloId) && Objects.equals(thisPiloId, thatPiloId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
