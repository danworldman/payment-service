package com.innowise.payment_service.dao;

import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.document.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PaymentDAOCustom {

    BigDecimal getTotalSuccessfulPaymentsByUserIdAndDateRange(Long userId, Instant from, Instant to);

    BigDecimal getTotalSuccessfulPaymentsByDateRange(Instant from, Instant to);

    List<Payment> findByDynamicFilters(Long userId, Long orderId, PaymentStatus status);
}