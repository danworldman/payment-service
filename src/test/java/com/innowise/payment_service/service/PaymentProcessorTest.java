package com.innowise.payment_service.service;

import com.innowise.payment_service.client.ExternalPaymentApiClient;
import com.innowise.payment_service.dao.PaymentDAO;
import com.innowise.payment_service.kafka.PaymentEventProducer;
import com.innowise.payment_service.model.document.PaymentStatus;
import com.innowise.payment_service.model.event.PaymentCompletedEvent;
import com.innowise.payment_service.service.impl.PaymentProcessor;
import com.innowise.payment_service.testdata.PaymentTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        defaultPendingPayment.setStatus(PaymentStatus.PENDING);
        when(paymentDAO.findById(DEFAULT_PAYMENT_ID)).thenReturn(Optional.of(defaultPendingPayment));
        when(externalPaymentApiClient.generateRandomNumber()).thenReturn(EVEN_NUMBER);

        paymentProcessor.processPaymentAsync(DEFAULT_PAYMENT_ID);

        assertThat(defaultPendingPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentDAO).save(defaultPendingPayment);
        verify(paymentEventProducer).sendPaymentEvent(any(PaymentCompletedEvent.class));
    }

    @Test
    void processPaymentAsync_shouldSetFailed_whenGeneratedNumberIsOdd() {
        defaultPendingPayment.setStatus(PaymentStatus.PENDING);
        when(paymentDAO.findById(DEFAULT_PAYMENT_ID)).thenReturn(Optional.of(defaultPendingPayment));
        when(externalPaymentApiClient.generateRandomNumber()).thenReturn(ODD_NUMBER);

        paymentProcessor.processPaymentAsync(DEFAULT_PAYMENT_ID);

        assertThat(defaultPendingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentDAO).save(defaultPendingPayment);
        verify(paymentEventProducer).sendPaymentEvent(any(PaymentCompletedEvent.class));
    }

    @Test
    void processPaymentAsync_shouldSetFailed_whenClientThrowsException() {
        defaultPendingPayment.setStatus(PaymentStatus.PENDING);
        when(paymentDAO.findById(DEFAULT_PAYMENT_ID)).thenReturn(Optional.of(defaultPendingPayment));
        when(externalPaymentApiClient.generateRandomNumber()).thenThrow(new RuntimeException("API error"));

        paymentProcessor.processPaymentAsync(DEFAULT_PAYMENT_ID);

        assertThat(defaultPendingPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(paymentDAO).save(defaultPendingPayment);
        verify(paymentEventProducer).sendPaymentEvent(any(PaymentCompletedEvent.class));
    }
}