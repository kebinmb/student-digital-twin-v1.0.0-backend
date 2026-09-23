package com.sdt.web_app.repositories.financial;

import com.sdt.web_app.entities.financial.VoidedOfficialReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoidedOfficialReceiptRepository extends JpaRepository<VoidedOfficialReceipt, Long> {

    @Query("SELECT v FROM VoidedOfficialReceipt v WHERE v.orNumber = :orNumber")
    Optional<VoidedOfficialReceipt> findByOrNumber(@Param("orNumber") String orNumber);

    @Query("SELECT v FROM VoidedOfficialReceipt v WHERE v.booklet.id = :bookletId")
    List<VoidedOfficialReceipt> findByBookletId(@Param("bookletId") Long bookletId);

    @Query("SELECT v FROM VoidedOfficialReceipt v WHERE v.voidedByCashier.id = :cashierUserId")
    List<VoidedOfficialReceipt> findByVoidedByCashierId(@Param("cashierUserId") Long cashierUserId);
}
