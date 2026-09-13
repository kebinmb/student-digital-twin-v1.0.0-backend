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
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sdt.web_app.dto.common.SliceResponse;
import com.sdt.web_app.utils.SortPropertyMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import com.sdt.web_app.service.security.StudentProfileL2CacheService;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrAttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final ClassScheduleRepository scheduleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentProfileL2CacheService studentProfileL2CacheService;

    @Transactional
    public AttendanceSessionResponse startSession(StartAttendanceSessionRequest request) {
        ClassSchedule schedule = scheduleRepository.findById(request.sectionScheduleId())
                .orElseThrow(() -> new EntityNotFoundException("Class schedule not found: " + request.sectionScheduleId()));

        String qrSeed = "QR-ATT-" + UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        AttendanceSession session = AttendanceSession.builder()
                .schedule(schedule)
                .sessionDate(LocalDate.now())
                .qrSeed(qrSeed)
                .qrExpiresAt(expiresAt)
                .latitude(request.latitude() != null ? request.latitude() : new BigDecimal("10.7202"))
                .longitude(request.longitude() != null ? request.longitude() : new BigDecimal("122.5621"))
                .allowedRadiusMeters(request.allowedRadiusMeters() != null ? request.allowedRadiusMeters() : 50)
                .build();

        AttendanceSession saved = sessionRepository.save(session);
        log.info("Dynamic QR Attendance Session started for schedule #{}. Seed: {}", schedule.getId(), qrSeed);

        String qrDataUrl = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200'><rect width='100%' height='100%' fill='%23116834'/><text x='50%' y='50%' fill='white' font-size='14' text-anchor='middle' dominant-baseline='middle'>" + qrSeed.substring(0, 15) + "</text></svg>";

        return new AttendanceSessionResponse(
                saved.getId(),
                schedule.getId(),
                saved.getQrSeed(),
                saved.getQrExpiresAt(),
                saved.getLatitude(),
                saved.getLongitude(),
                saved.getAllowedRadiusMeters(),
                qrDataUrl
        );
    }

    @Transactional
    public AttendanceRecordResponse scanAttendance(ScanAttendanceRequest request) {
        AttendanceSession session = sessionRepository.findByQrSeed(request.qrSeed())
                .orElseThrow(() -> new EntityNotFoundException("Invalid or expired QR attendance seed."));

        if (session.isExpired()) {
            throw new IllegalStateException("Attendance QR code has expired. Please request instructor to generate a fresh QR session.");
        }

        StudentProfile student = Optional.ofNullable(studentProfileL2CacheService.findById(request.studentId()))
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found: " + request.studentId()));

        // Geofence GPS Distance Verification using Haversine formula
        boolean isGeofenceValid = true;
        if (session.getLatitude() != null && session.getLongitude() != null && request.latitude() != null && request.longitude() != null) {
            double distanceMeters = calculateHaversineDistance(
                    session.getLatitude().doubleValue(), session.getLongitude().doubleValue(),
                    request.latitude().doubleValue(), request.longitude().doubleValue()
            );
            if (distanceMeters > session.getAllowedRadiusMeters()) {
                isGeofenceValid = false;
                throw new IllegalStateException(String.format(
                        "Geofence Violation: Device location (%.2f meters away) exceeds allowed %d-meter classroom radius.",
                        distanceMeters, session.getAllowedRadiusMeters()));
            }
        }

        AttendanceRecord record = recordRepository.findBySessionIdAndStudentId(session.getId(), student.getId())
                .orElseGet(() -> AttendanceRecord.builder()
                        .session(session)
                        .student(student)
                        .status(AttendanceRecord.Status.PRESENT)
                        .deviceFingerprint(request.deviceFingerprint() != null ? request.deviceFingerprint() : "Device-Mobile-App")
                        .verifiedLatitude(request.latitude())
                        .verifiedLongitude(request.longitude())
                        .build());

        AttendanceRecord saved = recordRepository.save(record);
        log.info("Attendance scanned and verified for student {} in session #{}", student.getStudentNumber(), session.getId());

        String studentName = student.getUser() != null ? student.getUser().getUsername() : "Student #" + student.getStudentNumber();
        return new AttendanceRecordResponse(
                saved.getId(),
                session.getId(),
                student.getId(),
                student.getStudentNumber(),
                studentName,
                saved.getStatus().name(),
                isGeofenceValid,
                saved.getScannedAt()
        );
    }

    public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_METERS = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    @Transactional(readOnly = true)
    public SliceResponse<AttendanceRecordResponse> getStudentAttendanceSlice(Long studentId, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = SortPropertyMapper.createAttendanceRecordPageable(page, size, sortBy, sortDir);
        Slice<AttendanceRecord> slice = recordRepository.findByStudentId(studentId, pageable);
        Slice<AttendanceRecordResponse> responseSlice = slice.map(saved -> {
            StudentProfile student = saved.getStudent();
            String studentName = student != null && student.getUser() != null ? student.getUser().getUsername() : "Student #" + (student != null ? student.getStudentNumber() : saved.getId());
            return new AttendanceRecordResponse(
                    saved.getId(),
                    saved.getSession() != null ? saved.getSession().getId() : null,
                    student != null ? student.getId() : null,
                    student != null ? student.getStudentNumber() : "N/A",
                    studentName,
                    saved.getStatus() != null ? saved.getStatus().name() : "PRESENT",
                    true,
                    saved.getScannedAt()
            );
        });
        return SliceResponse.from(responseSlice);
    }
}
