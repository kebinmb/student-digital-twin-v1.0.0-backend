package com.sdt.web_app.repositories.notice;

import com.sdt.web_app.entities.notice.CampusNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CampusNoticeRepository extends JpaRepository<CampusNotice, Long> {

    @Query("SELECT n FROM CampusNotice n " +
           "WHERE n.isPublished = true " +
           "AND n.publishAt <= :now " +
           "AND (n.expiresAt IS NULL OR n.expiresAt >= :now) " +
           "ORDER BY n.isPinned DESC, n.publishAt DESC")
    List<CampusNotice> findActiveNotices(@Param("now") Instant now);

    @Query(value = "SELECT n FROM CampusNotice n " +
           "WHERE n.isPublished = true " +
           "AND n.publishAt <= :now " +
           "AND (n.expiresAt IS NULL OR n.expiresAt >= :now) " +
           "ORDER BY n.isPinned DESC, n.publishAt DESC",
           countQuery = "SELECT count(n) FROM CampusNotice n " +
           "WHERE n.isPublished = true " +
           "AND n.publishAt <= :now " +
           "AND (n.expiresAt IS NULL OR n.expiresAt >= :now)")
    org.springframework.data.domain.Page<CampusNotice> findActiveNotices(@Param("now") Instant now, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT n FROM CampusNotice n " +
           "WHERE n.isPublished = true " +
           "AND n.publishAt <= :now " +
           "AND (n.expiresAt IS NULL OR n.expiresAt >= :now) " +
           "AND (n.audience = 'ALL' OR n.audience = :audience) " +
           "ORDER BY n.isPinned DESC, n.publishAt DESC")
    List<CampusNotice> findActiveNoticesForAudience(@Param("now") Instant now, @Param("audience") String audience);
}
