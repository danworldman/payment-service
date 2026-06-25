package com.innowise.payment_service.model.event;

public record PaymentCompletedEvent(
        Long orderId,
        String status
) {}