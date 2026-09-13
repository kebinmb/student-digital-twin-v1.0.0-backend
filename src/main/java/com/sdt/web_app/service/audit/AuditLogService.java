package com.sdt.web_app.service.audit;

import com.sdt.web_app.entities.audit.AuditLog;
import com.sdt.web_app.repositories.audit.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sdt.web_app.dto.common.SliceResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Asynchronously persists audit log entry via Spring proxy.
     * Uses virtual threads executor configured via applicationTaskExecutor.
     */
    @Async("applicationTaskExecutor")
    public void saveAuditLogAsync(AuditLog auditLog) {
        try {
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to asynchronously save AuditLog entry for action: {}", auditLog.getAction(), e);
        }
    }

    @Transactional(readOnly = true)
    public java.util.List<AuditLog> getLogsForUser(Long userId) {
        return auditLogRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public java.util.List<AuditLog> getAllLogs() {
        return auditLogRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SliceResponse<AuditLog> getLogsBetweenSlice(Instant start, Instant end, Pageable pageable) {
        Slice<AuditLog> slice = auditLogRepository.findSliceByCreatedAtBetween(start, end, pageable);
        return SliceResponse.from(slice);
    }
}
