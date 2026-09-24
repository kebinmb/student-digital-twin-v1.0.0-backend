package com.sdt.web_app.repositories.financial;

import com.sdt.web_app.entities.financial.UnifastFheClaimItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UnifastFheClaimItemRepository extends JpaRepository<UnifastFheClaimItem, Long> {

    @Query("SELECT ci FROM UnifastFheClaimItem ci " +
           "JOIN FETCH ci.studentProfile sp " +
           "LEFT JOIN FETCH sp.program prog " +
           "LEFT JOIN FETCH sp.user usr " +
           "WHERE ci.claimBatch.id = :claimBatchId " +
           "ORDER BY sp.lastName ASC, sp.firstName ASC")
    List<UnifastFheClaimItem> findByClaimBatchId(@Param("claimBatchId") Long claimBatchId);
}
