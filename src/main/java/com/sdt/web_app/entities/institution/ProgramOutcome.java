package com.sdt.web_app.entities.institution;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(
        name = "program_outcomes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_program_pilo", columnNames = {"program_id", "code"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = "program")
public class ProgramOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    public void updateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Outcome description cannot be blank");
        }
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProgramOutcome that)) return false;
        String thisProgramCode = program != null ? program.getCode() : null;
        String thatProgramCode = that.program != null ? that.program.getCode() : null;

        return Objects.equals(code, that.code) && Objects.equals(thisProgramCode, thatProgramCode);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
