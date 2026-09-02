package com.sdt.web_app.service.institution;

import com.sdt.web_app.entities.institution.GradingScale;
import com.sdt.web_app.repositories.institution.GradingScaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GradeTransmutationService {

    private final GradingScaleRepository gradingScaleRepository;


    public GradingScale transmutePercentage(BigDecimal rawPercentage) {
        if (rawPercentage == null) {
            throw new IllegalArgumentException("Raw percentage score cannot be null");
        }

        BigDecimal normalizedPercentage = rawPercentage.setScale(2, RoundingMode.HALF_UP);

        if (normalizedPercentage.compareTo(BigDecimal.ZERO) < 0 || normalizedPercentage.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException("Grade percentage out of range [0.00 - 100.00]: " + normalizedPercentage);
        }

        return gradingScaleRepository.findTransmutationScaleForPercentage(normalizedPercentage)
                .orElseThrow(() -> new IllegalStateException(
                        "No corresponding grading bracket found for percentage: " + normalizedPercentage + "%"));
    }

    public GradingScale resolveNonNumericMark(String markCode) {
        if (markCode == null || markCode.isBlank()) {
            throw new IllegalArgumentException("Mark code cannot be blank");
        }

        return gradingScaleRepository.findByCode(markCode.trim().toUpperCase())
                .filter(GradingScale::isNonNumeric)
                .orElseThrow(() -> new IllegalArgumentException("Invalid non-numeric grade code: " + markCode));
    }

    public boolean isPassingGrade(BigDecimal rawPercentage) {
        return transmutePercentage(rawPercentage).isPassing();
    }
}