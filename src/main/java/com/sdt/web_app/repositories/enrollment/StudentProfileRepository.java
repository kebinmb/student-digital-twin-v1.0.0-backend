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
}
