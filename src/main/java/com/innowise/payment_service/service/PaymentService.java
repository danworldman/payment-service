package com.innowise.payment_service.service;

import com.innowise.payment_service.model.dto.PaymentRequestDto;
import com.innowise.payment_service.model.dto.PaymentResponseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {

    PaymentResponseDto createPayment(PaymentRequestDto dto);

    List<PaymentResponseDto> getPaymentsByUserId(String userId);

    List<PaymentResponseDto> getPaymentsByOrderId(String orderId);

    List<PaymentResponseDto> getPaymentsByStatus(String status);

    BigDecimal getTotalSumForUser(String userId, LocalDateTime from, LocalDateTime to);

    BigDecimal getTotalSumForAll(LocalDateTime from, LocalDateTime to);
}