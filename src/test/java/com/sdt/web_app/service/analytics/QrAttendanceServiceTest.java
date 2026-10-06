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

import com.sdt.web_app.entities.analytics.FacultyAttendanceRecord;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.repositories.analytics.FacultyAttendanceRecordRepository;
import com.sdt.web_app.repositories.authentication.UserRepository;
import com.sdt.web_app.service.security.StudentProfileL2CacheService;

@ExtendWith(MockitoExtension.class)
class QrAttendanceServiceTest {

    @Mock private AttendanceSessionRepository sessionRepository;
    @Mock private AttendanceRecordRepository recordRepository;
    @Mock private FacultyAttendanceRecordRepository facultyAttendanceRecordRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClassScheduleRepository scheduleRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private StudentProfileL2CacheService studentProfileL2CacheService;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock private com.sdt.web_app.service.lms.StudentNotificationPublisherService studentNotificationPublisherService;

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
                .secretKey("secretkey123456789012345678901234")
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
        when(studentProfileL2CacheService.findById(200L)).thenReturn(mockStudent);
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
        when(studentProfileL2CacheService.findById(200L)).thenReturn(mockStudent);

        // Location 5km away
        ScanAttendanceRequest req = new ScanAttendanceRequest(
                "QR-SEED-12345", 200L, new BigDecimal("10.7600"), new BigDecimal("122.6000"), "Device-Mobile-1"
        );

