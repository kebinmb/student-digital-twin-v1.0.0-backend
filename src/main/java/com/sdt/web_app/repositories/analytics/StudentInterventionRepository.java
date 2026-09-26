package com.sdt.web_app.repositories.analytics;

import com.sdt.web_app.entities.analytics.StudentIntervention;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface StudentInterventionRepository extends JpaRepository<StudentIntervention, Long> {

    List<StudentIntervention> findByStudentIdOrderByDispatchedAtDesc(Long studentId);

    Slice<StudentIntervention> findByStudentId(Long studentId, Pageable pageable);

    Slice<StudentIntervention> findByStatus(StudentIntervention.InterventionStatus status, Pageable pageable);

    Slice<StudentIntervention> findByAssignedCounselorId(Long counselorId, Pageable pageable);

    boolean existsByStudentIdAndStatusIn(Long studentId, Collection<StudentIntervention.InterventionStatus> statuses);

    @Query("SELECT si FROM StudentIntervention si " +
           "JOIN FETCH si.student s " +
           "LEFT JOIN FETCH si.assignedCounselor c " +
           "LEFT JOIN FETCH si.riskScore rs " +
           "WHERE (:status IS NULL OR si.status = :status)")
    Page<StudentIntervention> findAllWithDetails(@Param("status") StudentIntervention.InterventionStatus status, Pageable pageable);
}
