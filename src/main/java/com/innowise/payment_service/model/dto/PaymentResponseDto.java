package com.innowise.payment_service.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponseDto(
        String id,
        String orderId,
        String userId,
        String status,
        LocalDateTime timestamp,
        BigDecimal paymentAmount
) {}