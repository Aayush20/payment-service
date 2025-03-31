package org.example.paymentservice.repositories;

import org.example.paymentservice.models.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    boolean existsByEventId(String eventId);
}

