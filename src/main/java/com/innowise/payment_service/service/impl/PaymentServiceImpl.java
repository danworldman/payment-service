package com.innowise.payment_service.service.impl;

import com.innowise.payment_service.client.ExternalPaymentApiClient;
import com.innowise.payment_service.dao.PaymentDAO;
import com.innowise.payment_service.exception.PaymentAlreadyProcessedException;
import com.innowise.payment_service.kafka.PaymentEventProducer;
import com.innowise.payment_service.mapper.PaymentMapper;
import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.dto.PaymentRequestDto;
import com.innowise.payment_service.model.dto.PaymentResponseDto;
import com.innowise.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    public PaymentResponseDto createPayment(PaymentRequestDto dto) {
        validateOrderId(dto.orderId());
        validateUserId(dto.userId());

        paymentDAO.findByOrderIdAndUserId(dto.orderId(), dto.userId())
                .ifPresent(p -> {
                    throw new PaymentAlreadyProcessedException("Payment already exists for order: " + dto.orderId());
                });

        Payment payment = paymentMapper.toEntity(dto);
        payment.setTimestamp(LocalDateTime.now());

        boolean isSuccess = externalApiClient.evaluatePayment(dto.paymentAmount());
        payment.setStatus(isSuccess ? "SUCCESS" : "FAILED");

        Payment saved = paymentDAO.save(payment);
        eventProducer.sendPaymentEvent(saved);

        return paymentMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsByUserId(String userId) {
        validateUserId(userId);
        return paymentDAO.findByUserId(userId).stream()
                .map(paymentMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsByOrderId(String orderId) {
        validateOrderId(orderId);
        return paymentDAO.findByOrderId(orderId).stream()
                .map(paymentMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsByStatus(String status) {
        return paymentDAO.findByStatus(status).stream()
                .map(paymentMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalSumForUser(String userId, LocalDateTime from, LocalDateTime to) {
        validateUserId(userId);
        return paymentDAO.findPaymentsByUserIdAndDateRange(userId, from, to).stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalSumForAll(LocalDateTime from, LocalDateTime to) {
        return paymentDAO.findPaymentsByDateRange(from, to).stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
    }

    private void validateOrderId(String orderId) {
        if (!StringUtils.hasText(orderId)) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
    }
}