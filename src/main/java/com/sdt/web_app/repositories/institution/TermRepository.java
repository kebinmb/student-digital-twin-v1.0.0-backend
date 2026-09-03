package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.institution.TermType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TermRepository extends JpaRepository<Term, Long> {

    Optional<Term> findByAcademicYearIdAndTermType(Long academicYearId, TermType termType);

    boolean existsByAcademicYearIdAndTermType(Long academicYearId, TermType termType);

    boolean existsByAcademicYearId(Long academicYearId);

    @EntityGraph(attributePaths = {"academicYear"})
    Optional<Term> findWithAcademicYearById(Long id);

    @EntityGraph(attributePaths = {"academicYear"})
    List<Term> findByAcademicYearId(Long academicYearId);

    @EntityGraph(attributePaths = {"academicYear"})
    Optional<Term> findByIsActiveTrue();

    @EntityGraph(attributePaths = {"academicYear"})
    List<Term> findByEnrollmentOpenTrue();

    @EntityGraph(attributePaths = {"academicYear"})
    List<Term> findByGradingOpenTrue();
}
