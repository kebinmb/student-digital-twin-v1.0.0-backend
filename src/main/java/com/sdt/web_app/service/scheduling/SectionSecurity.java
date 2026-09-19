package com.sdt.web_app.service.scheduling;

import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.grade.ClassRecordItemRepository;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import com.sdt.web_app.service.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

import org.springframework.transaction.annotation.Transactional;

@Component("sectionSecurity")
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SectionSecurity {

    private final ClassSectionRepository sectionRepository;
    private final ClassRecordItemRepository itemRepository;
    private final SecurityUtils securityUtils;
    private final com.sdt.web_app.service.security.AcademicScopeAssertionService academicScopeAssertionService;

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

    public boolean isInstructorForItem(Long itemId, Authentication authentication) {
        if (itemId == null || authentication == null) {
            return false;
        }
        return itemRepository.findSectionIdByItemId(itemId)
                .map(secId -> isInstructor(secId, authentication))
                .orElse(false);
    }

    public boolean canAccessSection(Long sectionId, Authentication authentication) {
        if (sectionId == null || authentication == null) {
            return false;
        }
        try {
            com.sdt.web_app.service.security.AcademicScopeContext scope = academicScopeAssertionService.assertAndResolveScope(authentication);
            if (scope.isUnrestricted()) return true;

            Optional<ClassSection> sectionOpt = sectionRepository.findByIdWithSchedules(sectionId);
            if (sectionOpt.isEmpty()) return false;

            academicScopeAssertionService.validateSectionAccess(scope, sectionOpt.get());
            return true;
        } catch (Exception e) {
            log.warn("Access denied for section {}: {}", sectionId, e.getMessage());
            return false;
        }
    }

    public boolean canAccessItem(Long itemId, Authentication authentication) {
        if (itemId == null || authentication == null) {
            return false;
        }
        return itemRepository.findSectionIdByItemId(itemId)
                .map(secId -> canAccessSection(secId, authentication))
                .orElse(false);
    }
}
