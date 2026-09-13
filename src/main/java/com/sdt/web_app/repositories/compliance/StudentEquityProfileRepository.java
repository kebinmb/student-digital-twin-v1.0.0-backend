package com.sdt.web_app.repositories.compliance;

import com.sdt.web_app.entities.compliance.StudentEquityProfile;
import com.sdt.web_app.entities.compliance.StudentEquityProfile.EquityVerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Slice;

import java.util.Optional;

@Repository
public interface StudentEquityProfileRepository extends JpaRepository<StudentEquityProfile, Long> {

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
    """)
    Page<StudentEquityProfile> searchProfiles(
        @Param("search") String search,
        @Param("status") EquityVerificationStatus status,
        @Param("is4ps") Boolean is4ps,
        @Param("isIp") Boolean isIp,
        @Param("isPwd") Boolean isPwd,
        @Param("isGida") Boolean isGida,
        @Param("isFirstGen") Boolean isFirstGen,
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
    """)
    Slice<StudentEquityProfile> searchProfilesSlice(
        @Param("search") String search,
        @Param("status") EquityVerificationStatus status,
        @Param("is4ps") Boolean is4ps,
        @Param("isIp") Boolean isIp,
        @Param("isPwd") Boolean isPwd,
        @Param("isGida") Boolean isGida,
        @Param("isFirstGen") Boolean isFirstGen,
        Pageable pageable
    );

    long countByIs4psBeneficiaryTrue();

    long countByIsListahananNhtsTrue();

    long countByUnifastTesAwardeeTrue();

    long countByIsIndigenousPeopleTrue();

    long countByIsPersonWithDisabilityTrue();

    @Query("SELECT COUNT(e) FROM StudentEquityProfile e WHERE e.isSoloParentOrDependent = true")
    long countByIsSoloParentOrDependentTrue();

    long countByIsFirstGenerationCollegeTrue();

    long countByIsGidaResidentTrue();

    long countByVerificationStatus(EquityVerificationStatus status);
}
