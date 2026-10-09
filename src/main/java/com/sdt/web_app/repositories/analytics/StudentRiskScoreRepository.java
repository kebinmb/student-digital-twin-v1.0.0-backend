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

    @Query("SELECT srs FROM StudentRiskScore srs " +
           "WHERE srs.compositeRiskLevel IN (:levels) " +
           "AND srs.student.id IN (:studentIds)")
    Slice<StudentRiskScore> findByCompositeRiskLevelInAndStudentIds(@Param("levels") List<StudentRiskScore.RiskLevel> levels, @Param("studentIds") java.util.Collection<Long> studentIds, Pageable pageable);

    @Query("SELECT srs FROM StudentRiskScore srs " +
           "WHERE srs.compositeRiskLevel IN (:levels) " +
           "AND srs.student.program.id = :programId")
    Slice<StudentRiskScore> findByCompositeRiskLevelInAndProgramId(@Param("levels") List<StudentRiskScore.RiskLevel> levels, @Param("programId") Long programId, Pageable pageable);

    @Query("SELECT srs FROM StudentRiskScore srs " +
           "WHERE srs.compositeRiskLevel IN (:levels) " +
           "AND srs.student.program.id IN (:programIds)")
    Slice<StudentRiskScore> findByCompositeRiskLevelInAndProgramIds(@Param("levels") List<StudentRiskScore.RiskLevel> levels, @Param("programIds") java.util.Collection<Long> programIds, Pageable pageable);

    @Query("SELECT srs FROM StudentRiskScore srs " +
           "WHERE srs.student.id IN (:studentIds) " +
           "ORDER BY srs.evaluatedAt DESC")
    List<StudentRiskScore> findByStudentIdInOrderByEvaluatedAtDesc(@Param("studentIds") java.util.Collection<Long> studentIds);
}
