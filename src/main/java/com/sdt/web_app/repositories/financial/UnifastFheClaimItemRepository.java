package com.sdt.web_app.repositories.financial;

import com.sdt.web_app.entities.financial.UnifastFheClaimItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnifastFheClaimItemRepository extends JpaRepository<UnifastFheClaimItem, Long> {

    List<UnifastFheClaimItem> findByClaimBatchId(Long claimBatchId);
}
