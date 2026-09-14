package com.sdt.web_app.service.analytics;

import com.sdt.web_app.entities.admission.AdmissionApplication;
import com.sdt.web_app.entities.compliance.StudentEquityProfile;
import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.compliance.StudentEquityProfileRepository;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EquityTargetService {

    private final StudentProfileRepository profileRepository;
    private final StudentEquityProfileRepository equityProfileRepository;

    @Transactional(readOnly = true)
    public BigDecimal calculateSocioeconomicRiskScore(Long studentId) {
        StudentProfile student = profileRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found: " + studentId));

        // Evaluate RA 10931 / UniFAST priority socioeconomic vulnerability score (0.00 to 100.00)
        double score = 10.0; // Baseline low vulnerability

        // Academic enrollment & financial clearance adjustment
        if (student.getFinancialClearance() != null && "PENDING".equalsIgnoreCase(student.getFinancialClearance().name())) {
            score += 20.0;
        }

        if (student.getEnrollmentStatus() != null &&
                ("TRANSFEREE".equalsIgnoreCase(student.getEnrollmentStatus().name()) || "RETURNEE".equalsIgnoreCase(student.getEnrollmentStatus().name()))) {
            score += 15.0;
        }

        // Wire Philippine statutory equity indicator signals
        Optional<StudentEquityProfile> equityOpt = equityProfileRepository.findByStudentProfileId(studentId);
        if (equityOpt.isPresent()) {
            StudentEquityProfile eq = equityOpt.get();

            // 1. Bottom 40% / Poverty line income vulnerability
            if (Boolean.TRUE.equals(eq.getIsBottom40IncomeBracket()) ||
                    (eq.getMonthlyHouseholdIncomeBracket() != null && eq.getMonthlyHouseholdIncomeBracket() == StudentEquityProfile.HouseholdIncomeBracket.POOR_BELOW_10K)) {
                score += 20.0;
            } else if (eq.getMonthlyHouseholdIncomeBracket() != null && eq.getMonthlyHouseholdIncomeBracket() == StudentEquityProfile.HouseholdIncomeBracket.LOW_INCOME_10K_TO_20K) {
                score += 10.0;
            }

            // 2. 4Ps Pantawid Pamilyang Pilipino Beneficiary
            if (Boolean.TRUE.equals(eq.getIs4psBeneficiary())) {
                score += 15.0;
            }

            // 3. Person with Disability (PWD)
            if (Boolean.TRUE.equals(eq.getIsPersonWithDisability())) {
                score += 15.0;
            }

            // 4. Solo Parent or Raised by Solo Parent
            if (Boolean.TRUE.equals(eq.getIsSoloParent()) || Boolean.TRUE.equals(eq.getIsRaisedBySoloParent())) {
                score += 10.0;
            }

            // 5. Orphan Status
            if (Boolean.TRUE.equals(eq.getIsOrphan())) {
                score += 15.0;
            }

            // 6. Geographically Isolated and Disadvantaged Area (GIDA)
            if (Boolean.TRUE.equals(eq.getIsGidaResident())) {
                score += 10.0;
            }

            // 7. Subsistence Farmer or Fisherfolk Family
            if (Boolean.TRUE.equals(eq.getIsFarmerFisherfolk())) {
                score += 10.0;
            }

            // 8. Rebel Returnee / E-CLIP Family
            if (Boolean.TRUE.equals(eq.getIsRebelReturneeFamily())) {
                score += 10.0;
            }

            // 9. Indigenous Peoples (IP)
            if (Boolean.TRUE.equals(eq.getIsIndigenousPeople())) {
                score += 10.0;
            }

            // 10. First Generation College Student
            if (Boolean.TRUE.equals(eq.getIsFirstGenerationCollege())) {
                score += 10.0;
            }
        }

        // Clamp between 0.00 and 100.00
        double clampedScore = Math.min(100.0, Math.max(0.0, score));
        return new BigDecimal(clampedScore).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateApplicantSocioeconomicRiskScore(AdmissionApplication app) {
        if (app == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        double score = 10.0; // Baseline low vulnerability

        // Academic / High school sector adjustment (RA 10931 affirmative action priority)
        if (app.getHighSchoolType() != null && "PUBLIC".equalsIgnoreCase(app.getHighSchoolType())) {
            score += 10.0;
        }

        // 1. Bottom 40% / Poverty line income vulnerability
        if (app.isBottom40IncomeBracket() || "POOR_BELOW_10K".equalsIgnoreCase(app.getMonthlyHouseholdIncomeBracket())) {
            score += 20.0;
        } else if ("LOW_INCOME_10K_TO_20K".equalsIgnoreCase(app.getMonthlyHouseholdIncomeBracket())) {
            score += 10.0;
        }

        // 2. 4Ps Pantawid Pamilyang Pilipino Beneficiary
        if (app.is4psBeneficiary()) {
            score += 15.0;
        }

        // 3. Person with Disability (PWD)
        if (app.isPersonWithDisability()) {
            score += 15.0;
        }

        // 4. Solo Parent or Raised by Solo Parent
        if (app.isSoloParent() || app.isRaisedBySoloParent()) {
            score += 10.0;
        }

        // 5. Orphan Status
        if (app.isOrphan()) {
            score += 15.0;
        }

        // 6. Geographically Isolated and Disadvantaged Area (GIDA)
        if (app.isGidaResident()) {
            score += 10.0;
        }

        // 7. Subsistence Farmer or Fisherfolk Family
        if (app.isFarmerFisherfolk()) {
            score += 10.0;
        }

        // 8. Rebel Returnee / E-CLIP Family
        if (app.isRebelReturneeFamily()) {
            score += 10.0;
        }

        // 9. Indigenous Peoples (IP)
        if (app.isIndigenousPeople()) {
            score += 10.0;
        }

        // 10. First Generation College Student
        if (app.isFirstGenerationCollege()) {
            score += 10.0;
        }

        double clampedScore = Math.min(100.0, Math.max(0.0, score));
        return new BigDecimal(clampedScore).setScale(2, RoundingMode.HALF_UP);
    }
}
