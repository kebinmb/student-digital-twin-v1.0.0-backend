package com.sdt.web_app.service.analytics;

import com.sdt.web_app.entities.enrollment.StudentProfile;
import com.sdt.web_app.repositories.enrollment.StudentProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class EquityTargetService {

    private final StudentProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public BigDecimal calculateSocioeconomicRiskScore(Long studentId) {
        StudentProfile student = profileRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found: " + studentId));

        // Evaluate RA 10931 / UniFAST priority vulnerability score (0.00 to 100.00)
        double baseScore = 15.0; // Baseline low vulnerability

        // Higher financial dependency / low family income tier increases equity priority score
        if (student.getFinancialClearance() != null && "PENDING".equalsIgnoreCase(student.getFinancialClearance().name())) {
            baseScore += 25.0;
        }

        // Transferee/Returnee transition adjustment
        if ("TRANSFEREE".equalsIgnoreCase(student.getEnrollmentStatus().name()) || "RETURNEE".equalsIgnoreCase(student.getEnrollmentStatus().name())) {
            baseScore += 20.0;
        }

        return new BigDecimal(baseScore).setScale(2, RoundingMode.HALF_UP);
    }
}
