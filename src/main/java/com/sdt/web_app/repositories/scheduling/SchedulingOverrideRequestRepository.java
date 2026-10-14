package com.sdt.web_app.repositories.scheduling;

import com.sdt.web_app.entities.scheduling.OverrideRequestStatus;
import com.sdt.web_app.entities.scheduling.SchedulingOverrideRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchedulingOverrideRequestRepository extends JpaRepository<SchedulingOverrideRequest, Long>, JpaSpecificationExecutor<SchedulingOverrideRequest> {

    @Query("SELECT r FROM SchedulingOverrideRequest r " +
           "LEFT JOIN FETCH r.section sec " +
           "LEFT JOIN FETCH sec.course " +
           "LEFT JOIN FETCH sec.term " +
           "LEFT JOIN FETCH r.schedule " +
           "LEFT JOIN FETCH r.facultyUser fu " +
           "LEFT JOIN FETCH r.facultyProfile fp " +
           "LEFT JOIN FETCH r.requestedByUser " +
           "LEFT JOIN FETCH r.homeProgram " +
           "LEFT JOIN FETCH r.homeCollege " +
           "LEFT JOIN FETCH r.targetProgram " +
           "LEFT JOIN FETCH r.targetCollege " +
           "LEFT JOIN FETCH r.approvals a " +
           "LEFT JOIN FETCH a.assignedApproverUser " +
           "LEFT JOIN FETCH a.decisionByUser " +
           "WHERE r.id = :id")
    Optional<SchedulingOverrideRequest> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT r FROM SchedulingOverrideRequest r " +
           "LEFT JOIN FETCH r.section sec " +
           "LEFT JOIN FETCH sec.course " +
           "LEFT JOIN FETCH sec.term " +
           "LEFT JOIN FETCH r.schedule " +
           "LEFT JOIN FETCH r.facultyUser fu " +
           "LEFT JOIN FETCH r.facultyProfile fp " +
           "LEFT JOIN FETCH r.homeProgram " +
           "LEFT JOIN FETCH r.homeCollege " +
           "LEFT JOIN FETCH r.targetProgram " +
           "LEFT JOIN FETCH r.targetCollege " +
           "LEFT JOIN FETCH r.approvals a " +
           "WHERE r.section.id = :sectionId " +
           "ORDER BY r.createdAt DESC")
    List<SchedulingOverrideRequest> findBySectionIdWithDetails(@Param("sectionId") Long sectionId);

    @Query("SELECT r FROM SchedulingOverrideRequest r " +
           "LEFT JOIN FETCH r.section sec " +
           "LEFT JOIN FETCH sec.course " +
           "LEFT JOIN FETCH sec.term " +
           "LEFT JOIN FETCH r.schedule " +
           "LEFT JOIN FETCH r.facultyUser fu " +
           "LEFT JOIN FETCH r.facultyProfile fp " +
           "LEFT JOIN FETCH r.homeProgram " +
           "LEFT JOIN FETCH r.homeCollege " +
           "LEFT JOIN FETCH r.targetProgram " +
           "LEFT JOIN FETCH r.targetCollege " +
           "LEFT JOIN FETCH r.approvals a " +
           "WHERE r.schedule.id = :scheduleId " +
           "ORDER BY r.createdAt DESC")
    List<SchedulingOverrideRequest> findByScheduleIdWithDetails(@Param("scheduleId") Long scheduleId);

    List<SchedulingOverrideRequest> findBySectionIdAndStatus(Long sectionId, OverrideRequestStatus status);

    List<SchedulingOverrideRequest> findByFacultyUserIdAndStatus(Long facultyUserId, OverrideRequestStatus status);

    @Query("SELECT r FROM SchedulingOverrideRequest r " +
           "JOIN r.approvals a " +
           "WHERE a.assignedApproverUser.id = :userId AND a.status = 'PENDING' AND r.status = 'PENDING_APPROVAL'")
    List<SchedulingOverrideRequest> findPendingRequestsForApprover(@Param("userId") Long userId);

    @Query("SELECT COUNT(DISTINCT r.id) FROM SchedulingOverrideRequest r " +
           "JOIN r.approvals a " +
           "WHERE a.assignedApproverUser.id = :userId AND a.status = 'PENDING' AND r.status = 'PENDING_APPROVAL'")
    long countPendingRequestsForApprover(@Param("userId") Long userId);

    @Query("SELECT r FROM SchedulingOverrideRequest r " +
           "LEFT JOIN FETCH r.section sec " +
           "LEFT JOIN FETCH sec.course " +
           "LEFT JOIN FETCH sec.term " +
           "LEFT JOIN FETCH r.schedule " +
           "LEFT JOIN FETCH r.facultyUser fu " +
           "LEFT JOIN FETCH r.facultyProfile fp " +
           "LEFT JOIN FETCH r.homeProgram " +
           "LEFT JOIN FETCH r.homeCollege " +
           "LEFT JOIN FETCH r.targetProgram " +
           "LEFT JOIN FETCH r.targetCollege " +
           "LEFT JOIN FETCH r.approvals a " +
           "WHERE r.facultyUser.id = :facultyUserId " +
           "ORDER BY r.createdAt DESC")
    List<SchedulingOverrideRequest> findByFacultyUserIdWithDetails(@Param("facultyUserId") Long facultyUserId);
}
