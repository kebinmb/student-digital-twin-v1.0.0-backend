package com.sdt.web_app.repositories.financial;

import com.sdt.web_app.entities.financial.StudentAccountLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudentAccountLedgerRepository extends JpaRepository<StudentAccountLedger, Long> {

    Optional<StudentAccountLedger> findByTransactionNumber(String transactionNumber);

    @Query("SELECT l FROM StudentAccountLedger l WHERE l.studentProfile.id = :studentProfileId ORDER BY l.id ASC")
    List<StudentAccountLedger> findByStudentProfileIdOrderByIdAsc(Long studentProfileId);

    @Query("SELECT l FROM StudentAccountLedger l WHERE l.studentProfile.id = :studentProfileId ORDER BY l.id DESC")
    List<StudentAccountLedger> findLatestByStudentProfileId(Long studentProfileId);
}
