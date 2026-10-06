package com.sdt.web_app.repositories.webhook;

import com.sdt.web_app.entities.webhook.InstitutionalWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstitutionalWebhookRepository extends JpaRepository<InstitutionalWebhook, Long> {
    List<InstitutionalWebhook> findByActiveTrue();
}
