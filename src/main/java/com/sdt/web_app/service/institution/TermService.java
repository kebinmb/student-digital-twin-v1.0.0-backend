package com.sdt.web_app.service.institution;

import com.sdt.web_app.entities.institution.AcademicYear;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.entities.institution.TermType;
import com.sdt.web_app.repositories.enrollment.StudentEnrollmentRepository;
import com.sdt.web_app.repositories.institution.AcademicYearRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import com.sdt.web_app.repositories.scheduling.ClassSectionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final ClassSectionRepository classSectionRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;

    @CacheEvict(value = {"terms", "termsById", "activeTerms"}, allEntries = true)
    public Term createTerm(Long academicYearId, TermType termType, LocalDate startDate, LocalDate endDate) {
        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new IllegalArgumentException("Academic year not found with ID: " + academicYearId));

        if (termRepository.existsByAcademicYearIdAndTermType(academicYearId, termType)) {
            throw new IllegalArgumentException("Term of type " + termType + " already exists for academic year " + academicYear.getCode());
        }

        validateTermDates(academicYear, null, startDate, endDate);

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

    @CacheEvict(value = {"terms", "termsById", "activeTerms"}, allEntries = true)
    public Term updateTermSchedule(Long termId, LocalDate startDate, LocalDate endDate) {
        Term term = findTermById(termId);
        validateTermDates(term.getAcademicYear(), termId, startDate, endDate);
        term.updateSchedule(startDate, endDate);
        return term;
    }

    @CacheEvict(value = {"terms", "termsById", "activeTerms"}, allEntries = true)
    public void deleteTerm(Long termId) {
        Term term = findTermById(termId);
        if (term.isActive()) {
            throw new IllegalStateException("Cannot delete an active operational academic term.");
        }
        if (classSectionRepository.existsByTermId(termId)) {
            throw new IllegalStateException("Cannot delete term because active class section schedules are attached.");
        }
        if (studentEnrollmentRepository.existsByTermId(termId)) {
            throw new IllegalStateException("Cannot delete term because student enrollments are attached.");
        }
        termRepository.delete(term);
    }

    private void validateTermDates(AcademicYear academicYear, Long excludeTermId, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("Term end date must be strictly after start date.");
        }

        if (startDate != null && endDate != null && academicYear != null) {
            if (academicYear.getStartDate() != null && startDate.isBefore(academicYear.getStartDate())) {
                throw new IllegalArgumentException("Term start date (" + startDate + ") cannot precede Academic Year start date (" + academicYear.getStartDate() + ").");
            }
            if (academicYear.getEndDate() != null && endDate.isAfter(academicYear.getEndDate())) {
                throw new IllegalArgumentException("Term end date (" + endDate + ") cannot exceed Academic Year end date (" + academicYear.getEndDate() + ").");
            }

            List<Term> existingTerms = termRepository.findByAcademicYearId(academicYear.getId());
            for (Term existing : existingTerms) {
                if (excludeTermId != null && existing.getId().equals(excludeTermId)) {
                    continue;
                }
                if (existing.getStartDate() != null && existing.getEndDate() != null) {
                    boolean overlaps = (startDate.isBefore(existing.getEndDate()) && endDate.isAfter(existing.getStartDate()))
                            || startDate.isEqual(existing.getStartDate())
                            || endDate.isEqual(existing.getEndDate());
                    if (overlaps) {
                        throw new IllegalArgumentException("Term schedule (" + startDate + " to " + endDate + ") overlaps with existing term " + existing.getTermType() + " (" + existing.getStartDate() + " to " + existing.getEndDate() + ").");
                    }
                }
            }
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "terms", key = "#termId")
    public Term getTermById(Long termId) {
        return findTermById(termId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "activeTerms", key = "'active'")
    public Term getActiveTerm() {
        return termRepository.findFirstByIsActiveTrueOrderByIdDesc()
                .or(() -> termRepository.findAll().stream().filter(Term::isActive).findFirst())
                .or(() -> termRepository.findAll().stream().findFirst())
                .orElseThrow(() -> new EntityNotFoundException("No active term found."));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "terms", key = "'all'")
    public List<Term> getAllTerms() {
        return termRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "terms", key = "'ay:' + #academicYearId")
    public List<Term> getTermsByAcademicYear(Long academicYearId) {
        if (!academicYearRepository.existsById(academicYearId)) {
            throw new IllegalArgumentException("Academic year not found with ID: " + academicYearId);
        }
        return termRepository.findByAcademicYearId(academicYearId);
    }

    private Term findTermById(Long termId) {
        return termRepository.findWithAcademicYearById(termId)
                .orElseThrow(() -> new EntityNotFoundException("Term not found with ID: " + termId));
    }
}
