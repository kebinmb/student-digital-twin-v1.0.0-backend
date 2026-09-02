package com.sdt.web_app.service.institution;

import com.sdt.web_app.entities.institution.AcademicYear;
import com.sdt.web_app.entities.institution.Term;
import com.sdt.web_app.repositories.institution.AcademicYearRepository;
import com.sdt.web_app.repositories.institution.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TermLifecycleService {
    private final TermRepository termRepository;
    private final AcademicYearRepository academicYearRepository;

    public Term activateTerm(Long termId) {
        Term targetTerm = termRepository.findById(termId)
                .orElseThrow(() -> new IllegalArgumentException("Term not found with ID: " + termId));

        termRepository.findByIsActiveTrue().ifPresent(currentActive -> {
            if (!currentActive.getId().equals(targetTerm.getId())) {
                currentActive.deactivate();
                currentActive.closeEnrollment();
                currentActive.closeEnrollment();
                currentActive.closeAddDrop();
            }
        });

        AcademicYear parentAy = targetTerm.getAcademicYear();
        academicYearRepository.findByIsCurrentTrue().ifPresent(currentAy -> {
            if (!currentAy.getId().equals(parentAy.getId())) {
                currentAy.unmarkAsCurrent();
            }
        });
        parentAy.markAsCurrent();
        targetTerm.activate();
        return targetTerm;
    }

    public Term openEnrollment(Long termId) {
        Term term = getTermOrThrow(termId);
        validateTermIsActive(term);
        term.openEnrollment();
        return term;
    }

    public Term closeEnrollment(Long termId) {
        Term term = getTermOrThrow(termId);
        term.closeEnrollment();
        return term;
    }

    public Term openGrading(Long termId) {
        Term term = getTermOrThrow(termId);
        validateTermIsActive(term);
        term.openGrading();
        return term;
    }

    public Term lockGrading(Long termId) {
        Term term = getTermOrThrow(termId);
        term.closeGrading();
        return term;
    }

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
    public Term getActiveTerm() {
        return termRepository.findByIsActiveTrue()
                .orElseThrow(() -> new IllegalStateException("No term is currently active in the system."));
    }

    private Term getTermOrThrow(Long termId) {
        return termRepository.findById(termId)
                .orElseThrow(() -> new IllegalArgumentException("Term not found with ID: " + termId));
    }

    private void validateTermIsActive(Term term) {
        if (!term.isActive()) {
            throw new IllegalStateException("Cannot alter operational windows on an inactive term: " + term.getTermType().name());
        }
    }
}

