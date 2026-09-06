package com.sdt.web_app.repositories.scheduling;

import com.sdt.web_app.entities.scheduling.ClassSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSectionRepository extends JpaRepository<ClassSection, Long> {

    List<ClassSection> findByTermId(Long termId);

    boolean existsByTermId(Long termId);

    List<ClassSection> findByTermIdAndCurriculumId(Long termId, Long curriculumId);

    List<ClassSection> findByTermIdAndCourseId(Long termId, Long courseId);

    Optional<ClassSection> findByTermIdAndCourseIdAndSectionCode(Long termId, Long courseId, String sectionCode);

    boolean existsByTermIdAndCourseIdAndSectionCode(Long termId, Long courseId, String sectionCode);

    @Query("SELECT s FROM ClassSection s LEFT JOIN FETCH s.schedules sched LEFT JOIN FETCH sched.room WHERE s.term.id = :termId")
    List<ClassSection> findAllWithSchedulesByTermId(@Param("termId") Long termId);

    @Query("SELECT s FROM ClassSection s LEFT JOIN FETCH s.schedules sched LEFT JOIN FETCH sched.room WHERE s.id = :id")
    Optional<ClassSection> findByIdWithSchedules(@Param("id") Long id);

    @Modifying
    @Query("""
        UPDATE ClassSection s 
        SET s.enrolledCount = s.enrolledCount + 1,
            s.status = CASE WHEN (s.enrolledCount + 1) >= s.maxCapacity THEN com.sdt.web_app.entities.scheduling.ClassSection.Status.CLOSED ELSE s.status END
        WHERE s.id = :id 
          AND s.enrolledCount < s.maxCapacity 
          AND s.status = com.sdt.web_app.entities.scheduling.ClassSection.Status.OPEN
    """)
    int incrementEnrolledCountIfOpen(@Param("id") Long id);

    @Modifying
    @Query("""
        UPDATE ClassSection s
        SET s.enrolledCount = s.enrolledCount - 1,
            s.status = CASE WHEN s.status = com.sdt.web_app.entities.scheduling.ClassSection.Status.CLOSED THEN com.sdt.web_app.entities.scheduling.ClassSection.Status.OPEN ELSE s.status END
        WHERE s.id = :id AND s.enrolledCount > 0
    """)
    int decrementEnrolledCount(@Param("id") Long id);
}