        assertThatThrownBy(() -> attendanceService.scanAttendance(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Geofence Violation");
    }

    @Test
    @DisplayName("Scan attendance rejects proxy scan when physical device was already used by another student")
    void scanAttendance_ProxyRejected() {
        when(sessionRepository.findByQrSeed("QR-SEED-12345")).thenReturn(Optional.of(mockSession));
        when(studentProfileL2CacheService.findById(200L)).thenReturn(mockStudent);
        when(recordRepository.existsBySessionIdAndDeviceFingerprintAndStudentIdNot(1L, "Device-Cheating-123", 200L))
                .thenReturn(true);

        ScanAttendanceRequest req = new ScanAttendanceRequest(
                "QR-SEED-12345", 200L, new BigDecimal("10.7202"), new BigDecimal("122.5621"), "Device-Cheating-123"
        );

        assertThatThrownBy(() -> attendanceService.scanAttendance(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Anti-Proxy Guard");
    }

    @Test
    @DisplayName("Scan attendance is idempotent: updates existing student record instead of creating duplicates")
    void scanAttendance_IdempotentUpdate() {
        when(sessionRepository.findByQrSeed("QR-SEED-12345")).thenReturn(Optional.of(mockSession));
        when(studentProfileL2CacheService.findById(200L)).thenReturn(mockStudent);

        AttendanceRecord existingRecord = AttendanceRecord.builder()
                .id(77L)
                .session(mockSession)
                .student(mockStudent)
                .status(AttendanceRecord.Status.PRESENT)
                .deviceFingerprint("Old-Device")
                .verifiedLatitude(new BigDecimal("10.7200"))
                .verifiedLongitude(new BigDecimal("122.5620"))
                .build();

        when(recordRepository.findBySessionIdAndStudentId(1L, 200L)).thenReturn(Optional.of(existingRecord));
        when(recordRepository.save(existingRecord)).thenReturn(existingRecord);

        ScanAttendanceRequest req = new ScanAttendanceRequest(
                "QR-SEED-12345", 200L, new BigDecimal("10.7202"), new BigDecimal("122.5621"), "New-Device-Mobile"
        );

        AttendanceRecordResponse res = attendanceService.scanAttendance(req);

        assertThat(res).isNotNull();
        assertThat(res.recordId()).isEqualTo(77L);
        assertThat(existingRecord.getDeviceFingerprint()).isEqualTo("New-Device-Mobile");
        verify(recordRepository, times(1)).save(existingRecord);
    }

    @Test
    @DisplayName("Start session with creator user stores creator on session and initializes faculty attendance")
    void startSession_WithCreator_RecordsFacultyAttendance() {
        User creator = User.builder().id(10L).username("prof_smith").email("smith@chmsu.edu.ph").build();
        when(scheduleRepository.findById(100L)).thenReturn(Optional.of(mockSchedule));
        when(userRepository.findById(10L)).thenReturn(Optional.of(creator));
        when(sessionRepository.save(any(AttendanceSession.class))).thenAnswer(invocation -> {
            AttendanceSession s = invocation.getArgument(0);
            return AttendanceSession.builder()
                    .id(55L)
                    .schedule(s.getSchedule())
                    .sessionDate(s.getSessionDate())
                    .qrSeed(s.getQrSeed())
                    .secretKey(s.getSecretKey())
                    .qrExpiresAt(s.getQrExpiresAt())
                    .latitude(s.getLatitude())
                    .longitude(s.getLongitude())
                    .allowedRadiusMeters(s.getAllowedRadiusMeters())
                    .creatorUser(s.getCreatorUser())
                    .build();
        });

        StartAttendanceSessionRequest req = new StartAttendanceSessionRequest(100L, new BigDecimal("10.7202"), new BigDecimal("122.5621"), 50);
        AttendanceSessionResponse res = attendanceService.startSession(req, 10L);

        assertThat(res).isNotNull();
        assertThat(res.sessionId()).isEqualTo(55L);
        verify(facultyAttendanceRecordRepository, times(1)).save(any(FacultyAttendanceRecord.class));
    }

    @Test
    @DisplayName("Verify creator attendance records faculty presence separately and does not touch student records")
    void verifyCreatorAttendance_Success_Idempotent() {
        User creator = User.builder().id(10L).username("prof_smith").email("smith@chmsu.edu.ph").build();
        when(sessionRepository.findByQrSeed("QR-SEED-12345")).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(10L)).thenReturn(Optional.of(creator));
        when(facultyAttendanceRecordRepository.findBySessionIdAndFacultyUserId(1L, 10L)).thenReturn(Optional.empty());

        FacultyAttendanceRecord savedFacultyRecord = FacultyAttendanceRecord.builder()
                .id(99L)
                .session(mockSession)
                .facultyUser(creator)
                .status(FacultyAttendanceRecord.Status.PRESENT)
                .isGeofenceValid(true)
                .verifiedAt(Instant.now())
                .deviceFingerprint("Faculty-Laptop")
                .build();
        when(facultyAttendanceRecordRepository.save(any(FacultyAttendanceRecord.class))).thenReturn(savedFacultyRecord);

        VerifyCreatorAttendanceRequest req = new VerifyCreatorAttendanceRequest(
                "QR-SEED-12345", new BigDecimal("10.7202"), new BigDecimal("122.5621"), "Faculty-Laptop"
        );

        FacultyAttendanceRecordResponse res = attendanceService.verifyCreatorAttendance(req, 10L);

        assertThat(res).isNotNull();
        assertThat(res.recordId()).isEqualTo(99L);
        assertThat(res.facultyUserId()).isEqualTo(10L);
        assertThat(res.facultyName()).isEqualTo("prof_smith");
        assertThat(res.isGeofenceValid()).isTrue();

        // Must NOT interact with student attendance_records
        verifyNoInteractions(recordRepository);
    }

    @Test
    @DisplayName("Verify creator attendance throws exception on geofence violation")
    void verifyCreatorAttendance_GeofenceViolation() {
        User creator = User.builder().id(10L).username("prof_smith").email("smith@chmsu.edu.ph").build();
        when(sessionRepository.findByQrSeed("QR-SEED-12345")).thenReturn(Optional.of(mockSession));
        when(userRepository.findById(10L)).thenReturn(Optional.of(creator));

        // Location 5km away
        VerifyCreatorAttendanceRequest req = new VerifyCreatorAttendanceRequest(
                "QR-SEED-12345", new BigDecimal("10.7600"), new BigDecimal("122.6000"), "Faculty-Laptop"
        );

        assertThatThrownBy(() -> attendanceService.verifyCreatorAttendance(req, 10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Geofence Violation");
    }
}
