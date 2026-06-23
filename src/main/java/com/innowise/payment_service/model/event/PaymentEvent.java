package com.innowise.payment_service.model.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentEvent(
        String paymentId,
        String orderId,
        String userId,
        String status,
        BigDecimal amount,
        LocalDateTime timestamp
) {}