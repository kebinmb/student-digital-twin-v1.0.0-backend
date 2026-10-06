package com.sdt.web_app.repositories.webhook;

import com.sdt.web_app.entities.webhook.InstitutionalWebhookDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface InstitutionalWebhookDeliveryRepository extends JpaRepository<InstitutionalWebhookDelivery, Long> {
    List<InstitutionalWebhookDelivery> findByStatusAndNextRetryAtBefore(
            InstitutionalWebhookDelivery.DeliveryStatus status,
            Instant cutoff
    );

    List<InstitutionalWebhookDelivery> findByWebhookIdOrderByCreatedAtDesc(Long webhookId);
}
