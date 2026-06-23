package com.innowise.payment_service.dao;

import com.innowise.payment_service.model.document.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentDAO extends MongoRepository<Payment, String> {

    List<Payment> findByUserId(String userId);

    List<Payment> findByOrderId(String orderId);

    List<Payment> findByStatus(String status);

    Optional<Payment> findByOrderIdAndUserId(String orderId, String userId);

    @Query(value = "{ 'user_id': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
    List<Payment> findPaymentsByUserIdAndDateRange(String userId, LocalDateTime from, LocalDateTime to);

    @Query(value = "{ 'timestamp': { $gte: ?0, $lte: ?1 } }")
    List<Payment> findPaymentsByDateRange(LocalDateTime from, LocalDateTime to);
}