package com.sdt.web_app.service.audit;

import com.sdt.web_app.entities.audit.AuditLog;
import com.sdt.web_app.repositories.audit.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
}
