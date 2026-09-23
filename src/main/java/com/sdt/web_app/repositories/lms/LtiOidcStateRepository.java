package com.sdt.web_app.repositories.lms;

import com.sdt.web_app.entities.lms.LtiOidcState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface LtiOidcStateRepository extends JpaRepository<LtiOidcState, Long> {

    Optional<LtiOidcState> findByStateAndExpiresAtAfter(String state, Instant now);

    void deleteByExpiresAtBefore(Instant now);
}
