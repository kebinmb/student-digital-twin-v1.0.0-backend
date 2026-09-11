package com.sdt.web_app.repositories.scheduling;

import com.sdt.web_app.entities.scheduling.ClassSection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSectionRepository extends JpaRepository<ClassSection, Long>, JpaSpecificationExecutor<ClassSection> {

    @EntityGraph(attributePaths = {"term", "course", "curriculum", "curriculum.program", "primaryInstructor"})
    @Override
    List<ClassSection> findAll(org.springframework.data.jpa.domain.Specification<ClassSection> spec);

    List<ClassSection> findByTermId(Long termId);

    boolean existsByTermId(Long termId);

    List<ClassSection> findByTermIdAndCurriculumId(Long termId, Long curriculumId);

    List<ClassSection> findByTermIdAndCourseId(Long termId, Long courseId);

    Optional<ClassSection> findByTermIdAndCourseIdAndSectionCode(Long termId, Long courseId, String sectionCode);

    boolean existsByTermIdAndCourseIdAndSectionCode(Long termId, Long courseId, String sectionCode);

    @Query("SELECT DISTINCT s FROM ClassSection s JOIN FETCH s.curriculum cur JOIN FETCH cur.program p JOIN FETCH s.course c LEFT JOIN FETCH s.schedules sched LEFT JOIN FETCH sched.room WHERE s.term.id = :termId")
    List<ClassSection> findAllWithSchedulesByTermId(@Param("termId") Long termId);

    @Query("SELECT s FROM ClassSection s JOIN FETCH s.term JOIN FETCH s.course JOIN FETCH s.curriculum cur JOIN FETCH cur.program LEFT JOIN FETCH s.primaryInstructor WHERE s.id = :id")
    Optional<ClassSection> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT s FROM ClassSection s LEFT JOIN FETCH s.schedules sched LEFT JOIN FETCH sched.room WHERE s.id = :id")
    Optional<ClassSection> findByIdWithSchedules(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ClassSection s 
        SET s.enrolledCount = s.enrolledCount + 1,
            s.status = CASE WHEN (s.enrolledCount + 1) >= s.maxCapacity THEN com.sdt.web_app.entities.scheduling.ClassSection.Status.CLOSED ELSE s.status END
        WHERE s.id = :id 
          AND s.enrolledCount < s.maxCapacity 
          AND s.status = com.sdt.web_app.entities.scheduling.ClassSection.Status.OPEN
    """)
    int incrementEnrolledCountIfOpen(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE ClassSection s
        SET s.enrolledCount = s.enrolledCount - 1,
            s.status = CASE WHEN s.status = com.sdt.web_app.entities.scheduling.ClassSection.Status.CLOSED THEN com.sdt.web_app.entities.scheduling.ClassSection.Status.OPEN ELSE s.status END
        WHERE s.id = :id AND s.enrolledCount > 0
    """)
    int decrementEnrolledCount(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT s.id FROM ClassSection s
        LEFT JOIN s.schedules sched
        WHERE s.primaryInstructor.id = :instructorId OR sched.instructor.id = :instructorId
    """)
    List<Long> findAssignedSectionIdsByInstructor(@Param("instructorId") Long instructorId);

    @Query("""
        SELECT DISTINCT s.id FROM ClassSection s
        LEFT JOIN s.schedules sched
        WHERE s.term.id = :termId
          AND (s.primaryInstructor.id = :instructorId OR sched.instructor.id = :instructorId)
    """)
    List<Long> findAssignedSectionIdsByTermAndInstructor(
            @Param("termId") Long termId,
            @Param("instructorId") Long instructorId);
}
