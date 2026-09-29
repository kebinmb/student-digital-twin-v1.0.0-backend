package com.sdt.web_app.repositories.notice;

import com.sdt.web_app.entities.notice.NoticeAcknowledgment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeAcknowledgmentRepository extends JpaRepository<NoticeAcknowledgment, Long> {

    Optional<NoticeAcknowledgment> findByNoticeIdAndUserId(Long noticeId, Long userId);

    boolean existsByNoticeIdAndUserId(Long noticeId, Long userId);

    List<NoticeAcknowledgment> findByUserId(Long userId);
}
