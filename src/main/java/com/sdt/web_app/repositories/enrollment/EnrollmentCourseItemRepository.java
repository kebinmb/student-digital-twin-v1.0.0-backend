package com.sdt.web_app.repositories.enrollment;

import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentCourseItemRepository extends JpaRepository<EnrollmentCourseItem, Long> {
    List<EnrollmentCourseItem> findByEnrollmentId(Long enrollmentId);
    List<EnrollmentCourseItem> findBySectionId(Long sectionId);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT eci FROM EnrollmentCourseItem eci " +
           "LEFT JOIN FETCH eci.enrollment e " +
           "LEFT JOIN FETCH e.student sp " +
           "LEFT JOIN FETCH sp.user u " +
           "LEFT JOIN FETCH sp.program p " +
           "WHERE eci.section.id = :sectionId")
    List<EnrollmentCourseItem> findBySectionIdWithStudentDetails(@org.springframework.data.repository.query.Param("sectionId") Long sectionId);

    boolean existsByEnrollmentIdAndSectionId(Long enrollmentId, Long sectionId);

    @org.springframework.data.jpa.repository.Query("SELECT eci FROM EnrollmentCourseItem eci " +
           "WHERE eci.enrollment.student.id = :studentId AND eci.section.course.id = :courseId")
    List<EnrollmentCourseItem> findByStudentIdAndCourseId(@org.springframework.data.repository.query.Param("studentId") Long studentId, @org.springframework.data.repository.query.Param("courseId") Long courseId);
}
