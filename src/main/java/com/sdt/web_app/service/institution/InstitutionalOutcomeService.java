package com.sdt.web_app.service.institution;

import com.sdt.web_app.entities.institution.InstitutionalOutcome;
import com.sdt.web_app.repositories.institution.InstitutionalOutcomeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstitutionalOutcomeService {

    private final InstitutionalOutcomeRepository repository;

    @Transactional(readOnly = true)
    public List<InstitutionalOutcome> getAllActiveOutcomes() {
        return repository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public InstitutionalOutcome getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Institutional outcome not found with ID: " + id));
    }

    @Transactional
    public InstitutionalOutcome createOutcome(String code, String statement, String description) {
        repository.findByCode(code).ifPresent(existing -> {
            throw new IllegalArgumentException("Institutional outcome code already exists: " + code);
        });

        InstitutionalOutcome outcome = InstitutionalOutcome.builder()
                .code(code.toUpperCase().trim())
                .statement(statement.trim())
                .description(description != null ? description.trim() : null)
                .active(true)
                .build();

        return repository.save(outcome);
    }
}
