package com.sdt.web_app.entities.analytics;

import com.sdt.web_app.entities.scheduling.ClassSchedule;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "attendance_sessions", indexes = {
    @Index(name = "idx_att_sess_date_sched", columnList = "session_date, section_schedule_id"),
    @Index(name = "idx_att_sess_seed", columnList = "qr_seed")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_schedule_id", nullable = false)
    private ClassSchedule schedule;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "qr_seed", nullable = false, length = 100)
    private String qrSeed;

    @Column(name = "qr_expires_at", nullable = false)
    private Instant qrExpiresAt;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "allowed_radius_meters")
    @Builder.Default
    private Integer allowedRadiusMeters = 50;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    public boolean isExpired() {
        return Instant.now().isAfter(qrExpiresAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AttendanceSession that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
