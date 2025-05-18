package org.example.paymentservice.repositories;

import org.example.paymentservice.models.RetryDeadLetterLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetryDeadLetterLogRepository extends JpaRepository<RetryDeadLetterLog, Long> {}
