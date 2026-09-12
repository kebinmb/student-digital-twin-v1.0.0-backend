package com.sdt.web_app.controller.audit;

import com.sdt.web_app.entities.audit.AuditLog;
import com.sdt.web_app.service.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/audit/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getLogsForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(auditLogService.getLogsForUser(userId));
    }
}
