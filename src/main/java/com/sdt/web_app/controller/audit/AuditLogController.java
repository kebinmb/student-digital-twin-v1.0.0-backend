package com.sdt.web_app.controller.audit;

import com.sdt.web_app.annotation.Auditable;
import com.sdt.web_app.entities.audit.AuditLog;
import com.sdt.web_app.service.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.sdt.web_app.dto.common.SliceResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Auditable(action = "READ_ALL_LOGS", entityName = "AuditLog")
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAllLogs() {
        return ResponseEntity.ok(auditLogService.getAllLogs());
    }

    @Auditable(action = "READ_USER_LOGS", entityName = "AuditLog", entityId = "#userId")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLog>> getLogsForUser(@PathVariable Long userId) {
        return ResponseEntity.ok(auditLogService.getLogsForUser(userId));
    }

    @Auditable(action = "QUERY_LOGS_SLICE", entityName = "AuditLog")
    @GetMapping("/slice")
    public ResponseEntity<SliceResponse<AuditLog>> getLogsBetweenSlice(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        Sort sort = "ASC".equalsIgnoreCase(sortDir) ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), sort);
        return ResponseEntity.ok(auditLogService.getLogsBetweenSlice(start, end, pageable));
    }
}
