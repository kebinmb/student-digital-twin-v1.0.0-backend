package com.sdt.web_app.repositories.enrollment;

import com.sdt.web_app.entities.enrollment.StudentCourseGrade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentCourseGradeRepository extends JpaRepository<StudentCourseGrade, Long> {

    @EntityGraph(attributePaths = {"course"})
    List<StudentCourseGrade> findByStudentId(Long studentId);

    Optional<StudentCourseGrade> findByStudentIdAndCourseId(Long studentId, Long courseId);

    Optional<StudentCourseGrade> findByStudentIdAndCourseIdAndTermId(Long studentId, Long courseId, Long termId);

    @Query("""
        SELECT scg FROM StudentCourseGrade scg
        WHERE scg.student.id = :studentId
          AND scg.completionStatus = 'PASSED'
          AND scg.numericalGrade <= 3.00
    """)
    List<StudentCourseGrade> findPassedGradesByStudentId(@Param("studentId") Long studentId);

    @Query("""
        SELECT COUNT(scg) > 0 FROM StudentCourseGrade scg
        WHERE scg.student.id = :studentId
          AND scg.course.id = :courseId
          AND scg.completionStatus = 'PASSED'
          AND scg.numericalGrade <= 3.00
    """)
    boolean isCoursePassedByStudent(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}
