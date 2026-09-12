package com.sdt.web_app.controller.lms;

import com.sdt.web_app.dto.lms.LmsDtos.LmsRosterSyncResponse;
import com.sdt.web_app.service.lms.LmsRosterSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lms")
@RequiredArgsConstructor
public class LmsSyncController {

    private final LmsRosterSyncService lmsRosterSyncService;

    @PostMapping("/sync/roster/{sectionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'FACULTY', 'DEAN')")
    public ResponseEntity<LmsRosterSyncResponse> syncRosterToLms(@PathVariable("sectionId") Long sectionId) {
        return ResponseEntity.ok(lmsRosterSyncService.syncRosterToLms(sectionId));
    }
}
