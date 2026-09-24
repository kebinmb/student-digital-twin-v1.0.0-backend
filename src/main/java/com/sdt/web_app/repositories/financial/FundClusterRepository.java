package com.sdt.web_app.repositories.financial;

import com.sdt.web_app.entities.financial.FundCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FundClusterRepository extends JpaRepository<FundCluster, Long> {
    Optional<FundCluster> findByCode(String code);
}
