package com.innowise.payment_service.testdata;

import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.document.PaymentStatus;
import com.innowise.payment_service.model.dto.PaymentRequestDto;
import com.innowise.payment_service.model.dto.PaymentResponseDto;

import java.math.BigDecimal;
import java.time.Instant;

public abstract class PaymentTestData {

    public static final Long DEFAULT_USER_ID = 1L;
    public static final Long OTHER_USER_ID = 1000L;
    public static final String DEFAULT_PAYMENT_ID = "PAY-BOB-001";
    public static final String ANOTHER_PAYMENT_ID = "PAY-BOB-002";
    public static final Long DEFAULT_ORDER_ID = 100L;
    public static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("250.00");
    public static final BigDecimal NEGATIVE_AMOUNT = new BigDecimal("-50.00");
    public static final BigDecimal TOTAL_AMOUNT = new BigDecimal("1000.00");
    public static final Instant DEFAULT_TIMESTAMP = Instant.parse("2026-07-01T10:00:00Z");
    public static final Instant SUMMARY_FROM = Instant.parse("2026-06-01T00:00:00Z");
    public static final Instant SUMMARY_TO = Instant.parse("2026-07-02T00:00:00Z");

    public static final String NON_EXISTENT_ID = "PAY-NON-EXISTENT";
    public static final String INVALID_ID = "INVALID";
    public static final String USER_ROLE = "USER";
    public static final String ADMIN_ROLE = "ADMIN";

    public static final int EVEN_NUMBER = 2;
    public static final int ODD_NUMBER = 1;

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