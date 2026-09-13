package com.sdt.web_app.repositories.audit;

import com.sdt.web_app.entities.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Slice;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserId(Long userId);
    List<AuditLog> findByAction(String action);
    Page<AuditLog> findByCreatedAtBetween(Instant start, Instant end, Pageable pageable);
    Slice<AuditLog> findSliceByCreatedAtBetween(Instant start, Instant end, Pageable pageable);
}
