package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.PaymentTermTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTermTemplateRepository extends JpaRepository<PaymentTermTemplate, Long> {

    Optional<PaymentTermTemplate> findByName(String name);

    boolean existsByName(String name);
}
