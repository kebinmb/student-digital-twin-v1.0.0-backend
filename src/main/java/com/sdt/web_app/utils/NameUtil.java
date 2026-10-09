package com.sdt.web_app.utils;

import com.sdt.web_app.entities.enrollment.StudentProfile;

/**
 * Utility for building human-readable full names from student_profiles fields.
 * Used across all modules that expose studentName or fullName in DTOs.
 */
public final class NameUtil {

    private NameUtil() {
        // Utility class
    }

    /**
     * Builds a full name from first, middle, and last names.
     * Format: "FirstName M. LastName" or "FirstName LastName" if middle name is null/empty.
     */
    public static String buildFullName(String firstName, String middleName, String lastName) {
        return buildFullName(firstName, middleName, lastName, null);
    }

    /**
     * Builds a full name from first, middle, last names, and suffix.
     * Format: "FirstName M. LastName Suffix"
     */
    public static String buildFullName(String firstName, String middleName, String lastName, String suffix) {
        String trimmedFirst = (firstName != null) ? firstName.trim() : "";
        String trimmedLast = (lastName != null) ? lastName.trim() : "";
        String trimmedMiddle = (middleName != null) ? middleName.trim() : "";
        String trimmedSuffix = (suffix != null) ? suffix.trim() : "";

        StringBuilder sb = new StringBuilder();

        if (!trimmedFirst.isEmpty()) {
            sb.append(trimmedFirst);
        }

        if (!trimmedMiddle.isEmpty()) {
            String middleInitial = trimmedMiddle.substring(0, 1).toUpperCase() + ".";
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(middleInitial);
        }

        if (!trimmedLast.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(trimmedLast);
        }

        if (!trimmedSuffix.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(trimmedSuffix);
        }

        return sb.toString().trim();
    }

    /**
     * Overload accepting StudentProfile entity directly.
     */
    public static String buildFullName(StudentProfile profile) {
        if (profile == null) {
            return "";
        }
        String name = buildFullName(
                profile.getFirstName(),
                profile.getMiddleName(),
                profile.getLastName(),
                profile.getSuffix()
        );
        if (name.isEmpty()) {
            if (profile.getUser() != null && profile.getUser().getUsername() != null && !profile.getUser().getUsername().isBlank()) {
                return profile.getUser().getUsername();
            }
            return profile.getStudentNumber() != null ? profile.getStudentNumber() : "";
        }
        return name;
    }
}
