package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.ProgramOutcome;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProgramOutcomeRepository extends JpaRepository<ProgramOutcome, Long> {

    Optional<ProgramOutcome> findByProgramIdAndCode(Long programId, String code);

    boolean existsByProgramIdAndCode(Long programId, String code);

    @EntityGraph(attributePaths = {"program"})
    List<ProgramOutcome> findByProgramId(Long programId);
}
