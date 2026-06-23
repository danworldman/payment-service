package com.innowise.payment_service.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponseDto(
        String id,
        String orderId,
        String userId,
        String status,
        Instant timestamp,
        BigDecimal paymentAmount
) {}