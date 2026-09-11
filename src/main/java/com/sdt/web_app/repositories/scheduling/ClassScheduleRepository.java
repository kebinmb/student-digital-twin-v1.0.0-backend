package com.sdt.web_app.repositories.scheduling;

import com.sdt.web_app.entities.scheduling.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {

    List<ClassSchedule> findBySectionId(Long sectionId);

    List<ClassSchedule> findBySectionTermId(Long termId);

    List<ClassSchedule> findByRoomIdAndSectionTermId(Long roomId, Long termId);

    List<ClassSchedule> findByInstructorIdAndSectionTermId(Long instructorId, Long termId);

    @Query("""
        SELECT s FROM ClassSchedule s
        JOIN FETCH s.room
        LEFT JOIN FETCH s.instructor
        WHERE s.section.id IN :sectionIds
        ORDER BY s.dayOfWeek ASC, s.startTime ASC
    """)
    List<ClassSchedule> findBySectionIdInWithRoomAndInstructor(@Param("sectionIds") java.util.Collection<Long> sectionIds);

    @Query("""
        SELECT COUNT(s) > 0 FROM ClassSchedule s
        WHERE s.section.term.id = :termId
          AND :roomId IS NOT NULL
          AND s.room IS NOT NULL
          AND s.room.id = :roomId
          AND s.dayOfWeek = :dayOfWeek
          AND s.startTime < :endTime
          AND s.endTime > :startTime
    """)
    boolean existsOverlappingRoomSchedule(
            @Param("termId") Long termId,
            @Param("roomId") Long roomId,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("""
        SELECT COUNT(s) > 0 FROM ClassSchedule s
        WHERE s.section.term.id = :termId
          AND :instructorUserId IS NOT NULL
          AND s.instructor IS NOT NULL
          AND s.instructor.id = :instructorUserId
          AND s.dayOfWeek = :dayOfWeek
          AND s.startTime < :endTime
          AND s.endTime > :startTime
    """)
    boolean existsOverlappingFacultySchedule(
            @Param("termId") Long termId,
            @Param("instructorUserId") Long instructorUserId,
            @Param("dayOfWeek") String dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("""
        SELECT COUNT(DISTINCT s.section.course.id) FROM ClassSchedule s
        WHERE s.instructor.id = :instructorId
          AND s.section.term.id = :termId
    """)
    long countDistinctCoursesByFacultyAndTerm(
            @Param("instructorId") Long instructorId,
            @Param("termId") Long termId);
}
