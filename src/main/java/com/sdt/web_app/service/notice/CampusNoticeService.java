package com.sdt.web_app.service.notice;

import com.sdt.web_app.dto.notice.NoticeDtos.*;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.notice.CampusNotice;
import com.sdt.web_app.entities.notice.NoticeAcknowledgment;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.repositories.notice.CampusNoticeRepository;
import com.sdt.web_app.repositories.notice.NoticeAcknowledgmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampusNoticeService {

    private final CampusNoticeRepository noticeRepository;
    private final NoticeAcknowledgmentRepository acknowledgmentRepository;
    private final UserRepository userRepository;
    private final com.sdt.web_app.config.WebSocketBroadcastService broadcastService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.of("Asia/Manila"));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.of("Asia/Manila"));

    @Transactional(readOnly = true)
    public List<NoticeDto> getActiveNotices(Long currentUserId) {
        Instant now = Instant.now();
        List<CampusNotice> notices = noticeRepository.findActiveNotices(now);

        Set<Long> acknowledgedNoticeIds = Collections.emptySet();
        if (currentUserId != null) {
            acknowledgedNoticeIds = acknowledgmentRepository.findByUserId(currentUserId).stream()
                    .map(ack -> ack.getNotice().getId())
                    .collect(Collectors.toSet());
        }

        final Set<Long> ackSet = acknowledgedNoticeIds;
        return notices.stream()
                .map(notice -> mapToDto(notice, ackSet.contains(notice.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<NoticeDto> getActiveNotices(Long currentUserId, org.springframework.data.domain.Pageable pageable) {
        Instant now = Instant.now();
        org.springframework.data.domain.Page<CampusNotice> noticePage = noticeRepository.findActiveNotices(now, pageable);

        Set<Long> acknowledgedNoticeIds = Collections.emptySet();
        if (currentUserId != null) {
            acknowledgedNoticeIds = acknowledgmentRepository.findByUserId(currentUserId).stream()
                    .map(ack -> ack.getNotice().getId())
                    .collect(Collectors.toSet());
        }

        final Set<Long> ackSet = acknowledgedNoticeIds;
        return noticePage.map(notice -> mapToDto(notice, ackSet.contains(notice.getId())));
    }

    @Transactional
    public NoticeDto createNotice(CreateNoticeRequest request, Long authorUserId, String authorRole) {
        User author = null;
        if (authorUserId != null) {
            author = userRepository.findById(authorUserId).orElse(null);
        }

        CampusNotice notice = CampusNotice.builder()
                .title(request.title())
                .category(request.category())
                .content(request.content())
                .audience(request.audience() != null && !request.audience().isBlank() ? request.audience() : "ALL")
                .priority(request.priority() != null && !request.priority().isBlank() ? request.priority() : "NORMAL")
                .author(author)
                .authorRole(authorRole != null ? authorRole : "ADMIN")
                .isPinned(false)
                .isPublished(true)
                .publishAt(Instant.now())
                .build();

        CampusNotice saved = noticeRepository.save(notice);
        log.info("Created campus notice id={} by author={}", saved.getId(), authorUserId);
        if (broadcastService != null) {
            broadcastService.broadcast(
                    com.sdt.web_app.config.WebSocketTopics.NOTIFICATIONS,
                    new com.sdt.web_app.websocket.dto.NotificationMessage(
                            saved.getId(),
                            saved.getTitle(),
                            saved.getContent(),
                            saved.getPriority(),
                            saved.getAudience(),
                            saved.getPublishAt() != null ? saved.getPublishAt() : Instant.now()
                    )
            );
        }
        return mapToDto(saved, false);
    }

    @Transactional
    public void acknowledgeNotice(Long noticeId, Long userId) {
        if (userId == null || noticeId == null) {
            return;
        }

        if (acknowledgmentRepository.existsByNoticeIdAndUserId(noticeId, userId)) {
            return;
        }

        CampusNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("Notice not found: " + noticeId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        NoticeAcknowledgment ack = NoticeAcknowledgment.builder()
                .notice(notice)
                .user(user)
                .acknowledgedAt(Instant.now())
                .build();

        acknowledgmentRepository.save(ack);
        log.debug("User id={} acknowledged notice id={}", userId, noticeId);
    }

    @Transactional
    public void deleteNotice(Long noticeId, Long userId, boolean isAdmin) {
        CampusNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new EntityNotFoundException("Notice not found: " + noticeId));

        if (!isAdmin) {
            if (notice.getAuthor() == null || !notice.getAuthor().getId().equals(userId)) {
                throw new AccessDeniedException("You do not have permission to delete this notice.");
            }
        }

        noticeRepository.delete(notice);
        log.info("Deleted campus notice id={} by user={}", noticeId, userId);
    }

    private NoticeDto mapToDto(CampusNotice notice, boolean isAcknowledged) {
        String formattedDate = formatRelativeDate(notice.getPublishAt());
        String authorDisplayName = notice.getAuthor() != null
                ? notice.getAuthor().getUsername()
                : (notice.getAuthorRole() != null ? notice.getAuthorRole() : "CHMSU Administration");

        return new NoticeDto(
                String.valueOf(notice.getId()),
                notice.getTitle(),
                notice.getCategory(),
                formattedDate,
                !isAcknowledged,
                notice.getContent(),
                notice.getAudience(),
                notice.getPriority(),
                authorDisplayName,
                notice.isPinned(),
                notice.getPublishAt()
        );
    }

    private String formatRelativeDate(Instant instant) {
        if (instant == null) return "Recent";
        ZoneId zone = ZoneId.of("Asia/Manila");
        LocalDate noticeDate = instant.atZone(zone).toLocalDate();
        LocalDate today = LocalDate.now(zone);

        if (noticeDate.equals(today)) {
            return "Today, " + TIME_FORMATTER.format(instant);
        } else if (noticeDate.equals(today.minusDays(1))) {
            return "Yesterday";
        } else {
            return DATE_FORMATTER.format(instant);
        }
    }
}
