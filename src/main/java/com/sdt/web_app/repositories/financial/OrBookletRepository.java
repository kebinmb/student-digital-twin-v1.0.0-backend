package com.sdt.web_app.repositories.financial;

import com.sdt.web_app.entities.financial.OrBooklet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrBookletRepository extends JpaRepository<OrBooklet, Long> {

    Optional<OrBooklet> findByBookletCode(String bookletCode);

    @Query("SELECT b FROM OrBooklet b WHERE b.assignedCashier.id = :cashierUserId AND b.status = 'ACTIVE'")
    Optional<OrBooklet> findActiveBookletByCashierId(@Param("cashierUserId") Long cashierUserId);

    @Query("SELECT b FROM OrBooklet b WHERE b.assignedCashier.id = :cashierUserId")
    List<OrBooklet> findByAssignedCashierId(@Param("cashierUserId") Long cashierUserId);

    @Query(value = "SELECT b FROM OrBooklet b WHERE b.assignedCashier.id = :cashierUserId",
           countQuery = "SELECT count(b) FROM OrBooklet b WHERE b.assignedCashier.id = :cashierUserId")
    org.springframework.data.domain.Page<OrBooklet> findByAssignedCashierId(@Param("cashierUserId") Long cashierUserId, org.springframework.data.domain.Pageable pageable);
}
