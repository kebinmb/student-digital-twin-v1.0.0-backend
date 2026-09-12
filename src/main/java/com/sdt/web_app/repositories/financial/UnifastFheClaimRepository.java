package com.sdt.web_app.repositories.financial;

import com.sdt.web_app.entities.financial.UnifastFheClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UnifastFheClaimRepository extends JpaRepository<UnifastFheClaim, Long> {

    Optional<UnifastFheClaim> findByClaimBatchNumber(String claimBatchNumber);

    @Query("SELECT c FROM UnifastFheClaim c WHERE c.term.id = :termId ORDER BY c.createdAt DESC")
    List<UnifastFheClaim> findByTermId(Long termId);
}
