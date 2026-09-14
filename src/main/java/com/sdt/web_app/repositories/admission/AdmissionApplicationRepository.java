package com.sdt.web_app.repositories.admission;

import com.sdt.web_app.entities.admission.AdmissionApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdmissionApplicationRepository extends JpaRepository<AdmissionApplication, Long>, JpaSpecificationExecutor<AdmissionApplication> {

    Optional<AdmissionApplication> findByApplicationNumber(String applicationNumber);

    Optional<AdmissionApplication> findByEmailAndTermId(String email, Long termId);

    boolean existsByEmailAndTermId(String email, Long termId);

    boolean existsByEmailIgnoreCaseAndTermId(String email, Long termId);

    boolean existsByLrnNumberAndTermId(String lrnNumber, Long termId);

    List<AdmissionApplication> findByTermId(Long termId);

    List<AdmissionApplication> findByTermIdAndApplicationStatus(Long termId, AdmissionApplication.ApplicationStatus status);

    @Query("SELECT COUNT(a) FROM AdmissionApplication a WHERE a.term.id = :termId")
    long countByTermId(@Param("termId") Long termId);

    @Query("SELECT a FROM AdmissionApplication a WHERE " +
           ":query IS NULL OR :query = '' OR " +
           "LOWER(a.applicationNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(CONCAT(a.firstName, ' ', a.lastName)) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<AdmissionApplication> searchKeyword(@Param("query") String query);

    Optional<AdmissionApplication> findFirstByEmailIgnoreCase(String email);

    Optional<AdmissionApplication> findFirstByApplicationNumber(String applicationNumber);

    @Query("SELECT a FROM AdmissionApplication a WHERE " +
           "(a.applicationStatus = com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.APPROVED " +
           " OR a.applicationStatus = com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.ELIGIBLE_FOR_ENROLLMENT " +
           " OR a.applicationStatus = com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.INTERVIEW_ACCEPTED) " +
           "AND a.applicationStatus != com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.ENROLLED " +
           "AND a.isEnrolled = false " +
           "AND (:termId IS NULL OR a.term.id = :termId) " +
           "AND NOT EXISTS (SELECT 1 FROM StudentProfile s WHERE s.admissionApplicationId = a.id " +
           "  OR (a.email IS NOT NULL AND a.email != '' AND LOWER(s.user.email) = LOWER(a.email)) " +
           "  OR s.studentNumber = a.applicationNumber) " +
           "ORDER BY a.createdAt DESC")
    List<AdmissionApplication> findUnclaimedApprovedApplications(@Param("termId") Long termId);
}
