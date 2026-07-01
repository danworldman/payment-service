package com.innowise.payment_service.testdata;

import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.document.PaymentStatus;
import com.innowise.payment_service.model.dto.PaymentRequestDto;
import com.innowise.payment_service.model.dto.PaymentResponseDto;

import java.math.BigDecimal;
import java.time.Instant;

public abstract class PaymentTestData {

    public static final Long DEFAULT_USER_ID = 1L;

    public static final String DEFAULT_PAYMENT_ID = "PAY-666-BOB";
    public static final Long DEFAULT_ORDER_ID = 1001L;
    public static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("250.00");
    public static final Instant DEFAULT_TIMESTAMP = Instant.parse("2026-07-01T10:00:00Z");

    public static final String NON_EXISTENT_ID = "PAY-NON-EXISTENT";

    protected final PaymentRequestDto defaultPaymentRequestDto;
    protected final PaymentResponseDto defaultPaymentResponseDto;
    protected final Payment defaultPendingPayment;
    protected final Payment defaultSuccessPayment;
    protected final Payment samplePayment;

    protected PaymentTestData() {
        defaultPaymentRequestDto = new PaymentRequestDto(
                DEFAULT_ORDER_ID,
                DEFAULT_AMOUNT
        );
        defaultPaymentResponseDto = new PaymentResponseDto(
                DEFAULT_PAYMENT_ID,
                DEFAULT_ORDER_ID,
                DEFAULT_USER_ID,
                PaymentStatus.PENDING,
                DEFAULT_TIMESTAMP,
                DEFAULT_AMOUNT
        );
        defaultPendingPayment = Payment.builder()
                .id(DEFAULT_PAYMENT_ID)
                .orderId(DEFAULT_ORDER_ID)
                .userId(DEFAULT_USER_ID)
                .status(PaymentStatus.PENDING)
                .timestamp(DEFAULT_TIMESTAMP)
                .paymentAmount(DEFAULT_AMOUNT)
                .build();
        defaultSuccessPayment = Payment.builder()
                .id(DEFAULT_PAYMENT_ID)
                .orderId(DEFAULT_ORDER_ID)
                .userId(DEFAULT_USER_ID)
                .status(PaymentStatus.SUCCESS)
                .timestamp(DEFAULT_TIMESTAMP)
                .paymentAmount(DEFAULT_AMOUNT)
                .build();
        samplePayment = defaultPendingPayment;
    }
}