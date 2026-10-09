package com.sdt.web_app.repositories.admission;

import com.sdt.web_app.entities.admission.AdmissionApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    List<AdmissionApplication> findByApplicationStatus(AdmissionApplication.ApplicationStatus status);

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
    
    @Query(value = """
        SELECT a FROM AdmissionApplication a
        JOIN a.targetProgram p
        JOIN a.term t
        WHERE a.isEnrolled = false
          AND a.applicationStatus != com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.ENROLLED
          AND (a.examScore IS NOT NULL OR a.applicationStatus IN (
              com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.EXAM_PASSED,
              com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.EXAM_FAILED,
              com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.INTERVIEW_ACCEPTED,
              com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.ELIGIBLE_FOR_ENROLLMENT,
              com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.APPROVED
          ))
          AND (:search IS NULL OR :search = ''
               OR LOWER(a.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(a.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(a.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(CONCAT(a.firstName, ' ', a.lastName)) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:status IS NULL OR a.applicationStatus = :status)
          AND (:is4ps IS NULL OR a.is4psBeneficiary = :is4ps)
          AND (:isIp IS NULL OR a.isIndigenousPeople = :isIp)
          AND (:isPwd IS NULL OR a.isPersonWithDisability = :isPwd)
          AND (:isSoloParent IS NULL OR (a.isSoloParent = :isSoloParent OR a.isRaisedBySoloParent = :isSoloParent))
          AND (:isFarmerFisherfolk IS NULL OR a.isFarmerFisherfolk = :isFarmerFisherfolk)
          AND (:isBottom40 IS NULL OR a.isBottom40IncomeBracket = :isBottom40)
          AND (:isGida IS NULL OR a.isGidaResident = :isGida)
          AND (:isFirstGen IS NULL OR a.isFirstGenerationCollege = :isFirstGen)
    """,
    countQuery = """
        SELECT COUNT(a) FROM AdmissionApplication a
        WHERE a.isEnrolled = false
          AND a.applicationStatus != com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.ENROLLED
          AND (a.examScore IS NOT NULL OR a.applicationStatus IN (
              com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.EXAM_PASSED,
              com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.EXAM_FAILED,
              com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.INTERVIEW_ACCEPTED,
              com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.ELIGIBLE_FOR_ENROLLMENT,
              com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.APPROVED
          ))
          AND (:search IS NULL OR :search = ''
               OR LOWER(a.applicationNumber) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(a.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(a.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(CONCAT(a.firstName, ' ', a.lastName)) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:status IS NULL OR a.applicationStatus = :status)
          AND (:is4ps IS NULL OR a.is4psBeneficiary = :is4ps)
          AND (:isIp IS NULL OR a.isIndigenousPeople = :isIp)
          AND (:isPwd IS NULL OR a.isPersonWithDisability = :isPwd)
          AND (:isSoloParent IS NULL OR (a.isSoloParent = :isSoloParent OR a.isRaisedBySoloParent = :isSoloParent))
          AND (:isFarmerFisherfolk IS NULL OR a.isFarmerFisherfolk = :isFarmerFisherfolk)
          AND (:isBottom40 IS NULL OR a.isBottom40IncomeBracket = :isBottom40)
          AND (:isGida IS NULL OR a.isGidaResident = :isGida)
          AND (:isFirstGen IS NULL OR a.isFirstGenerationCollege = :isFirstGen)
    """)
    Page<AdmissionApplication> searchPostExamApplicationsForEquityAudit(
        @Param("search") String search,
        @Param("status") AdmissionApplication.ApplicationStatus status,
        @Param("is4ps") Boolean is4ps,
        @Param("isIp") Boolean isIp,
        @Param("isPwd") Boolean isPwd,
        @Param("isSoloParent") Boolean isSoloParent,
        @Param("isFarmerFisherfolk") Boolean isFarmerFisherfolk,
        @Param("isBottom40") Boolean isBottom40,
        @Param("isGida") Boolean isGida,
        @Param("isFirstGen") Boolean isFirstGen,
        Pageable pageable
    );

    interface PostExamApplicantEquityStatisticsProjection {
        Long getTotalPostExamCount();
        Long getExamPassedCount();
        Long getExamFailedCount();
        Long getFourPsCount();
        Long getIpCount();
        Long getPwdCount();
        Long getSoloParentCount();
        Long getOrphanCount();
        Long getGidaCount();
        Long getFarmerFisherfolkCount();
        Long getBottom40Count();
        Long getFirstGenCount();
    }

    @Query("""
        SELECT 
            COUNT(a.id) AS totalPostExamCount,
            COALESCE(SUM(CASE WHEN a.applicationStatus = com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.EXAM_PASSED 
                          OR a.applicationStatus = com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.INTERVIEW_ACCEPTED
                          OR a.applicationStatus = com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.ELIGIBLE_FOR_ENROLLMENT
                          OR a.applicationStatus = com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.APPROVED THEN 1L ELSE 0L END), 0L) AS examPassedCount,
            COALESCE(SUM(CASE WHEN a.applicationStatus = com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.EXAM_FAILED THEN 1L ELSE 0L END), 0L) AS examFailedCount,
            COALESCE(SUM(CASE WHEN a.is4psBeneficiary = true THEN 1L ELSE 0L END), 0L) AS fourPsCount,
            COALESCE(SUM(CASE WHEN a.isIndigenousPeople = true THEN 1L ELSE 0L END), 0L) AS ipCount,
            COALESCE(SUM(CASE WHEN a.isPersonWithDisability = true THEN 1L ELSE 0L END), 0L) AS pwdCount,
            COALESCE(SUM(CASE WHEN a.isSoloParent = true OR a.isRaisedBySoloParent = true THEN 1L ELSE 0L END), 0L) AS soloParentCount,
            COALESCE(SUM(CASE WHEN a.isOrphan = true THEN 1L ELSE 0L END), 0L) AS orphanCount,
            COALESCE(SUM(CASE WHEN a.isGidaResident = true THEN 1L ELSE 0L END), 0L) AS gidaCount,
            COALESCE(SUM(CASE WHEN a.isFarmerFisherfolk = true THEN 1L ELSE 0L END), 0L) AS farmerFisherfolkCount,
            COALESCE(SUM(CASE WHEN a.isBottom40IncomeBracket = true THEN 1L ELSE 0L END), 0L) AS bottom40Count,
            COALESCE(SUM(CASE WHEN a.isFirstGenerationCollege = true THEN 1L ELSE 0L END), 0L) AS firstGenCount
        FROM AdmissionApplication a
        WHERE a.isEnrolled = false
          AND a.applicationStatus != com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.ENROLLED
          AND (a.examScore IS NOT NULL OR a.applicationStatus IN (
              com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.EXAM_PASSED,
              com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.EXAM_FAILED,
              com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.INTERVIEW_ACCEPTED,
              com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.ELIGIBLE_FOR_ENROLLMENT,
              com.sdt.web_app.entities.admission.AdmissionApplication.ApplicationStatus.APPROVED
          ))
    """)
    PostExamApplicantEquityStatisticsProjection getPostExamApplicantEquityStatistics();
}
