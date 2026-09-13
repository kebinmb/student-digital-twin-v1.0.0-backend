package com.sdt.web_app.service.institution;

import com.sdt.web_app.entities.institution.AcademicYear;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.repositories.institution.AcademicYearRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TermLifecycleService {
    private final TermRepository termRepository;
    private final AcademicYearRepository academicYearRepository;

    @CacheEvict(value = {"terms", "termsById", "activeTerms"}, allEntries = true)
    public Term activateTerm(Long termId) {
        Term targetTerm = termRepository.findWithAcademicYearById(termId)
                .orElseThrow(() -> new IllegalArgumentException("Term not found with ID: " + termId));

        List<Term> activeTerms = termRepository.findAllByIsActiveTrue();
        for (Term currentActive : activeTerms) {
            if (!currentActive.getId().equals(targetTerm.getId())) {
                currentActive.deactivate();
                currentActive.closeEnrollment();
                currentActive.closeGrading();
                currentActive.closeAddDrop();
            }
        }

        AcademicYear parentAy = targetTerm.getAcademicYear();
        academicYearRepository.findFirstByIsCurrentTrueOrderByIdDesc().ifPresent(currentAy -> {
            if (!currentAy.getId().equals(parentAy.getId())) {
                currentAy.unmarkAsCurrent();
            }
        });
        parentAy.markAsCurrent();
        targetTerm.activate();
        return targetTerm;
    }

    @CacheEvict(value = {"terms", "termsById", "activeTerms"}, allEntries = true)
    public Term openEnrollment(Long termId) {
        Term term = getTermOrThrow(termId);
        validateTermIsActive(term);
        term.openEnrollment();
        return term;
    }

    @CacheEvict(value = {"terms", "termsById", "activeTerms"}, allEntries = true)
    public Term closeEnrollment(Long termId) {
        Term term = getTermOrThrow(termId);
        term.closeEnrollment();
        return term;
    }

    @CacheEvict(value = {"terms", "termsById", "activeTerms"}, allEntries = true)
    public Term openGrading(Long termId) {
        Term term = getTermOrThrow(termId);
        validateTermIsActive(term);
        term.openGrading();
        return term;
    }

    @CacheEvict(value = {"terms", "termsById", "activeTerms"}, allEntries = true)
    public Term lockGrading(Long termId) {
        Term term = getTermOrThrow(termId);
        term.closeGrading();
        return term;
    }

    @CacheEvict(value = {"terms", "termsById", "activeTerms"}, allEntries = true)
    public Term toggleAddDrop(Long termId, boolean isOpen) {
        Term term = getTermOrThrow(termId);
        validateTermIsActive(term);
        if (isOpen) {
            term.openAddDrop();
        } else {
            term.closeAddDrop();
        }
        return term;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "activeTerms", key = "'active'")
    public Term getActiveTerm() {
        return termRepository.findFirstByIsActiveTrueOrderByIdDesc()
                .orElseThrow(() -> new IllegalStateException("No term is currently active in the system."));
    }

    private Term getTermOrThrow(Long termId) {
        return termRepository.findWithAcademicYearById(termId)
                .orElseThrow(() -> new IllegalArgumentException("Term not found with ID: " + termId));
    }

    private void validateTermIsActive(Term term) {
        if (!term.isActive()) {
            throw new IllegalStateException("Cannot alter operational windows on an inactive term: " + term.getTermType().name());
        }
    }
}

