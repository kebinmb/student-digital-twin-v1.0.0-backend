package com.sdt.web_app.service.institution;

import com.sdt.web_app.entities.institution.AcademicYear;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.institution.TermType;
import com.sdt.web_app.repositories.institution.AcademicYearRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TermLifecycleServiceTest {

    @Mock
    private TermRepository termRepository;

    @Mock
    private AcademicYearRepository academicYearRepository;

    @InjectMocks
    private TermLifecycleService termLifecycleService;

    private AcademicYear academicYear;
    private Term term;

    @BeforeEach
    void setUp() {
        academicYear = AcademicYear.builder()
                .id(1L)
                .code("AY 2026-2027")
                .startDate(LocalDate.of(2026, 8, 10))
                .endDate(LocalDate.of(2027, 7, 16))
                .isCurrent(false)
                .build();

        term = Term.builder()
                .id(10L)
                .academicYear(academicYear)
                .termType(TermType.FIRST_SEM)
                .startDate(LocalDate.of(2026, 8, 10))
                .endDate(LocalDate.of(2026, 12, 18))
                .isActive(false)
                .build();
    }

    @Test
    @DisplayName("Should successfully activate a term and set parent academic year as current")
    void activateTerm_Success() {
        given(termRepository.findWithAcademicYearById(10L)).willReturn(Optional.of(term));
        given(termRepository.findByIsActiveTrue()).willReturn(Optional.empty());
        given(academicYearRepository.findByIsCurrentTrue()).willReturn(Optional.empty());

        Term result = termLifecycleService.activateTerm(10L);

        assertThat(result.isActive()).isTrue();
        assertThat(academicYear.isCurrent()).isTrue();
    }

    @Test
    @DisplayName("Should open enrollment when term is active")
    void openEnrollment_Success() {
        term.activate();
        given(termRepository.findWithAcademicYearById(10L)).willReturn(Optional.of(term));

        Term result = termLifecycleService.openEnrollment(10L);

        assertThat(result.isEnrollmentOpen()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when attempting to open enrollment on inactive term")
    void openEnrollment_InactiveTerm_ThrowsException() {
        given(termRepository.findWithAcademicYearById(10L)).willReturn(Optional.of(term));

        assertThatThrownBy(() -> termLifecycleService.openEnrollment(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot alter operational windows on an inactive term");
    }

    @Test
    @DisplayName("Should open and lock grading windows on active term")
    void gradingLifecycle_Success() {
        term.activate();
        given(termRepository.findWithAcademicYearById(10L)).willReturn(Optional.of(term));

        termLifecycleService.openGrading(10L);
        assertThat(term.isGradingOpen()).isTrue();

        termLifecycleService.lockGrading(10L);
        assertThat(term.isGradingOpen()).isFalse();
    }

    @Test
    @DisplayName("Should return currently active term")
    void getActiveTerm_Success() {
        term.activate();
        given(termRepository.findByIsActiveTrue()).willReturn(Optional.of(term));

        Term activeTerm = termLifecycleService.getActiveTerm();

        assertThat(activeTerm.getId()).isEqualTo(10L);
    }
}
