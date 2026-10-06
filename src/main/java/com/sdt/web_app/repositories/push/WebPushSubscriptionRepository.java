package com.sdt.web_app.repositories.push;

import com.sdt.web_app.entities.push.WebPushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscription, Long> {

    List<WebPushSubscription> findByUserIdAndActiveTrue(Long userId);

    Optional<WebPushSubscription> findByUserIdAndEndpoint(Long userId, String endpoint);

    void deleteByEndpoint(String endpoint);
}
