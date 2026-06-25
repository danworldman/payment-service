package com.innowise.payment_service.model.dto;

import com.innowise.payment_service.model.document.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponseDto(
        String id,
        Long  orderId,
        Long  userId,
        PaymentStatus status,
        Instant timestamp,
        BigDecimal paymentAmount
) {}