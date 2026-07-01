package com.innowise.payment_service.service;


import com.innowise.payment_service.client.ExternalPaymentApiClient;
import com.innowise.payment_service.dao.PaymentDAO;
import com.innowise.payment_service.kafka.PaymentEventProducer;
import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.document.PaymentStatus;
import com.innowise.payment_service.service.impl.PaymentProcessor;
import com.innowise.payment_service.testdata.PaymentTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentProcessorTest extends PaymentTestData {

    private ExternalPaymentApiClient externalPaymentApiClient;
    private PaymentDAO paymentDAO;
    private PaymentEventProducer paymentEventProducer;
    private PaymentProcessor paymentProcessor;

    @BeforeEach
    void setUp() {
        externalPaymentApiClient = Mockito.mock(ExternalPaymentApiClient.class);
        paymentDAO = Mockito.mock(PaymentDAO.class);
        paymentEventProducer = Mockito.mock(PaymentEventProducer.class);
        paymentProcessor = new PaymentProcessor(externalPaymentApiClient, paymentDAO, paymentEventProducer);
    }

    @Test
    void processPaymentAsync_shouldSetSuccess_whenGeneratedNumberIsEven() {
        Payment payment = Payment.builder()
                .id(DEFAULT_PAYMENT_ID)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentDAO.findById(DEFAULT_PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(externalPaymentApiClient.generateRandomNumber()).thenReturn(42);

        paymentProcessor.processPaymentAsync(DEFAULT_PAYMENT_ID);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentDAO).save(payment);
        verify(paymentEventProducer).sendPaymentEvent(payment);
    }

    @Test
    void processPaymentAsync_shouldSetFailed_whenGeneratedNumberIsOdd() {
        Payment payment = Payment.builder()
                .id(DEFAULT_PAYMENT_ID)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentDAO.findById(DEFAULT_PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(externalPaymentApiClient.generateRandomNumber()).thenReturn(41);

        paymentProcessor.processPaymentAsync(DEFAULT_PAYMENT_ID);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentDAO).save(payment);
        verify(paymentEventProducer).sendPaymentEvent(payment);
    }

    @Test
    void processPaymentAsync_shouldSetFailed_whenClientThrowsException() {
        Payment payment = Payment.builder()
                .id(DEFAULT_PAYMENT_ID)
                .status(PaymentStatus.PENDING)
                .build();
        when(paymentDAO.findById(DEFAULT_PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(externalPaymentApiClient.generateRandomNumber()).thenThrow(new RuntimeException("API error"));

        paymentProcessor.processPaymentAsync(DEFAULT_PAYMENT_ID);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentDAO).save(payment);
        verify(paymentEventProducer).sendPaymentEvent(payment);
    }
}