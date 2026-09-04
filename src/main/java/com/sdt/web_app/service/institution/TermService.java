package com.sdt.web_app.service.institution;

import com.sdt.web_app.entities.institution.AcademicYear;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.institution.TermType;
import com.sdt.web_app.repositories.institution.AcademicYearRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TermService {

    private final TermRepository termRepository;
    private final AcademicYearRepository academicYearRepository;

    public Term createTerm(Long academicYearId, TermType termType, LocalDate startDate, LocalDate endDate) {
        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new IllegalArgumentException("Academic year not found with ID: " + academicYearId));

        if (termRepository.existsByAcademicYearIdAndTermType(academicYearId, termType)) {
            throw new IllegalArgumentException("Term of type " + termType + " already exists for academic year " + academicYear.getCode());
        }

        if (startDate != null && endDate != null && !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("Term end date must be after start date");
        }

        Term term = Term.builder()
                .academicYear(academicYear)
                .termType(termType)
                .startDate(startDate)
                .endDate(endDate)
                .enrollmentOpen(false)
                .gradingOpen(false)
                .addDropOpen(false)
                .isActive(false)
                .build();

        return termRepository.save(term);
    }

    public Term updateTermSchedule(Long termId, LocalDate startDate, LocalDate endDate) {
        Term term = findTermById(termId);
        term.updateSchedule(startDate, endDate);
        return term;
    }

    public void deleteTerm(Long termId) {
        Term term = findTermById(termId);
        if (term.isActive()) {
            throw new IllegalStateException("Cannot delete an active operational academic term");
        }
        termRepository.delete(term);
    }

    @Transactional(readOnly = true)
    public Term getTermById(Long termId) {
        return findTermById(termId);
    }

    @Transactional(readOnly = true)
    public List<Term> getAllTerms() {
        return termRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Term> getTermsByAcademicYear(Long academicYearId) {
        if (!academicYearRepository.existsById(academicYearId)) {
            throw new IllegalArgumentException("Academic year not found with ID: " + academicYearId);
        }
        return termRepository.findByAcademicYearId(academicYearId);
    }

    private Term findTermById(Long termId) {
        return termRepository.findById(termId)
                .orElseThrow(() -> new EntityNotFoundException("Term not found with ID: " + termId));
    }
}
