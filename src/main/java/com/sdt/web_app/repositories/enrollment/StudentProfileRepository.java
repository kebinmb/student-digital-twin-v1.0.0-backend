package com.sdt.web_app.repositories.enrollment;

import com.sdt.web_app.entities.enrollment.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    Optional<StudentProfile> findByUserId(Long userId);

    Optional<StudentProfile> findByStudentNumber(String studentNumber);

    @Query("SELECT sp FROM StudentProfile sp JOIN FETCH sp.program JOIN FETCH sp.curriculum WHERE sp.id = :id")
    Optional<StudentProfile> findByIdWithProgramAndCurriculum(@Param("id") Long id);

    @Query("SELECT sp FROM StudentProfile sp JOIN FETCH sp.program JOIN FETCH sp.curriculum WHERE sp.user.id = :userId")
    Optional<StudentProfile> findByUserIdWithProgramAndCurriculum(@Param("userId") Long userId);

    boolean existsByStudentNumber(String studentNumber);

    @Query("SELECT sp FROM StudentProfile sp LEFT JOIN FETCH sp.program LEFT JOIN FETCH sp.user " +
           "WHERE :query IS NULL OR :query = '' " +
           "OR LOWER(sp.studentNumber) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(sp.user.username) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(sp.program.code) LIKE LOWER(CONCAT('%', :query, '%'))")
    java.util.List<StudentProfile> searchStudents(@Param("query") String query);
}
