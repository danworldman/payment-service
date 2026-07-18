package com.innowise.payment_service.service;

import com.innowise.payment_service.model.document.PaymentStatus;
import com.innowise.payment_service.model.dto.PaymentRequestDto;
import com.innowise.payment_service.model.dto.PaymentResponseDto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PaymentService {

    PaymentResponseDto initiatePayment(PaymentRequestDto paymentFields, Long userId);

    PaymentResponseDto getPaymentById(String id);

    List<PaymentResponseDto> getPaymentsByFilters(Long userId, Long orderId, PaymentStatus status);

    BigDecimal getTotalSuccessfulPaymentsForUser(Long userId, Instant from, Instant to);

    BigDecimal getTotalSuccessfulPaymentsForAll(Instant from, Instant to);

    boolean isPaymentOwnedByUser(String id, Long userId);
}