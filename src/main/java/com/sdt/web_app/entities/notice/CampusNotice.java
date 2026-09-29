package com.sdt.web_app.entities.notice;

import com.sdt.web_app.entities.authentication.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "campus_notices", indexes = {
    @Index(name = "idx_notices_published", columnList = "is_published, publish_at"),
    @Index(name = "idx_notices_audience", columnList = "audience"),
    @Index(name = "idx_notices_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CampusNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "category", nullable = false, length = 60)
    private String category;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "audience", nullable = false, length = 30)
    @Builder.Default
    private String audience = "ALL";

    @Column(name = "priority", nullable = false, length = 20)
    @Builder.Default
    private String priority = "NORMAL";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(name = "author_role", length = 50)
    private String authorRole;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private boolean isPinned = false;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private boolean isPublished = true;

    @Column(name = "publish_at", nullable = false)
    @Builder.Default
    private Instant publishAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (publishAt == null) publishAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
