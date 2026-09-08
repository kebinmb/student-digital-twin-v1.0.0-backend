package com.sdt.web_app.service.faculty;

import com.sdt.web_app.service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("facultySecurity")
@RequiredArgsConstructor
public class FacultySecurity {

    private final SecurityUtils securityUtils;

    public boolean isFacultySelf(Long userId, Authentication authentication) {
        if (userId == null || authentication == null) {
            return false;
        }
        Long currentUserId = securityUtils.resolveUserId(authentication);
        return userId.equals(currentUserId);
    }
}
