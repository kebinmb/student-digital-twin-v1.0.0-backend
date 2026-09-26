package com.sdt.web_app.entities.analytics;

import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.entities.faculty.FacultyProfile;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "faculty_attendance_records", indexes = {
    @Index(name = "idx_facatt_session", columnList = "attendance_session_id"),
    @Index(name = "idx_facatt_user", columnList = "faculty_user_id"),
    @Index(name = "idx_facatt_verified_at", columnList = "verified_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_facatt_session_user", columnNames = {"attendance_session_id", "faculty_user_id"})
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class FacultyAttendanceRecord {

    public enum Status {
        PRESENT, LATE, EXCUSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_session_id", nullable = false)
    private AttendanceSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_user_id", nullable = false)
    private User facultyUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_profile_id")
    private FacultyProfile facultyProfile;

    @Column(name = "verified_at", nullable = false)
    @Builder.Default
    private Instant verifiedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PRESENT;

    @Column(name = "device_fingerprint")
    private String deviceFingerprint;

    @Column(name = "verified_latitude", precision = 10, scale = 8)
    private BigDecimal verifiedLatitude;

    @Column(name = "verified_longitude", precision = 11, scale = 8)
    private BigDecimal verifiedLongitude;

    @Column(name = "is_geofence_valid", nullable = false)
    @Builder.Default
    private Boolean isGeofenceValid = true;

    @Column(name = "notes")
    private String notes;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FacultyAttendanceRecord that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
