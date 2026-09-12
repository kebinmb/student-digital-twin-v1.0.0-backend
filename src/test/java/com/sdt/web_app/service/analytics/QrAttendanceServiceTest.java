package com.sdt.web_app.service.analytics;

import com.sdt.web_app.dto.analytics.AnalyticsDtos.*;
import com.sdt.web_app.entities.analytics.AttendanceRecord;
import com.sdt.web_app.entities.analytics.AttendanceSession;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.entities.scheduling.ClassSchedule;
import com.sdt.web_app.repositories.analytics.AttendanceRecordRepository;
import com.sdt.web_app.repositories.analytics.AttendanceSessionRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import com.sdt.web_app.repositories.scheduling.ClassScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QrAttendanceServiceTest {

    @Mock private AttendanceSessionRepository sessionRepository;
    @Mock private AttendanceRecordRepository recordRepository;
    @Mock private ClassScheduleRepository scheduleRepository;
    @Mock private StudentProfileRepository studentProfileRepository;

    @InjectMocks
    private QrAttendanceService attendanceService;

    private ClassSchedule mockSchedule;
    private StudentProfile mockStudent;
    private AttendanceSession mockSession;

    @BeforeEach
    void setUp() {
        mockSchedule = ClassSchedule.builder().id(100L).dayOfWeek("MONDAY").build();
        mockStudent = StudentProfile.builder().id(200L).studentNumber("2024-8888").build();
        mockSession = AttendanceSession.builder()
                .id(1L)
                .schedule(mockSchedule)
                .sessionDate(LocalDate.now())
                .qrSeed("QR-SEED-12345")
                .qrExpiresAt(Instant.now().plus(15, ChronoUnit.MINUTES))
                .latitude(new BigDecimal("10.7202"))
                .longitude(new BigDecimal("122.5621"))
                .allowedRadiusMeters(50)
                .build();
    }

    @Test
    @DisplayName("Haversine formula calculates correct distance between coordinates")
    void haversineDistance_Valid() {
        // Points ~15 meters apart in Iloilo City
        double d = QrAttendanceService.calculateHaversineDistance(10.720200, 122.562100, 10.720210, 122.562110);
        assertThat(d).isLessThan(50.0);
    }

    @Test
    @DisplayName("Scan attendance succeeds when within geofence and not expired")
    void scanAttendance_Success() {
        when(sessionRepository.findByQrSeed("QR-SEED-12345")).thenReturn(Optional.of(mockSession));
        when(studentProfileRepository.findById(200L)).thenReturn(Optional.of(mockStudent));
        when(recordRepository.findBySessionIdAndStudentId(1L, 200L)).thenReturn(Optional.empty());

        AttendanceRecord record = AttendanceRecord.builder()
                .id(50L)
                .session(mockSession)
                .student(mockStudent)
                .status(AttendanceRecord.Status.PRESENT)
                .build();

        when(recordRepository.save(any(AttendanceRecord.class))).thenReturn(record);

        ScanAttendanceRequest req = new ScanAttendanceRequest(
                "QR-SEED-12345", 200L, new BigDecimal("10.7202"), new BigDecimal("122.5621"), "Device-Mobile-1"
        );

        AttendanceRecordResponse res = attendanceService.scanAttendance(req);

        assertThat(res).isNotNull();
        assertThat(res.attendanceStatus()).isEqualTo("PRESENT");
        assertThat(res.isGeofenceValid()).isTrue();
    }

    @Test
    @DisplayName("Scan attendance fails when geofence radius exceeded (> 50 meters)")
    void scanAttendance_GeofenceViolation() {
        when(sessionRepository.findByQrSeed("QR-SEED-12345")).thenReturn(Optional.of(mockSession));
        when(studentProfileRepository.findById(200L)).thenReturn(Optional.of(mockStudent));

        // Location 5km away
        ScanAttendanceRequest req = new ScanAttendanceRequest(
                "QR-SEED-12345", 200L, new BigDecimal("10.7600"), new BigDecimal("122.6000"), "Device-Mobile-1"
        );

        assertThatThrownBy(() -> attendanceService.scanAttendance(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Geofence Violation");
    }
}
