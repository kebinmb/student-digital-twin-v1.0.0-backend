package com.sdt.web_app.repositories.institution;

import com.sdt.web_app.entities.institution.CertificateRevocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRevocationRepository extends JpaRepository<CertificateRevocation, Long> {

    Optional<CertificateRevocation> findByCertificateId(String certificateId);

    Optional<CertificateRevocation> findByCertificateIdAndActiveTrue(String certificateId);

    List<CertificateRevocation> findByActiveTrueOrderByRevokedAtDesc();

    List<CertificateRevocation> findAllByOrderByRevokedAtDesc();

    boolean existsByCertificateIdAndActiveTrue(String certificateId);
}
