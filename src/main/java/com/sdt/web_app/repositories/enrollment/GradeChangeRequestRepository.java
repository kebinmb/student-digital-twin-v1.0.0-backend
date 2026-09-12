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
           "JOIN FETCH gcr.course " +
           "JOIN FETCH gcr.term " +
           "JOIN FETCH gcr.requestedBy " +
           "WHERE gcr.status = :status ORDER BY gcr.createdAt DESC")
    List<GradeChangeRequest> findByStatusWithDetails(@Param("status") GradeChangeRequest.Status status);

    List<GradeChangeRequest> findByStudentId(Long studentId);
}
