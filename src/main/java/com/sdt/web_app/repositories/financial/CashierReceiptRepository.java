package com.sdt.web_app.repositories.financial;

import com.sdt.web_app.entities.financial.CashierReceipt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CashierReceiptRepository extends JpaRepository<CashierReceipt, Long> {

    @Query("SELECT r FROM CashierReceipt r WHERE r.orNumber = :orNumber")
    Optional<CashierReceipt> findByOrNumber(String orNumber);

    @Query("SELECT r FROM CashierReceipt r WHERE r.studentProfile.id = :studentProfileId ORDER BY r.issuedAt DESC")
    List<CashierReceipt> findByStudentProfileId(Long studentProfileId);

    Slice<CashierReceipt> findByStudentProfileId(Long studentProfileId, Pageable pageable);

    @Query("SELECT r FROM CashierReceipt r WHERE r.cashierUser.id = :cashierUserId ORDER BY r.issuedAt DESC")
    List<CashierReceipt> findByCashierUserId(Long cashierUserId);

    @Query("SELECT COUNT(r) FROM CashierReceipt r")
    long countTotalReceipts();
}
