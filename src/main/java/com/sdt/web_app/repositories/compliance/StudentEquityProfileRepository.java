package com.sdt.web_app.repositories.compliance;

import com.sdt.web_app.entities.compliance.StudentEquityProfile;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.EquityVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentEquityProfileRepository extends JpaRepository<StudentEquityProfile, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"studentProfile", "studentProfile.user", "studentProfile.program"})
    Optional<StudentEquityProfile> findByStudentProfileId(Long studentProfileId);

    Optional<StudentEquityProfile> findByStudentProfileUserUsername(String username);

    @Query("""
        SELECT e FROM StudentEquityProfile e
        JOIN e.studentProfile sp
        JOIN sp.user u
        WHERE (:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(sp.studentNumber) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:status IS NULL OR e.verificationStatus = :status)
          AND (:is4ps IS NULL OR e.is4psBeneficiary = :is4ps)
          AND (:isIp IS NULL OR e.isIndigenousPeople = :isIp)
          AND (:isPwd IS NULL OR e.isPersonWithDisability = :isPwd)
          AND (:isGida IS NULL OR e.isGidaResident = :isGida)
          AND (:isFirstGen IS NULL OR e.isFirstGenerationCollege = :isFirstGen)
          AND (:isSoloParent IS NULL OR e.isSoloParent = :isSoloParent)
          AND (:isFarmerFisherfolk IS NULL OR e.isFarmerFisherfolk = :isFarmerFisherfolk)
          AND (:isBottom40 IS NULL OR e.isBottom40IncomeBracket = :isBottom40)
    """)
    Page<StudentEquityProfile> searchProfiles(
        @Param("search") String search,
        @Param("status") EquityVerificationStatus status,
        @Param("is4ps") Boolean is4ps,
        @Param("isIp") Boolean isIp,
        @Param("isPwd") Boolean isPwd,
        @Param("isGida") Boolean isGida,
        @Param("isFirstGen") Boolean isFirstGen,
        @Param("isSoloParent") Boolean isSoloParent,
        @Param("isFarmerFisherfolk") Boolean isFarmerFisherfolk,
        @Param("isBottom40") Boolean isBottom40,
        Pageable pageable
    );

    @Query("""
        SELECT e FROM StudentEquityProfile e
        JOIN e.studentProfile sp
        JOIN sp.user u
        WHERE (:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(sp.studentNumber) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:status IS NULL OR e.verificationStatus = :status)
          AND (:is4ps IS NULL OR e.is4psBeneficiary = :is4ps)
          AND (:isIp IS NULL OR e.isIndigenousPeople = :isIp)
          AND (:isPwd IS NULL OR e.isPersonWithDisability = :isPwd)
          AND (:isGida IS NULL OR e.isGidaResident = :isGida)
          AND (:isFirstGen IS NULL OR e.isFirstGenerationCollege = :isFirstGen)
          AND (:isSoloParent IS NULL OR e.isSoloParent = :isSoloParent)
          AND (:isFarmerFisherfolk IS NULL OR e.isFarmerFisherfolk = :isFarmerFisherfolk)
          AND (:isBottom40 IS NULL OR e.isBottom40IncomeBracket = :isBottom40)
    """)
    Slice<StudentEquityProfile> searchProfilesSlice(
        @Param("search") String search,
        @Param("status") EquityVerificationStatus status,
        @Param("is4ps") Boolean is4ps,
        @Param("isIp") Boolean isIp,
        @Param("isPwd") Boolean isPwd,
        @Param("isGida") Boolean isGida,
        @Param("isFirstGen") Boolean isFirstGen,
        @Param("isSoloParent") Boolean isSoloParent,
        @Param("isFarmerFisherfolk") Boolean isFarmerFisherfolk,
        @Param("isBottom40") Boolean isBottom40,
        Pageable pageable
    );

    // Derived Spring Data JPA queries
    long countByIsPersonWithDisabilityTrue();

    long countByIsSoloParentTrue();

    long countByIsRaisedBySoloParentTrue();

    long countByIs4psBeneficiaryTrue();

    long countByIsListahananNhtsTrue();

    long countByUnifastTesAwardeeTrue();

    long countByIsIndigenousPeopleTrue();

    long countByIsOrphanTrue();

    long countByIsGidaResidentTrue();

    long countByIsFarmerFisherfolkTrue();

    long countByIsRebelReturneeFamilyTrue();

    long countByIsBottom40IncomeBracketTrue();

    long countByIsFirstGenerationCollegeTrue();

    long countByVerificationStatus(EquityVerificationStatus status);

    // UniFAST & CHED HEMIS E-Form Affirmative Action Projection
    interface AffirmativeActionStatisticsProjection {
        long getTotalCount();
        long getPwdCount();
        long getSoloParentCount();
        long getRaisedBySoloParentCount();
        long getFourPsCount();
        long getIpCount();
        long getOrphanCount();
        long getGidaCount();
        long getFarmerFisherfolkCount();
        long getRebelReturneeCount();
        long getBottom40Count();
        long getFirstGenCount();
    }

    @Query("""
        SELECT 
            COUNT(e.id) AS totalCount,
            SUM(CASE WHEN e.isPersonWithDisability = true THEN 1 ELSE 0 END) AS pwdCount,
            SUM(CASE WHEN e.isSoloParent = true THEN 1 ELSE 0 END) AS soloParentCount,
            SUM(CASE WHEN e.isRaisedBySoloParent = true THEN 1 ELSE 0 END) AS raisedBySoloParentCount,
            SUM(CASE WHEN e.is4psBeneficiary = true THEN 1 ELSE 0 END) AS fourPsCount,
            SUM(CASE WHEN e.isIndigenousPeople = true THEN 1 ELSE 0 END) AS ipCount,
            SUM(CASE WHEN e.isOrphan = true THEN 1 ELSE 0 END) AS orphanCount,
            SUM(CASE WHEN e.isGidaResident = true THEN 1 ELSE 0 END) AS gidaCount,
            SUM(CASE WHEN e.isFarmerFisherfolk = true THEN 1 ELSE 0 END) AS farmerFisherfolkCount,
            SUM(CASE WHEN e.isRebelReturneeFamily = true THEN 1 ELSE 0 END) AS rebelReturneeCount,
            SUM(CASE WHEN e.isBottom40IncomeBracket = true THEN 1 ELSE 0 END) AS bottom40Count,
            SUM(CASE WHEN e.isFirstGenerationCollege = true THEN 1 ELSE 0 END) AS firstGenCount
        FROM StudentEquityProfile e
    """)
    AffirmativeActionStatisticsProjection getAffirmativeActionStatistics();
}
