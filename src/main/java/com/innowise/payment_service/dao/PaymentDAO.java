package com.innowise.payment_service.dao;

import com.innowise.payment_service.model.document.Payment;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentDAO extends MongoRepository<Payment, String> {

    List<Payment> findByUserId(String userId);

    List<Payment> findByOrderId(String orderId);

    List<Payment> findByStatus(String status);

    Optional<Payment> findByOrderIdAndUserId(String orderId, String userId);

    @Query(value = "{ 'order_id': ?0, 'status': ?1 }")
    List<Payment> findByOrderIdAndStatus(String orderId, String status);

    @Query(value = "{ 'user_id': ?0, 'status': ?1 }")
    List<Payment> findByUserIdAndStatus(String userId, String status);

    @Aggregation(pipeline = {
            "{ $match: { 'user_id': ?0, 'status': 'SUCCESS', 'timestamp': { $gte: ?1, $lte: ?2 } } }",
            "{ $group: { _id: null, total: { $sum: '$payment_amount' } } }"
    })
    Optional<AggregationResult> getTotalSuccessfulPaymentsForUser(String userId, Instant from, Instant to);

    @Aggregation(pipeline = {
            "{ $match: { 'status': 'SUCCESS', 'timestamp': { $gte: ?0, $lte: ?1 } } }",
            "{ $group: { _id: null, total: { $sum: '$payment_amount' } } }"
    })
    Optional<AggregationResult> getTotalSuccessfulPaymentsForAll(Instant from, Instant to);

    record AggregationResult(BigDecimal total) {}
}