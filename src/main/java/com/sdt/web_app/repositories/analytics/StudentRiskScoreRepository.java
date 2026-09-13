package com.sdt.web_app.repositories.analytics;

import com.sdt.web_app.entities.analytics.StudentRiskScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRiskScoreRepository extends JpaRepository<StudentRiskScore, Long> {
    Optional<StudentRiskScore> findTopByStudentIdOrderByEvaluatedAtDesc(Long studentId);

    @Query("SELECT srs FROM StudentRiskScore srs " +
           "JOIN FETCH srs.student s " +
           "JOIN FETCH s.user " +
           "WHERE srs.compositeRiskLevel IN (:levels) " +
           "ORDER BY srs.predictedDropoutProbability DESC")
    List<StudentRiskScore> findByRiskLevelsWithDetails(@Param("levels") List<StudentRiskScore.RiskLevel> levels);

    Slice<StudentRiskScore> findByCompositeRiskLevelIn(List<StudentRiskScore.RiskLevel> levels, Pageable pageable);
}
