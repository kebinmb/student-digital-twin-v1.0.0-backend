package com.sdt.web_app.repositories.enrollment;

import com.sdt.web_app.entities.enrollment.CourseEquivalency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseEquivalencyRepository extends JpaRepository<CourseEquivalency, Long> {

    List<CourseEquivalency> findByStudentId(Long studentId);

    @Query("SELECT ce FROM CourseEquivalency ce JOIN FETCH ce.internalCourse WHERE ce.student.id = :studentId")
    List<CourseEquivalency> findByStudentIdWithCourses(@Param("studentId") Long studentId);

    boolean existsByStudentIdAndInternalCourseId(Long studentId, Long internalCourseId);
}
