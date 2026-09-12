package com.sdt.web_app.repositories.lms;

import com.sdt.web_app.entities.lms.LtiUserMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LtiUserMappingRepository extends JpaRepository<LtiUserMapping, Long> {
    Optional<LtiUserMapping> findByDeploymentIdAndSubClaim(Long deploymentId, String subClaim);
}
