package com.innowise.payment_service.dao;

import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.document.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentDAO extends MongoRepository<Payment, String>, PaymentDAOCustom {

    List<Payment> findByOrderId(Long orderId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByUserId(Long userId);

    List<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

    List<Payment> findByUserIdAndOrderId(Long userId, Long orderId);

    List<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status);

    List<Payment> findByUserIdAndOrderIdAndStatus(Long userId, Long orderId, PaymentStatus status);
}