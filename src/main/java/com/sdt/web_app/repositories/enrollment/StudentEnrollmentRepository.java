package com.sdt.web_app.repositories.enrollment;

import com.sdt.web_app.entities.enrollment.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {

    List<StudentEnrollment> findByStudentId(Long studentId);

    Optional<StudentEnrollment> findByStudentIdAndTermId(Long studentId, Long termId);

    boolean existsByStudentIdAndTermId(Long studentId, Long termId);

    @Query("""
        SELECT se FROM StudentEnrollment se
        LEFT JOIN FETCH se.items item
        LEFT JOIN FETCH item.section sec
        LEFT JOIN FETCH sec.course
        WHERE se.student.id = :studentId
          AND se.term.id = :termId
    """)
    Optional<StudentEnrollment> findByStudentIdAndTermIdWithItems(@Param("studentId") Long studentId, @Param("termId") Long termId);
}
