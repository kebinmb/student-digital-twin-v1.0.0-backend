package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.CiloPiloMapping;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CiloPiloMappingRepository extends JpaRepository<CiloPiloMapping, Long> {

    Optional<CiloPiloMapping> findByCourseOutcomeIdAndProgramOutcomeId(Long courseOutcomeId, Long programOutcomeId);

    boolean existsByCourseOutcomeIdAndProgramOutcomeId(Long courseOutcomeId, Long programOutcomeId);

    @EntityGraph(attributePaths = {"courseOutcome", "programOutcome"})
    List<CiloPiloMapping> findByCourseOutcomeId(Long courseOutcomeId);

    @EntityGraph(attributePaths = {"courseOutcome", "programOutcome"})
    List<CiloPiloMapping> findByProgramOutcomeId(Long programOutcomeId);

    @EntityGraph(attributePaths = {"courseOutcome", "programOutcome"})
    @Query("SELECT m FROM CiloPiloMapping m WHERE m.courseOutcome.course.id = :courseId")
    List<CiloPiloMapping> findByCourseId(@Param("courseId") Long courseId);

    @EntityGraph(attributePaths = {"courseOutcome", "programOutcome"})
    @Query("SELECT m FROM CiloPiloMapping m WHERE m.programOutcome.program.id = :programId")
    List<CiloPiloMapping> findByProgramId(@Param("programId") Long programId);

    @EntityGraph(attributePaths = {"courseOutcome", "programOutcome"})
    @Query("SELECT m FROM CiloPiloMapping m WHERE m.courseOutcome.course.id = :courseId AND m.programOutcome.program.id = :programId")
    List<CiloPiloMapping> findByCourseIdAndProgramId(@Param("courseId") Long courseId, @Param("programId") Long programId);

    boolean existsByCourseOutcomeId(Long courseOutcomeId);

    boolean existsByProgramOutcomeId(Long programOutcomeId);
}
