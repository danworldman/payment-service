package com.innowise.payment_service.model.event;

public record PaymentEvent(
        String orderId,
        String status
) {}