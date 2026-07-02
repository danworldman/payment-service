package com.innowise.payment_service.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentRequestDto(
        @NotNull(message = "Order ID is required")
        Long orderId,

        @NotNull(message = "Payment amount is required")
        @Positive(message = "Payment amount must be positive")
        BigDecimal paymentAmount
) {}