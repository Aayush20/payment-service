package org.example.paymentservice.repositories;

import org.example.paymentservice.models.Payment;
import org.example.paymentservice.models.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime before);
    Payment findByOrderId(String orderId);
    List<Payment> findByUserId(String userId);


}

