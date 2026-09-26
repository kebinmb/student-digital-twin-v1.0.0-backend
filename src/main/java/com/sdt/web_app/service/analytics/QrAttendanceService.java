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
import java.util.List;
import java.util.Optional;

import com.sdt.web_app.entities.analytics.FacultyAttendanceRecord;
import com.sdt.web_app.entities.authentication.User;
import com.sdt.web_app.repositories.analytics.FacultyAttendanceRecordRepository;
import com.sdt.web_app.repositories.authentication.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrAttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final FacultyAttendanceRecordRepository facultyAttendanceRecordRepository;
    private final UserRepository userRepository;
    private final ClassScheduleRepository scheduleRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final StudentProfileL2CacheService studentProfileL2CacheService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public static String computeHmacToken(Long sessionId, long window, String secretKey) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                    secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal((sessionId + ":" + window).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception e) {
            return "TOTP-" + window;
        }
    }

    @Transactional
    public AttendanceSessionResponse startSession(StartAttendanceSessionRequest request) {
        return startSession(request, null);
    }

    @Transactional
    public AttendanceSessionResponse startSession(StartAttendanceSessionRequest request, Long creatorUserId) {
        ClassSchedule schedule = scheduleRepository.findById(request.sectionScheduleId())
                .orElseThrow(() -> new EntityNotFoundException("Class schedule not found: " + request.sectionScheduleId()));

        User creatorUser = null;
        if (creatorUserId != null) {
            creatorUser = userRepository.findById(creatorUserId).orElse(null);
        }
        if (creatorUser == null && schedule.getInstructor() != null) {
            creatorUser = schedule.getInstructor();
        }

        String qrSeed = "QR-ATT-" + UUID.randomUUID().toString();
        String secretKey = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        // Dynamically resolve campus coordinates if request coordinates are null
        BigDecimal sessionLat = request.latitude();
        BigDecimal sessionLon = request.longitude();
        if (sessionLat == null || sessionLon == null) {
            BigDecimal defaultLat = new BigDecimal("10.7428");
            BigDecimal defaultLon = new BigDecimal("122.9694");
            if (schedule.getRoom() != null && schedule.getRoom().getCampus() != null) {
                String code = schedule.getRoom().getCampus().getCode();
                if (code != null) {
                    switch (code.toUpperCase()) {
                        case "ALIJIS" -> {
                            defaultLat = new BigDecimal("10.6385");
                            defaultLon = new BigDecimal("122.9723");
                        }
                        case "FORTUNE_TOWNE", "FORTUNETOWNE" -> {
                            defaultLat = new BigDecimal("10.6772");
                            defaultLon = new BigDecimal("122.9856");
                        }
                        case "BINALBAGAN" -> {
                            defaultLat = new BigDecimal("10.1916");
                            defaultLon = new BigDecimal("122.8624");
                        }
                        default -> {
                            defaultLat = new BigDecimal("10.7428");
                            defaultLon = new BigDecimal("122.9694");
                        }
                    }
                }
            }
            if (sessionLat == null) sessionLat = defaultLat;
            if (sessionLon == null) sessionLon = defaultLon;
        }

        AttendanceSession session = AttendanceSession.builder()
                .schedule(schedule)
                .sessionDate(LocalDate.now())
                .qrSeed(qrSeed)
                .secretKey(secretKey)
                .qrExpiresAt(expiresAt)
                .latitude(sessionLat)
                .longitude(sessionLon)
                .allowedRadiusMeters(request.allowedRadiusMeters() != null ? request.allowedRadiusMeters() : 50)
                .creatorUser(creatorUser)
                .build();

        AttendanceSession saved = sessionRepository.save(session);
        log.info("Dynamic QR Attendance Session started for schedule #{}. Seed: {}", schedule.getId(), qrSeed);

        // Record initial separate faculty/creator attendance when host launches session
        if (creatorUser != null) {
            recordInitialFacultyAttendance(saved, creatorUser, sessionLat, sessionLon);
        }

        String qrDataUrl;
        try {
            com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(qrSeed, com.google.zxing.BarcodeFormat.QR_CODE, 256, 256);
            java.io.ByteArrayOutputStream pngOutputStream = new java.io.ByteArrayOutputStream();
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            qrDataUrl = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());
        } catch (Exception e) {
            log.error("Failed to generate ZXing QR barcode, falling back to SVG", e);
            qrDataUrl = "data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='200' height='200'><rect width='100%' height='100%' fill='%23116834'/><text x='50%' y='50%' fill='white' font-size='14' text-anchor='middle' dominant-baseline='middle'>" + qrSeed.substring(0, 15) + "</text></svg>";
        }

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

    private void recordInitialFacultyAttendance(AttendanceSession session, User creatorUser, BigDecimal lat, BigDecimal lon) {
        try {
            FacultyAttendanceRecord existing = facultyAttendanceRecordRepository
                    .findBySessionIdAndFacultyUserId(session.getId(), creatorUser.getId())
                    .orElse(null);
            if (existing == null) {
                FacultyAttendanceRecord record = FacultyAttendanceRecord.builder()
                        .session(session)
                        .facultyUser(creatorUser)
                        .facultyProfile(creatorUser.getFacultyProfile())
                        .verifiedAt(Instant.now())
                        .status(FacultyAttendanceRecord.Status.PRESENT)
                        .deviceFingerprint("Session-Host-Console")
                        .verifiedLatitude(lat)
                        .verifiedLongitude(lon)
                        .isGeofenceValid(true)
                        .notes("Initiated attendance session")
                        .build();
                facultyAttendanceRecordRepository.save(record);
            }
        } catch (Exception e) {
            log.warn("Could not record initial faculty attendance for user #{}: {}", creatorUser.getId(), e.getMessage());
        }
    }

    public AttendanceSession resolveSessionBySeed(String rawSeed) {
        if (rawSeed != null && rawSeed.contains(":TOTP:")) {
            String[] parts = rawSeed.split(":TOTP:");
            String baseSeed = parts[0];
            String token = parts.length > 1 ? parts[1] : "";
            AttendanceSession session = sessionRepository.findByQrSeed(baseSeed)
                    .orElseThrow(() -> new EntityNotFoundException("Invalid or expired QR attendance seed."));

            if (session.getSecretKey() != null) {
                long currentWindow = Instant.now().getEpochSecond() / 30;
                boolean tokenMatch = false;
                for (long window : List.of(currentWindow, currentWindow - 1)) {
                    String expected = computeHmacToken(session.getId(), window, session.getSecretKey());
                    if (expected.equals(token)) {
                        tokenMatch = true;
                        break;
                    }
                }
                if (!tokenMatch) {
                    throw new SecurityException("Expired or invalid rotating QR security token. Please scan the current live QR board.");
                }
            }
            return session;
        } else {
            return sessionRepository.findByQrSeed(rawSeed)
                    .orElseThrow(() -> new EntityNotFoundException("Invalid or expired QR attendance seed."));
        }
    }

    @Transactional
    public AttendanceRecordResponse scanAttendance(ScanAttendanceRequest request) {
        String rawSeed = request.qrSeed();
        AttendanceSession session = resolveSessionBySeed(rawSeed);

        if (session.isExpired()) {
            throw new IllegalStateException("Attendance QR code has expired. Please request instructor to generate a fresh QR session.");
        }

        StudentProfile student = studentProfileL2CacheService.findById(request.studentId());
        if (student == null) {
            // Check if the provided studentId was actually a userId
            student = studentProfileL2CacheService.findByUserId(request.studentId());
        }
        if (student == null) {
            student = studentProfileRepository.findByStudentNumber(String.valueOf(request.studentId())).orElse(null);
        }
        if (student == null) {
            throw new EntityNotFoundException("Student profile not found: " + request.studentId());
        }

        // Anti-Proxy Guard: Check if physical device has already submitted attendance for another student
        String deviceFp = request.deviceFingerprint() != null && !request.deviceFingerprint().isBlank()
                ? request.deviceFingerprint()
                : "Device-Mobile-App";

        if (!"Device-Mobile-App".equalsIgnoreCase(deviceFp)) {
            boolean deviceReused = recordRepository.existsBySessionIdAndDeviceFingerprintAndStudentIdNot(
                    session.getId(), deviceFp, student.getId());
            if (deviceReused) {
                log.warn("Proxy scan rejected: Device {} already submitted attendance for another student in session #{}", deviceFp, session.getId());
                throw new IllegalStateException("Anti-Proxy Guard: This mobile device has already submitted attendance for another student in this session.");
            }
        }

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

        final StudentProfile finalStudent = student;
        AttendanceRecord record = recordRepository.findBySessionIdAndStudentId(session.getId(), finalStudent.getId())
                .orElse(null);

        if (record != null) {
            // Update existing record rather than creating a duplicate
            record.setScannedAt(Instant.now());
            record.setVerifiedLatitude(request.latitude());
            record.setVerifiedLongitude(request.longitude());
            record.setDeviceFingerprint(deviceFp);
            record.setStatus(AttendanceRecord.Status.PRESENT);
        } else {
            record = AttendanceRecord.builder()
                    .session(session)
                    .student(finalStudent)
                    .status(AttendanceRecord.Status.PRESENT)
                    .deviceFingerprint(deviceFp)
                    .verifiedLatitude(request.latitude())
                    .verifiedLongitude(request.longitude())
                    .build();
        }

        AttendanceRecord saved = recordRepository.save(record);
        log.info("Attendance scanned and verified for student {} in session #{}", student.getStudentNumber(), session.getId());

        // Publish telemetry event for event-driven asynchronous risk computation
        if (eventPublisher != null) {
            try {
                eventPublisher.publishEvent(new com.sdt.web_app.events.analytics.AttendanceScannedEvent(student.getId(), session.getId(), saved.getId()));
            } catch (Exception e) {
                log.warn("Failed to publish AttendanceScannedEvent: {}", e.getMessage());
            }
        }

        String sectionCode = session.getSchedule() != null && session.getSchedule().getSection() != null
                ? session.getSchedule().getSection().getSectionCode() : "N/A";
        String courseCode = session.getSchedule() != null && session.getSchedule().getSection() != null && session.getSchedule().getSection().getCourse() != null
                ? session.getSchedule().getSection().getCourse().getCode() : "N/A";

        String studentName = student.getUser() != null ? student.getUser().getUsername() : "Student #" + student.getStudentNumber();
        AttendanceRecordResponse response = new AttendanceRecordResponse(
                saved.getId(),
                session.getId(),
                sectionCode,
                courseCode,
                student.getId(),
                student.getStudentNumber(),
                studentName,
                saved.getStatus().name(),
                isGeofenceValid,
                saved.getScannedAt(),
                saved.getDeviceFingerprint()
        );

        notifySessionEmitters(session.getId(), response);
        return response;
    }

    private final java.util.Map<Long, java.util.List<org.springframework.web.servlet.mvc.method.annotation.SseEmitter>> sessionEmitters = new java.util.concurrent.ConcurrentHashMap<>();

    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter subscribeToSessionStream(Long sessionId) {
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter = new org.springframework.web.servlet.mvc.method.annotation.SseEmitter(30 * 60 * 1000L);
        sessionEmitters.computeIfAbsent(sessionId, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(sessionId, emitter));
        emitter.onTimeout(() -> removeEmitter(sessionId, emitter));
        emitter.onError(e -> removeEmitter(sessionId, emitter));

        return emitter;
    }

    private void removeEmitter(Long sessionId, org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter) {
        java.util.List<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> list = sessionEmitters.get(sessionId);
        if (list != null) {
            list.remove(emitter);
        }
    }

    private void notifySessionEmitters(Long sessionId, AttendanceRecordResponse response) {
        java.util.List<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> list = sessionEmitters.get(sessionId);
        if (list != null && !list.isEmpty()) {
            java.util.List<org.springframework.web.servlet.mvc.method.annotation.SseEmitter> deadEmitters = new java.util.ArrayList<>();
            for (org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter : list) {
                try {
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event()
                            .name("attendance-scan")
                            .data(response));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }
            list.removeAll(deadEmitters);
        }
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
            String secCode = saved.getSession() != null && saved.getSession().getSchedule() != null && saved.getSession().getSchedule().getSection() != null
                    ? saved.getSession().getSchedule().getSection().getSectionCode() : "N/A";
            String crsCode = saved.getSession() != null && saved.getSession().getSchedule() != null && saved.getSession().getSchedule().getSection() != null && saved.getSession().getSchedule().getSection().getCourse() != null
                    ? saved.getSession().getSchedule().getSection().getCourse().getCode() : "N/A";

            return new AttendanceRecordResponse(
                    saved.getId(),
                    saved.getSession() != null ? saved.getSession().getId() : null,
                    secCode,
                    crsCode,
                    student != null ? student.getId() : null,
                    student != null ? student.getStudentNumber() : "N/A",
                    studentName,
                    saved.getStatus() != null ? saved.getStatus().name() : "PRESENT",
                    true,
                    saved.getScannedAt(),
                    saved.getDeviceFingerprint()
            );
        });
        return SliceResponse.from(responseSlice);
    }

    @Transactional(readOnly = true)
    public java.util.List<AttendanceRecordResponse> getDailyAttendance(LocalDate date, Long sectionId) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        java.util.List<AttendanceRecord> records = recordRepository.findDailyAttendanceRecords(queryDate, sectionId);
        return records.stream().map(saved -> {
            AttendanceSession session = saved.getSession();
            ClassSchedule schedule = session != null ? session.getSchedule() : null;
            com.sdt.web_app.entities.scheduling.ClassSection section = schedule != null ? schedule.getSection() : null;
            StudentProfile student = saved.getStudent();

            String secCode = section != null ? section.getSectionCode() : "N/A";
            String crsCode = (section != null && section.getCourse() != null) ? section.getCourse().getCode() : "N/A";
            String studentName = (student != null && student.getUser() != null) ? student.getUser().getUsername() : "Student #" + (student != null ? student.getStudentNumber() : saved.getId());

            return new AttendanceRecordResponse(
                    saved.getId(),
                    session != null ? session.getId() : null,
                    secCode,
                    crsCode,
                    student != null ? student.getId() : null,
                    student != null ? student.getStudentNumber() : "N/A",
                    studentName,
                    saved.getStatus() != null ? saved.getStatus().name() : "PRESENT",
                    true,
                    saved.getScannedAt(),
                    saved.getDeviceFingerprint()
            );
        }).toList();
    }

    @Transactional
    public FacultyAttendanceRecordResponse verifyCreatorAttendance(VerifyCreatorAttendanceRequest request, Long creatorUserId) {
        String rawSeed = request.qrSeed();
        AttendanceSession session = resolveSessionBySeed(rawSeed);

        if (session.isExpired()) {
            throw new IllegalStateException("Attendance QR code has expired. Please generate a fresh QR session.");
        }

        User creatorUser = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new EntityNotFoundException("Faculty/Conductor user not found: " + creatorUserId));

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
                        "Geofence Violation: Conductor device location (%.2f meters away) exceeds allowed %d-meter classroom radius.",
                        distanceMeters, session.getAllowedRadiusMeters()));
            }
        }

        String deviceFp = request.deviceFingerprint() != null && !request.deviceFingerprint().isBlank()
                ? request.deviceFingerprint()
                : "Host-Console";

        // Idempotent: Update existing faculty attendance record or create new if not yet present
        FacultyAttendanceRecord record = facultyAttendanceRecordRepository
                .findBySessionIdAndFacultyUserId(session.getId(), creatorUser.getId())
                .orElseGet(() -> FacultyAttendanceRecord.builder()
                        .session(session)
                        .facultyUser(creatorUser)
                        .facultyProfile(creatorUser.getFacultyProfile())
                        .build());

        record.setVerifiedAt(Instant.now());
        record.setVerifiedLatitude(request.latitude());
        record.setVerifiedLongitude(request.longitude());
        record.setDeviceFingerprint(deviceFp);
        record.setIsGeofenceValid(isGeofenceValid);
        record.setStatus(FacultyAttendanceRecord.Status.PRESENT);
        if (record.getFacultyProfile() == null && creatorUser.getFacultyProfile() != null) {
            record.setFacultyProfile(creatorUser.getFacultyProfile());
        }

        FacultyAttendanceRecord saved = facultyAttendanceRecordRepository.save(record);
        log.info("Faculty/Host geofenced attendance verified for user {} in session #{}", creatorUser.getUsername(), session.getId());

        String sectionCode = session.getSchedule() != null && session.getSchedule().getSection() != null
                ? session.getSchedule().getSection().getSectionCode() : "N/A";
        String courseCode = session.getSchedule() != null && session.getSchedule().getSection() != null && session.getSchedule().getSection().getCourse() != null
                ? session.getSchedule().getSection().getCourse().getCode() : "N/A";
        String roleName = creatorUser.getRoles() != null && !creatorUser.getRoles().isEmpty()
                ? creatorUser.getRoles().iterator().next().name() : "FACULTY";

        return new FacultyAttendanceRecordResponse(
                saved.getId(),
                session.getId(),
                sectionCode,
                courseCode,
                creatorUser.getId(),
                creatorUser.getUsername(),
                roleName,
                saved.getStatus().name(),
                Boolean.TRUE.equals(saved.getIsGeofenceValid()),
                saved.getVerifiedAt(),
                saved.getDeviceFingerprint()
        );
    }

    @Transactional(readOnly = true)
    public List<FacultyAttendanceRecordResponse> getDailyFacultyAttendance(LocalDate date, Long sectionId) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        List<FacultyAttendanceRecord> records = facultyAttendanceRecordRepository.findDailyFacultyAttendanceRecords(queryDate, sectionId);
        return records.stream().map(saved -> {
            AttendanceSession session = saved.getSession();
            ClassSchedule schedule = session != null ? session.getSchedule() : null;
            com.sdt.web_app.entities.scheduling.ClassSection section = schedule != null ? schedule.getSection() : null;
            User user = saved.getFacultyUser();

            String secCode = section != null ? section.getSectionCode() : "N/A";
            String crsCode = (section != null && section.getCourse() != null) ? section.getCourse().getCode() : "N/A";
            String userName = user != null ? user.getUsername() : "Faculty #" + saved.getId();
            String roleName = user != null && user.getRoles() != null && !user.getRoles().isEmpty()
                    ? user.getRoles().iterator().next().name() : "FACULTY";

            return new FacultyAttendanceRecordResponse(
                    saved.getId(),
                    session != null ? session.getId() : null,
                    secCode,
                    crsCode,
                    user != null ? user.getId() : null,
                    userName,
                    roleName,
                    saved.getStatus() != null ? saved.getStatus().name() : "PRESENT",
                    Boolean.TRUE.equals(saved.getIsGeofenceValid()),
                    saved.getVerifiedAt(),
                    saved.getDeviceFingerprint()
            );
        }).toList();
    }
}
