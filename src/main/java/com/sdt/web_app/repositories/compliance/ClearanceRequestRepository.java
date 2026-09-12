package com.sdt.web_app.repositories.compliance;

import com.sdt.web_app.entities.compliance.ClearanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClearanceRequestRepository extends JpaRepository<ClearanceRequest, Long> {

    @Query("SELECT r FROM ClearanceRequest r WHERE r.studentProfile.id = :studentProfileId ORDER BY r.createdAt DESC")
    List<ClearanceRequest> findByStudentProfileId(@Param("studentProfileId") Long studentProfileId);

    @Query("SELECT r FROM ClearanceRequest r WHERE r.studentProfile.id = :studentProfileId AND r.term.id = :termId")
    Optional<ClearanceRequest> findByStudentProfileIdAndTermId(
            @Param("studentProfileId") Long studentProfileId,
            @Param("termId") Long termId
    );

    @Query("SELECT r FROM ClearanceRequest r WHERE r.overallStatus = :status")
    List<ClearanceRequest> findByOverallStatus(@Param("status") String status);
}
