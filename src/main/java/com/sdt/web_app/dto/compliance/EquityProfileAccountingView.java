package com.sdt.web_app.dto.compliance;

import com.sdt.web_app.entities.compliance.StudentEquityProfile;
import com.sdt.web_app.entities.enrollment.StudentProfile;

/**
 * Stripped equity profile view for ACCOUNTANT role.
 * Contains only fields needed for fee computation and scholarship adjustment.
 * Does NOT include: evidence files, verification notes, officer remarks,
 * personal demographic details beyond what is needed for financial processing.
 */
public record EquityProfileAccountingView(
        Long studentId,
        String studentNumber,
        String studentName,        // from student_profiles (first_name + last_name)
        String equityCategory,     // e.g., PWD, IP, SOLO_PARENT, 4PS
        String verificationStatus, // PENDING / VERIFIED / REJECTED
        String programCode,        // for fee schedule lookup
        Long termId                // for term-scoped financial processing
) {
    public static EquityProfileAccountingView from(StudentEquityProfile entity) {
        if (entity == null) return null;
        StudentProfile sp = entity.getStudentProfile();
        Long studentId = sp != null ? sp.getId() : null;
        String studentNumber = sp != null ? sp.getStudentNumber() : null;
        String studentName = sp != null ? sp.getFullName() : null;
        String programCode = (sp != null && sp.getProgram() != null) ? sp.getProgram().getCode() : null;
        String verificationStatus = entity.getVerificationStatus() != null ? entity.getVerificationStatus().name() : "VERIFIED";

        String category = "STANDARD";
        if (Boolean.TRUE.equals(entity.getIsPersonWithDisability())) {
            category = "PWD";
        } else if (Boolean.TRUE.equals(entity.getIs4psBeneficiary())) {
            category = "4PS";
        } else if (Boolean.TRUE.equals(entity.getIsIndigenousPeople())) {
            category = "IP";
        } else if (Boolean.TRUE.equals(entity.getIsSoloParent())) {
            category = "SOLO_PARENT";
        } else if (Boolean.TRUE.equals(entity.getIsFarmerFisherfolk())) {
            category = "FARMER_FISHERFOLK";
        } else if (Boolean.TRUE.equals(entity.getIsGidaResident())) {
            category = "GIDA";
        } else if (Boolean.TRUE.equals(entity.getIsBottom40IncomeBracket())) {
            category = "BOTTOM_40";
        } else if (Boolean.TRUE.equals(entity.getIsFirstGenerationCollege())) {
            category = "FIRST_GEN";
        }

        return new EquityProfileAccountingView(
                studentId,
                studentNumber,
                studentName,
                category,
                verificationStatus,
                programCode,
                null
        );
    }
}
