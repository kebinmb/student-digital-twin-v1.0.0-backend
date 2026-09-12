package com.sdt.web_app.entities.analytics;

import com.sdt.web_app.entities.enrollment.StudentProfile;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "student_risk_scores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class StudentRiskScore {

    public enum RiskLevel {
        LOW, MODERATE, HIGH, CRITICAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_profile_id", nullable = false)
    private StudentProfile student;

    @Column(name = "evaluated_at", updatable = false)
    @Builder.Default
    private Instant evaluatedAt = Instant.now();

    @Column(name = "academic_risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal academicRiskScore;

    @Column(name = "attendance_risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal attendanceRiskScore;

    @Column(name = "socioeconomic_risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal socioeconomicRiskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "composite_risk_level", nullable = false, length = 20)
    @Builder.Default
    private RiskLevel compositeRiskLevel = RiskLevel.LOW;

    @Column(name = "predicted_dropout_probability", nullable = false, precision = 5, scale = 4)
    private BigDecimal predictedDropoutProbability;

    @Column(name = "recommended_interventions", columnDefinition = "TEXT")
    private String recommendedInterventions;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StudentRiskScore that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
