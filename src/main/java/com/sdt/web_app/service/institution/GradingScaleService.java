package com.sdt.web_app.service.institution;

import com.sdt.web_app.entities.institution.GradingScale;
import com.sdt.web_app.repositories.institution.GradingScaleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class GradingScaleService {

    private final GradingScaleRepository gradingScaleRepository;

    @CacheEvict(value = "gradingScales", allEntries = true)
    public GradingScale createGradingScale(
            String code,
            BigDecimal numericGrade,
            BigDecimal percentageMin,
            BigDecimal percentageMax,
            String transmutedGrade,
            String remarks,
            boolean isPassing,
            boolean isNonNumeric
    ) {
        if (gradingScaleRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Grading scale with code already exists: " + code);
        }

        if (percentageMin == null || percentageMax == null) {
            throw new IllegalArgumentException("Percentage min and max cannot be null");
        }
        if (percentageMin.compareTo(BigDecimal.ZERO) < 0 || percentageMax.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException("Percentage bounds must be between 0.00% and 100.00%");
        }
        if (percentageMin.compareTo(percentageMax) > 0) {
            throw new IllegalArgumentException("Percentage min cannot exceed percentage max");
        }
        if (remarks == null || remarks.isBlank()) {
            throw new IllegalArgumentException("Remarks cannot be blank");
        }

        GradingScale scale = GradingScale.builder()
                .code(code.trim().toUpperCase())
                .numericGrade(numericGrade)
                .percentageMin(percentageMin)
                .percentageMax(percentageMax)
                .transmutedGrade(transmutedGrade)
                .remarks(remarks.trim())
                .isPassing(isPassing)
                .isNonNumeric(isNonNumeric)
                .build();

        return gradingScaleRepository.save(scale);
    }

    @CacheEvict(value = "gradingScales", allEntries = true)
    public GradingScale updateGradingScale(Long id, BigDecimal min, BigDecimal max, String remarks, boolean isPassing) {
        GradingScale scale = findScaleById(id);
        scale.updateBracket(min, max, remarks, isPassing);
        return scale;
    }

    @CacheEvict(value = "gradingScales", allEntries = true)
    public void deleteGradingScale(Long id) {
        GradingScale scale = findScaleById(id);
        gradingScaleRepository.delete(scale);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "gradingScales", key = "#id")
    public GradingScale getGradingScaleById(Long id) {
        return findScaleById(id);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "gradingScales", key = "'all'")
    public List<GradingScale> getAllGradingScales() {
        return gradingScaleRepository.findAllByOrderByPercentageMinDesc();
    }

    private GradingScale findScaleById(Long id) {
        return gradingScaleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Grading scale not found with ID: " + id));
    }
}
