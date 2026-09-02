package com.sdt.web_app.service.institution;

import com.sdt.web_app.entities.institution.GradingScale;
import com.sdt.web_app.repositories.institution.GradingScaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GradeTransmutationServiceTest {

    @Mock
    private GradingScaleRepository gradingScaleRepository;

    @InjectMocks
    private GradeTransmutationService gradeTransmutationService;

    private GradingScale excellentScale;
    private GradingScale incScale;

    @BeforeEach
    void setUp() {
        excellentScale = GradingScale.builder()
                .id(1L)
                .code("1.00")
                .numericGrade(new BigDecimal("1.00"))
                .percentageMin(new BigDecimal("97.00"))
                .percentageMax(new BigDecimal("100.00"))
                .transmutedGrade("1.00")
                .remarks("EXCELLENT")
                .isPassing(true)
                .isNonNumeric(false)
                .build();

        incScale = GradingScale.builder()
                .id(11L)
                .code("INC")
                .numericGrade(null)
                .percentageMin(BigDecimal.ZERO)
                .percentageMax(BigDecimal.ZERO)
                .transmutedGrade("INC")
                .remarks("INCOMPLETE")
                .isPassing(false)
                .isNonNumeric(true)
                .build();
    }

    @Test
    @DisplayName("Should successfully transmute a raw percentage score to a GradingScale")
    void transmutePercentage_Success() {
        BigDecimal percentage = new BigDecimal("98.50");
        given(gradingScaleRepository.findTransmutationScaleForPercentage(new BigDecimal("98.50")))
                .willReturn(Optional.of(excellentScale));

        GradingScale result = gradeTransmutationService.transmutePercentage(percentage);

        assertThat(result.getCode()).isEqualTo("1.00");
        assertThat(result.getRemarks()).isEqualTo("EXCELLENT");
        assertThat(result.isPassing()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when percentage score is out of range")
    void transmutePercentage_OutOfRange_ThrowsException() {
        assertThatThrownBy(() -> gradeTransmutationService.transmutePercentage(new BigDecimal("105.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Grade percentage out of range");
    }

    @Test
    @DisplayName("Should resolve non-numeric grade code successfully")
    void resolveNonNumericMark_Success() {
        given(gradingScaleRepository.findByCode("INC")).willReturn(Optional.of(incScale));

        GradingScale result = gradeTransmutationService.resolveNonNumericMark("inc");

        assertThat(result.getCode()).isEqualTo("INC");
        assertThat(result.isNonNumeric()).isTrue();
        assertThat(result.isPassing()).isFalse();
    }
}
