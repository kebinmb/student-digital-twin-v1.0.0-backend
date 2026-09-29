package com.sdt.web_app.controller.notice;

import com.sdt.web_app.dto.notice.NoticeDtos.*;
import com.sdt.web_app.service.notice.CampusNoticeService;
import com.sdt.web_app.service.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class CampusNoticeController {

    private final CampusNoticeService noticeService;
    private final SecurityUtils securityUtils;

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NoticeDto>> getActiveNotices(Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(noticeService.getActiveNotices(userId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<org.springframework.data.domain.Page<NoticeDto>> getActiveNoticesPaginated(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "5") int size,
            Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        return ResponseEntity.ok(noticeService.getActiveNotices(userId, org.springframework.data.domain.PageRequest.of(page, size)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'GUIDANCE', 'ACCOUNTANT')")
    public ResponseEntity<NoticeDto> createNotice(@Valid @RequestBody CreateNoticeRequest request, Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        String primaryRole = "ADMIN";
        if (authentication != null && authentication.getAuthorities() != null) {
            primaryRole = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.replace("ROLE_", ""))
                    .findFirst()
                    .orElse("ADMIN");
        }

        NoticeDto created = noticeService.createNotice(request, userId, primaryRole);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> acknowledgeNotice(@PathVariable("id") Long id, Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        noticeService.acknowledgeNotice(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'REGISTRAR', 'DEAN', 'CHAIRPERSON', 'GUIDANCE', 'ACCOUNTANT')")
    public ResponseEntity<Void> deleteNotice(@PathVariable("id") Long id, Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        noticeService.deleteNotice(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }
}
