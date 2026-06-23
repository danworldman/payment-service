package com.innowise.payment_service.service;

import com.innowise.payment_service.model.dto.PaymentRequestDto;
import com.innowise.payment_service.model.dto.PaymentResponseDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PaymentService {

    PaymentResponseDto initiatePayment(PaymentRequestDto request, String userId);

    PaymentResponseDto getPaymentById(String id);

    List<PaymentResponseDto> getPaymentsByFilters(String userId, String orderId, String status);

    BigDecimal getTotalSuccessfulPaymentsForUser(String userId, Instant from, Instant to);

    BigDecimal getTotalSuccessfulPaymentsForAll(Instant from, Instant to);

    boolean isPaymentOwnedByUser(String paymentId, String userId);
}