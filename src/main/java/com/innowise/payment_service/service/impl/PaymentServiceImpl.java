package com.innowise.payment_service.service.impl;

import com.innowise.payment_service.dao.PaymentDAO;
import com.innowise.payment_service.exception.PaymentNotFoundException;
import com.innowise.payment_service.mapper.PaymentMapper;
import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.document.PaymentStatus;
import com.innowise.payment_service.model.dto.PaymentRequestDto;
import com.innowise.payment_service.model.dto.PaymentResponseDto;
import com.innowise.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentDAO paymentDAO;
    private final PaymentMapper paymentMapper;
    private final PaymentProcessor paymentProcessor;

    @Override
    public PaymentResponseDto initiatePayment(PaymentRequestDto paymentFields, Long userId) {
        Payment payment = Payment.builder()
                .orderId(paymentFields.orderId())
                .userId(userId)
                .paymentAmount(paymentFields.paymentAmount())
                .status(PaymentStatus.PENDING)
                .timestamp(Instant.now())
                .build();

        Payment savedPayment = paymentDAO.save(payment);
        paymentProcessor.processPaymentAsync(savedPayment);
        return paymentMapper.toResponseDto(savedPayment);
    }

    @Override
    public PaymentResponseDto getPaymentById(String id) {
        Payment payment = paymentDAO.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
        return paymentMapper.toResponseDto(payment);
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByFilters(Long userId, Long orderId, PaymentStatus status) {
        List<Payment> payments;
        if (userId != null && orderId != null && status != null) {
            payments = paymentDAO.findByUserIdAndOrderIdAndStatus(userId, orderId, status);
        } else if (userId != null && orderId != null) {
            payments = paymentDAO.findByUserIdAndOrderId(userId, orderId);
        } else if (userId != null && status != null) {
            payments = paymentDAO.findByUserIdAndStatus(userId, status);
        } else if (orderId != null && status != null) {
            payments = paymentDAO.findByOrderIdAndStatus(orderId, status);
        } else if (userId != null) {
            payments = paymentDAO.findByUserId(userId);
        } else if (orderId != null) {
            payments = paymentDAO.findByOrderId(orderId);
        } else if (status != null) {
            payments = paymentDAO.findByStatus(status);
        } else {
            payments = paymentDAO.findAll();
        }
        return payments.stream()
                .map(paymentMapper::toResponseDto)
                .toList();
    }

    @Override
    public BigDecimal getTotalSuccessfulPaymentsForUser(Long userId, Instant from, Instant to) {
        return paymentDAO.getTotalSuccessfulPaymentsByUserIdAndDateRange(userId, from, to);
    }

    @Override
    public BigDecimal getTotalSuccessfulPaymentsForAll(Instant from, Instant to) {
        return paymentDAO.getTotalSuccessfulPaymentsByDateRange(from, to);
    }

    @Override
    public boolean isPaymentOwnedByUser(String id, Long userId) {
        Payment payment = paymentDAO.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
        return payment.getUserId().equals(userId);
    }
}