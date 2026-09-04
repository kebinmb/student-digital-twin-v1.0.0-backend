package com.sdt.web_app.repositories.enrollment;

import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrollmentCourseItemRepository extends JpaRepository<EnrollmentCourseItem, Long> {
    List<EnrollmentCourseItem> findByEnrollmentId(Long enrollmentId);
    List<EnrollmentCourseItem> findBySectionId(Long sectionId);
    boolean existsByEnrollmentIdAndSectionId(Long enrollmentId, Long sectionId);
}
