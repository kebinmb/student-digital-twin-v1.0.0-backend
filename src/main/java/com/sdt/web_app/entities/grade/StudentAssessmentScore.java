package com.sdt.web_app.entities.grade;

import com.sdt.web_app.entities.enrollment.StudentProfile;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "student_assessment_scores",
       uniqueConstraints = {@UniqueConstraint(name = "uq_student_item", columnNames = {"item_id", "student_id"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"item", "student"})
public class StudentAssessmentScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ClassRecordItem item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    @Column(name = "score_earned", precision = 6, scale = 2)
    private BigDecimal scoreEarned;

    @Column(name = "is_excused", nullable = false)
    @Builder.Default
    private boolean isExcused = false;

    public void updateScore(BigDecimal scoreEarned, boolean isExcused) {
        if (isExcused) {
            this.isExcused = true;
            this.scoreEarned = null;
            return;
        }
        this.isExcused = false;
        if (scoreEarned != null) {
            if (scoreEarned.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Score earned cannot be negative");
            }
            if (this.item != null && this.item.getMaxPoints() != null && scoreEarned.compareTo(this.item.getMaxPoints()) > 0) {
                throw new IllegalArgumentException("Score earned (" + scoreEarned + ") exceeds max points (" + this.item.getMaxPoints() + ")");
            }
        }
        this.scoreEarned = scoreEarned;
    }
}
