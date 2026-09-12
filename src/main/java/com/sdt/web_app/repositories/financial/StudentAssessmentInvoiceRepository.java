package com.sdt.web_app.repositories.financial;

import com.sdt.web_app.entities.financial.StudentAssessmentInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudentAssessmentInvoiceRepository extends JpaRepository<StudentAssessmentInvoice, Long> {

    Optional<StudentAssessmentInvoice> findByInvoiceNumber(String invoiceNumber);

    Optional<StudentAssessmentInvoice> findByStudentEnrollmentId(Long enrollmentId);

    @Query("SELECT i FROM StudentAssessmentInvoice i WHERE i.studentProfile.id = :studentProfileId ORDER BY i.createdAt DESC")
    List<StudentAssessmentInvoice> findByStudentProfileId(Long studentProfileId);

    @Query("SELECT i FROM StudentAssessmentInvoice i WHERE i.studentProfile.id = :studentProfileId AND i.term.id = :termId")
    Optional<StudentAssessmentInvoice> findByStudentProfileIdAndTermId(Long studentProfileId, Long termId);

    @Query("SELECT i FROM StudentAssessmentInvoice i WHERE i.term.id = :termId AND i.fheEligible = true AND i.status != 'VOID'")
    List<StudentAssessmentInvoice> findFheEligibleInvoicesByTerm(Long termId);
}
