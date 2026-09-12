package com.sdt.web_app.repositories.lms;

import com.sdt.web_app.entities.lms.LtiDeployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LtiDeploymentRepository extends JpaRepository<LtiDeployment, Long> {
    Optional<LtiDeployment> findByClientIdAndDeploymentId(String clientId, String deploymentId);
}
