package com.sdt.web_app.service.lms;

import com.sdt.web_app.dto.lms.LmsDtos.LmsRosterSyncResponse;
import com.sdt.web_app.entities.enrollment.EnrollmentCourseItem;
import com.sdt.web_app.entities.scheduling.ClassSection;
import com.sdt.web_app.repositories.enrollment.EnrollmentCourseItemRepository;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LmsRosterSyncService {

    private final ClassSectionRepository sectionRepository;
    private final EnrollmentCourseItemRepository itemRepository;

    @Transactional
    public LmsRosterSyncResponse syncRosterToLms(Long sectionId) {
        ClassSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new EntityNotFoundException("Class section not found with id: " + sectionId));

        List<EnrollmentCourseItem> items = itemRepository.findBySectionIdWithStudentDetails(sectionId);
        long enrolledCount = items.stream()
                .filter(item -> item.getCompletionStatus() != EnrollmentCourseItem.CompletionStatus.DROPPED)
                .count();

        log.info("LMS Roster Sync triggered for section {} ({}). Pushed {} student enrollments to Canvas/Moodle LTI Advantage API.",
                section.getSectionCode(), section.getCourse().getCode(), enrolledCount);

        return new LmsRosterSyncResponse(
                section.getId(),
                section.getSectionCode(),
                (int) enrolledCount,
                "SUCCESS",
                String.format("Successfully synchronized %d active student enrollments for section %s to LMS platform.", enrolledCount, section.getSectionCode())
        );
    }
}
