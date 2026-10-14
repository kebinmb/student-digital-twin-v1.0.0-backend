package com.sdt.web_app.repositories.scheduling;

import com.sdt.web_app.entities.scheduling.ApprovalStepStatus;
import com.sdt.web_app.entities.scheduling.SchedulingOverrideApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchedulingOverrideApprovalRepository extends JpaRepository<SchedulingOverrideApproval, Long> {

    List<SchedulingOverrideApproval> findByRequestIdOrderByStepOrderAsc(Long requestId);

    List<SchedulingOverrideApproval> findByAssignedApproverUserIdAndStatus(Long approverUserId, ApprovalStepStatus status);

    @Query("SELECT a FROM SchedulingOverrideApproval a " +
           "JOIN FETCH a.request r " +
           "JOIN FETCH r.section sec " +
           "JOIN FETCH sec.course " +
           "JOIN FETCH sec.term " +
           "JOIN FETCH r.facultyUser fu " +
           "LEFT JOIN FETCH r.facultyProfile fp " +
           "WHERE a.assignedApproverUser.id = :approverUserId AND a.status = :status")
    List<SchedulingOverrideApproval> findByAssignedApproverWithDetails(
            @Param("approverUserId") Long approverUserId,
            @Param("status") ApprovalStepStatus status);
}
