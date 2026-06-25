package com.innowise.payment_service.dao;

import java.math.BigDecimal;
import java.time.Instant;

public interface PaymentDAOCustom {

    BigDecimal getTotalSuccessfulPaymentsByUserIdAndDateRange(Long userId, Instant from, Instant to);

    BigDecimal getTotalSuccessfulPaymentsByDateRange(Instant from, Instant to);
}