package com.sdt.web_app.repositories.analytics;

import com.sdt.web_app.entities.analytics.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    Optional<AttendanceSession> findByQrSeed(String qrSeed);
}
