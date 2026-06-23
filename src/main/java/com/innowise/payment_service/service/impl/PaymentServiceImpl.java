package com.innowise.payment_service.service.impl;

import com.innowise.payment_service.client.ExternalPaymentApiClient;
import com.innowise.payment_service.dao.PaymentDAO;
import com.innowise.payment_service.exception.PaymentNotFoundException;
import com.innowise.payment_service.kafka.PaymentEventProducer;
import com.innowise.payment_service.mapper.PaymentMapper;
import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.dto.PaymentRequestDto;
import com.innowise.payment_service.model.dto.PaymentResponseDto;
import com.innowise.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentDAO paymentDAO;
    private final PaymentMapper paymentMapper;
    private final ExternalPaymentApiClient externalApiClient;
    private final PaymentEventProducer eventProducer;

    @Override
    @Transactional
    public PaymentResponseDto initiatePayment(PaymentRequestDto request, String userId) {
        Payment payment = Payment.builder()
                .orderId(request.orderId())
                .userId(userId)
                .paymentAmount(request.paymentAmount())
                .status("PENDING")
                .timestamp(Instant.now())
                .build();

        Payment saved = paymentDAO.save(payment);
        processPaymentAsync(saved);
        return paymentMapper.toResponseDto(saved);
    }

    @Async
    public void processPaymentAsync(Payment payment) {
        try {
            boolean isSuccess = externalApiClient.evaluatePayment(payment.getPaymentAmount());
            String newStatus = isSuccess ? "SUCCESS" : "FAILED";
            payment.setStatus(newStatus);
            paymentDAO.save(payment);
            eventProducer.sendPaymentEvent(payment);
        } catch (Exception e) {
            payment.setStatus("FAILED");
            paymentDAO.save(payment);
        }
    }

    @Override
    public PaymentResponseDto getPaymentById(String id) {
        Payment payment = paymentDAO.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + id));
        return paymentMapper.toResponseDto(payment);
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByFilters(String userId, String orderId, String status) {
        if (userId != null && orderId != null && status != null) {
            return paymentDAO.findByUserIdAndStatus(userId, status).stream()
                    .map(paymentMapper::toResponseDto)
                    .collect(Collectors.toList());
        } else if (userId != null && orderId != null) {
            return paymentDAO.findByOrderId(orderId).stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .map(paymentMapper::toResponseDto)
                    .collect(Collectors.toList());
        } else if (userId != null) {
            return paymentDAO.findByUserId(userId).stream()
                    .map(paymentMapper::toResponseDto)
                    .collect(Collectors.toList());
        } else if (orderId != null) {
            return paymentDAO.findByOrderId(orderId).stream()
                    .map(paymentMapper::toResponseDto)
                    .collect(Collectors.toList());
        } else if (status != null) {
            return paymentDAO.findByStatus(status).stream()
                    .map(paymentMapper::toResponseDto)
                    .collect(Collectors.toList());
        } else {
            return paymentDAO.findAll().stream()
                    .map(paymentMapper::toResponseDto)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public BigDecimal getTotalSuccessfulPaymentsForUser(String userId, Instant from, Instant to) {
        Optional<PaymentDAO.AggregationResult> result =
                paymentDAO.getTotalSuccessfulPaymentsForUser(userId, from, to);
        return result.map(PaymentDAO.AggregationResult::total).orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal getTotalSuccessfulPaymentsForAll(Instant from, Instant to) {
        Optional<PaymentDAO.AggregationResult> result =
                paymentDAO.getTotalSuccessfulPaymentsForAll(from, to);
        return result.map(PaymentDAO.AggregationResult::total).orElse(BigDecimal.ZERO);
    }

    @Override
    public boolean isPaymentOwnedByUser(String paymentId, String userId) {
        Payment payment = paymentDAO.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
        return payment.getUserId().equals(userId);
    }
}