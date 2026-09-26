package com.sdt.web_app.repositories.analytics;

import com.sdt.web_app.entities.analytics.FacultyAttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FacultyAttendanceRecordRepository extends JpaRepository<FacultyAttendanceRecord, Long> {

    Optional<FacultyAttendanceRecord> findBySessionIdAndFacultyUserId(Long sessionId, Long facultyUserId);

    List<FacultyAttendanceRecord> findBySessionId(Long sessionId);

    @Query("""
        SELECT DISTINCT far FROM FacultyAttendanceRecord far
        JOIN FETCH far.session s
        JOIN FETCH s.schedule sched
        JOIN FETCH sched.section sec
        JOIN FETCH far.facultyUser u
        LEFT JOIN FETCH far.facultyProfile fp
        WHERE (:sessionDate IS NULL OR s.sessionDate = :sessionDate)
          AND (:sectionId IS NULL OR sec.id = :sectionId)
        ORDER BY far.verifiedAt DESC
    """)
    List<FacultyAttendanceRecord> findDailyFacultyAttendanceRecords(
            @Param("sessionDate") LocalDate sessionDate,
            @Param("sectionId") Long sectionId);
}
