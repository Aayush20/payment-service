package org.example.paymentservice.repositories;

import org.example.paymentservice.models.WebhookRetryTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookRetryTaskRepository extends JpaRepository<WebhookRetryTask, Long> {
    List<WebhookRetryTask> findByProcessedFalse();

    List<WebhookRetryTask> findTop10ByProcessedFalseOrderByCreatedAtAsc();
}
