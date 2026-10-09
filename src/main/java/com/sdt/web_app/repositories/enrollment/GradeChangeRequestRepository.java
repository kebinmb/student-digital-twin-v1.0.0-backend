package com.sdt.web_app.repositories.enrollment;

import com.sdt.web_app.entities.enrollment.GradeChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GradeChangeRequestRepository extends JpaRepository<GradeChangeRequest, Long> {

    List<GradeChangeRequest> findByStatus(GradeChangeRequest.Status status);

    @Query("SELECT gcr FROM GradeChangeRequest gcr " +
           "JOIN FETCH gcr.student s " +
           "JOIN FETCH s.user " +
           "LEFT JOIN FETCH s.program " +
           "LEFT JOIN FETCH s.college " +
           "JOIN FETCH gcr.course " +
           "JOIN FETCH gcr.term " +
           "JOIN FETCH gcr.requestedBy " +
           "WHERE gcr.status = :status ORDER BY gcr.createdAt DESC")
    List<GradeChangeRequest> findByStatusWithDetails(@Param("status") GradeChangeRequest.Status status);

    @Query("SELECT gcr FROM GradeChangeRequest gcr " +
           "JOIN FETCH gcr.student s " +
           "JOIN FETCH s.user " +
           "LEFT JOIN FETCH s.program " +
           "LEFT JOIN FETCH s.college " +
           "JOIN FETCH gcr.course " +
           "JOIN FETCH gcr.term " +
           "JOIN FETCH gcr.requestedBy " +
           "WHERE gcr.status = :status " +
           "AND (:termId IS NULL OR gcr.term.id = :termId) " +
           "ORDER BY gcr.createdAt DESC")
    List<GradeChangeRequest> findByStatusWithDetails(
            @Param("status") GradeChangeRequest.Status status,
            @Param("termId") Long termId);

    @Query("SELECT gcr FROM GradeChangeRequest gcr " +
           "JOIN FETCH gcr.student s " +
           "JOIN FETCH s.user " +
           "LEFT JOIN FETCH s.program " +
           "LEFT JOIN FETCH s.college " +
           "JOIN FETCH gcr.course " +
           "JOIN FETCH gcr.term " +
           "JOIN FETCH gcr.requestedBy " +
           "WHERE gcr.status = :status " +
           "AND s.program.id = :programId " +
           "AND (:termId IS NULL OR gcr.term.id = :termId) " +
           "ORDER BY gcr.createdAt DESC")
    List<GradeChangeRequest> findPendingByProgramId(
            @Param("status") GradeChangeRequest.Status status,
            @Param("programId") Long programId,
            @Param("termId") Long termId);

    @Query("SELECT gcr FROM GradeChangeRequest gcr " +
           "JOIN FETCH gcr.student s " +
           "JOIN FETCH s.user " +
           "LEFT JOIN FETCH s.program " +
           "LEFT JOIN FETCH s.college " +
           "JOIN FETCH gcr.course " +
           "JOIN FETCH gcr.term " +
           "JOIN FETCH gcr.requestedBy " +
           "WHERE gcr.status = :status " +
           "AND (s.college.id = :collegeId OR s.program.college.id = :collegeId OR s.program.department.id = :collegeId) " +
           "AND (:termId IS NULL OR gcr.term.id = :termId) " +
           "ORDER BY gcr.createdAt DESC")
    List<GradeChangeRequest> findPendingByCollegeId(
            @Param("status") GradeChangeRequest.Status status,
            @Param("collegeId") Long collegeId,
            @Param("termId") Long termId);

    @Query("SELECT gcr FROM GradeChangeRequest gcr " +
           "JOIN FETCH gcr.student s " +
           "JOIN FETCH s.user " +
           "LEFT JOIN FETCH s.program " +
           "LEFT JOIN FETCH s.college " +
           "JOIN FETCH gcr.course " +
           "JOIN FETCH gcr.term " +
           "JOIN FETCH gcr.requestedBy " +
           "WHERE gcr.status = :status " +
           "AND gcr.requestedBy.id = :userId " +
           "AND (:termId IS NULL OR gcr.term.id = :termId) " +
           "ORDER BY gcr.createdAt DESC")
    List<GradeChangeRequest> findPendingByRequestedById(
            @Param("status") GradeChangeRequest.Status status,
            @Param("userId") Long userId,
            @Param("termId") Long termId);

    List<GradeChangeRequest> findByStudentId(Long studentId);
}
