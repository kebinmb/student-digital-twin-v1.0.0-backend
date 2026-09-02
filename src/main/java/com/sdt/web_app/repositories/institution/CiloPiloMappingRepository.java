package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.CiloPiloMapping;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
