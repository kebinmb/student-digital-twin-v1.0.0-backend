package com.sdt.web_app.repositories.admission;

import com.sdt.web_app.entities.admission.AdmissionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdmissionConfigRepository extends JpaRepository<AdmissionConfig, Long> {

    Optional<AdmissionConfig> findByTermId(Long termId);

    Optional<AdmissionConfig> findFirstByIsActiveTrue();
}
