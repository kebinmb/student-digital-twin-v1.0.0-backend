package com.sdt.web_app.service.scheduling;

import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import com.sdt.web_app.service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("sectionSecurity")
@RequiredArgsConstructor
@Slf4j
public class SectionSecurity {

    private final ClassSectionRepository sectionRepository;
    private final SecurityUtils securityUtils;

    public boolean isInstructor(Long sectionId, Authentication authentication) {
        if (sectionId == null || authentication == null) {
            return false;
        }

        Long userId = securityUtils.resolveUserId(authentication);
        if (userId == null) {
            return false;
        }

        Optional<ClassSection> sectionOpt = sectionRepository.findByIdWithSchedules(sectionId);
        if (sectionOpt.isEmpty()) {
            return false;
        }

        ClassSection section = sectionOpt.get();

        // 1. Check primary instructor
        if (section.getPrimaryInstructor() != null && userId.equals(section.getPrimaryInstructor().getId())) {
            return true;
        }

        // 2. Check scheduled instructors across all timetable slots
        return section.getSchedules().stream()
                .anyMatch(s -> s.getInstructor() != null && userId.equals(s.getInstructor().getId()));
    }
}
