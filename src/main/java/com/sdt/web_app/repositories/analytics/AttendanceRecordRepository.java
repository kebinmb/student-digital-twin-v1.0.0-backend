package com.sdt.web_app.repositories.analytics;

import com.sdt.web_app.entities.analytics.AttendanceRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);
    List<AttendanceRecord> findByStudentId(Long studentId);
    Slice<AttendanceRecord> findByStudentId(Long studentId, Pageable pageable);
    
    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.student.id = :studentId AND ar.status = 'PRESENT'")
    long countPresentByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.student.id = :studentId")
    long countTotalByStudentId(@Param("studentId") Long studentId);

    @Query("""
        SELECT DISTINCT ar FROM AttendanceRecord ar
        JOIN FETCH ar.session s
        JOIN FETCH s.schedule sched
        JOIN FETCH sched.section sec
        JOIN FETCH ar.student sp
        LEFT JOIN FETCH sp.user u
        WHERE (:sessionDate IS NULL OR s.sessionDate = :sessionDate)
          AND (:sectionId IS NULL OR sec.id = :sectionId)
        ORDER BY ar.scannedAt DESC
    """)
    List<AttendanceRecord> findDailyAttendanceRecords(
            @Param("sessionDate") java.time.LocalDate sessionDate,
            @Param("sectionId") Long sectionId);
}
