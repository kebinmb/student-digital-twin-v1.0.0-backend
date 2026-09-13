package com.sdt.web_app.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.Set;

/**
 * Type-safe property reference mapper for Pageable sorting.
 * Eliminates reflection overhead and prevents Spring Data JPA Property Path Injection vulnerabilities.
 */
public class SortPropertyMapper {

    private SortPropertyMapper() {}

    /**
     * Map of safe client-facing sort fields to explicit JPA Entity property paths for Equity Profiles.
     */
    private static final Map<String, String> EQUITY_PROFILE_SORT_FIELDS = Map.of(
            "updatedat", "updatedAt",
            "createdat", "createdAt",
            "verificationstatus", "verificationStatus",
            "id", "id"
    );

    /**
     * Map of safe client-facing sort fields to explicit JPA Entity property paths for Courses.
     */
    private static final Map<String, String> COURSE_SORT_FIELDS = Map.of(
            "code", "code",
            "title", "title",
            "creditunits", "creditUnits",
            "id", "id"
    );

    /**
     * Map of safe client-facing sort fields for ClassSection.
     */
    private static final Map<String, String> CLASS_SECTION_SORT_FIELDS = Map.of(
            "sectioncode", "sectionCode",
            "maxcapacity", "maxCapacity",
            "enrolledcount", "enrolledCount",
            "status", "status",
            "id", "id"
    );

    /**
     * Map of safe client-facing sort fields for FacultyProfile.
     */
    private static final Map<String, String> FACULTY_PROFILE_SORT_FIELDS = Map.of(
            "employeeid", "employeeId",
            "academicrank", "academicRank",
            "employmentstatus", "employmentStatus",
            "id", "id"
    );

    /**
     * Map of safe client-facing sort fields for Program.
     */
    private static final Map<String, String> PROGRAM_SORT_FIELDS = Map.of(
            "code", "code",
            "name", "name",
            "isactive", "isActive",
            "id", "id"
    );

    /**
     * Map of safe client-facing sort fields for StudentProfile.
     */
    private static final Map<String, String> STUDENT_PROFILE_SORT_FIELDS = Map.of(
            "studentnumber", "studentNumber",
            "yearlevel", "yearLevel",
            "gpa", "cumulativeGpa",
            "id", "id"
    );

    /**
     * Map of safe client-facing sort fields for AttendanceRecord.
     */
    private static final Map<String, String> ATTENDANCE_RECORD_SORT_FIELDS = Map.of(
            "scannedat", "scannedAt",
            "status", "status",
            "id", "id"
    );

    /**
     * Map of safe client-facing sort fields for CashierReceipt.
     */
    private static final Map<String, String> CASHIER_RECEIPT_SORT_FIELDS = Map.of(
            "issuedat", "issuedAt",
            "ornumber", "orNumber",
            "amountpaid", "amountPaid",
            "id", "id"
    );

    /**
     * Map of safe client-facing sort fields for ClassSchedule.
     */
    private static final Map<String, String> CLASS_SCHEDULE_SORT_FIELDS = Map.of(
            "dayofweek", "dayOfWeek",
            "starttime", "startTime",
            "endtime", "endTime",
            "id", "id"
    );

    /**
     * Map of safe client-facing sort fields for StudentRiskScore.
     */
    private static final Map<String, String> STUDENT_RISK_SCORE_SORT_FIELDS = Map.of(
            "evaluatedat", "evaluatedAt",
            "compositerisklevel", "compositeRiskLevel",
            "predicteddropoutprobability", "predictedDropoutProbability",
            "id", "id"
    );

    /**
     * Creates a type-safe Sort instance using explicit property whitelist mapping.
     *
     * @param clientProperty The requested sort property string from the client.
     * @param direction      The requested direction ("ASC" or "DESC").
     * @param defaultProperty The fallback property if clientProperty is null or empty.
     * @param allowedProperties Map of lower-cased client properties to JPA Entity field names.
     * @return Validated, type-safe Sort instance.
     */
    public static Sort createSafeSort(String clientProperty, String direction, String defaultProperty, Map<String, String> allowedProperties) {
        String targetProperty = defaultProperty;

        if (clientProperty != null && !clientProperty.isBlank()) {
            String normalizedKey = clientProperty.trim().toLowerCase();
            if (allowedProperties.containsKey(normalizedKey)) {
                targetProperty = allowedProperties.get(normalizedKey);
            } else {
                throw new IllegalArgumentException("Invalid sort field: '" + clientProperty + "'. Allowed fields: " + allowedProperties.keySet());
            }
        }

        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(sortDirection, targetProperty);
    }

    public static Pageable createEquityProfilePageable(int page, int size, String sortBy, String sortDir) {
        Sort safeSort = createSafeSort(sortBy, sortDir, "updatedAt", EQUITY_PROFILE_SORT_FIELDS);
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), safeSort);
    }

    public static Pageable createCoursePageable(int page, int size, String sortBy, String sortDir) {
        Sort safeSort = createSafeSort(sortBy, sortDir, "code", COURSE_SORT_FIELDS);
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), safeSort);
    }

    public static Pageable createClassSectionPageable(int page, int size, String sortBy, String sortDir) {
        Sort safeSort = createSafeSort(sortBy, sortDir, "sectionCode", CLASS_SECTION_SORT_FIELDS);
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), safeSort);
    }

    public static Pageable createFacultyProfilePageable(int page, int size, String sortBy, String sortDir) {
        Sort safeSort = createSafeSort(sortBy, sortDir, "employeeId", FACULTY_PROFILE_SORT_FIELDS);
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), safeSort);
    }

    public static Pageable createProgramPageable(int page, int size, String sortBy, String sortDir) {
        Sort safeSort = createSafeSort(sortBy, sortDir, "code", PROGRAM_SORT_FIELDS);
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), safeSort);
    }

    public static Pageable createStudentProfilePageable(int page, int size, String sortBy, String sortDir) {
        Sort safeSort = createSafeSort(sortBy, sortDir, "studentNumber", STUDENT_PROFILE_SORT_FIELDS);
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), safeSort);
    }

    public static Pageable createAttendanceRecordPageable(int page, int size, String sortBy, String sortDir) {
        Sort safeSort = createSafeSort(sortBy, sortDir, "scannedAt", ATTENDANCE_RECORD_SORT_FIELDS);
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), safeSort);
    }

    public static Pageable createCashierReceiptPageable(int page, int size, String sortBy, String sortDir) {
        Sort safeSort = createSafeSort(sortBy, sortDir, "issuedAt", CASHIER_RECEIPT_SORT_FIELDS);
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), safeSort);
    }

    public static Pageable createClassSchedulePageable(int page, int size, String sortBy, String sortDir) {
        Sort safeSort = createSafeSort(sortBy, sortDir, "startTime", CLASS_SCHEDULE_SORT_FIELDS);
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), safeSort);
    }

    public static Pageable createStudentRiskScorePageable(int page, int size, String sortBy, String sortDir) {
        Sort safeSort = createSafeSort(sortBy, sortDir, "evaluatedAt", STUDENT_RISK_SCORE_SORT_FIELDS);
        return PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), safeSort);
    }
}
