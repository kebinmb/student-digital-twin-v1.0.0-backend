package com.sdt.web_app.config;

/**
 * Centralized WebSocket topic constants matching system-wide specifications.
 * Used by both broadcast service and WebSocket controllers.
 * Mirror these in the Angular WebSocketTopics constant file.
 */
public final class WebSocketTopics {

    private WebSocketTopics() {}

    // Global Topics (all connected clients)
    public static final String ACTIVE_TERM        = "/topic/terms/active";
    public static final String NOTIFICATIONS      = "/topic/notifications";

    // Per-Student Topics
    public static final String ENROLLMENT         = "/topic/enrollment.%d";
    public static final String GRADES             = "/topic/grades.%d";
    public static final String STUDENT_PROFILE    = "/topic/student.%d";
    public static final String ATTENDANCE         = "/topic/attendance.%d";
    public static final String CLEARANCE          = "/topic/clearance.%d";
    public static final String PERFORMANCE        = "/topic/performance.%d";
    public static final String EQUITY             = "/topic/equity.%d";

    // Per-Class / Section Topics
    public static final String CLASS_SCHEDULE     = "/topic/schedule.%d";
    public static final String ATTENDANCE_CLASS   = "/topic/attendance.class.%d";

    // Per-Faculty Topics
    public static final String FACULTY            = "/topic/faculty.%d";
    public static final String FACULTY_SCHEDULE   = "/topic/schedule.faculty.%d";

    // Admin Topics
    public static final String ADMIN_ENROLLMENTS  = "/topic/admin.enrollments";
    public static final String ADMIN_GRADES       = "/topic/admin.grades";
    public static final String ADMIN_CLEARANCE    = "/topic/admin.clearance";
    public static final String ADMIN_TELEMETRY    = "/topic/admin.telemetry";
    public static final String ADMIN_STUDENTS     = "/topic/admin.students";
    public static final String ADMIN_ATTENDANCE   = "/topic/admin.attendance";

    // Helper Methods
    public static String enrollment(Long studentId) {
        return String.format(ENROLLMENT, studentId);
    }

    public static String grades(Long studentId) {
        return String.format(GRADES, studentId);
    }

    public static String student(Long studentId) {
        return String.format(STUDENT_PROFILE, studentId);
    }

    public static String studentProfile(Long studentId) {
        return String.format(STUDENT_PROFILE, studentId);
    }

    public static String attendance(Long studentId) {
        return String.format(ATTENDANCE, studentId);
    }

    public static String attendanceClass(Long classId) {
        return String.format(ATTENDANCE_CLASS, classId);
    }

    public static String clearance(Long studentId) {
        return String.format(CLEARANCE, studentId);
    }

    public static String performance(Long studentId) {
        return String.format(PERFORMANCE, studentId);
    }

    public static String equity(Long studentId) {
        return String.format(EQUITY, studentId);
    }

    public static String classSchedule(Long classId) {
        return String.format(CLASS_SCHEDULE, classId);
    }

    public static String schedule(Long classId) {
        return String.format(CLASS_SCHEDULE, classId);
    }

    public static String faculty(Long facultyId) {
        return String.format(FACULTY, facultyId);
    }

    public static String facultySchedule(Long facultyId) {
        return String.format(FACULTY_SCHEDULE, facultyId);
    }
}
