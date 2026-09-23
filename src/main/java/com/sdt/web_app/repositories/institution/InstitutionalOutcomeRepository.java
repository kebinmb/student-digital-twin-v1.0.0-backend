package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.InstitutionalOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstitutionalOutcomeRepository extends JpaRepository<InstitutionalOutcome, Long> {

    Optional<InstitutionalOutcome> findByCode(String code);

    List<InstitutionalOutcome> findByActiveTrue();
}
