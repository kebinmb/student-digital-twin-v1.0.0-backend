package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.Curriculum;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {

    Optional<Curriculum> findByCode(String code);

    boolean existsByCode(String code);

    @EntityGraph(attributePaths = {"program", "major"})
    List<Curriculum> findByProgramId(Long programId);

    boolean existsByProgramId(Long programId);

    @EntityGraph(attributePaths = {"program", "major"})
    List<Curriculum> findByIsActiveTrue();

    @EntityGraph(attributePaths = {"program", "major"})
    List<Curriculum> findByIsActiveTrueOrderByCodeAsc();

    @EntityGraph(attributePaths = {"program", "major"})
    List<Curriculum> findByProgramIdInAndIsActiveTrueOrderByCodeAsc(List<Long> programIds);

    @EntityGraph(attributePaths = {"program", "major"})
    List<Curriculum> findByProgramIdAndIsActiveTrueOrderByCodeAsc(Long programId);

    @EntityGraph(attributePaths = {"program", "major"})
    List<Curriculum> findByProgramIdAndMajorIdAndIsActiveTrueOrderByCodeAsc(Long programId, Long majorId);

    @EntityGraph(attributePaths = {"program", "major"})
    List<Curriculum> findByProgramIdAndMajorIsNullAndIsActiveTrueOrderByCodeAsc(Long programId);
}
