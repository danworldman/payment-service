package com.innowise.payment_service.service.impl;

import com.innowise.payment_service.client.ExternalPaymentApiClient;
import com.innowise.payment_service.dao.PaymentDAO;
import com.innowise.payment_service.kafka.PaymentEventProducer;
import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.document.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentProcessor {

    private final ExternalPaymentApiClient externalPaymentApiClient;
    private final PaymentDAO paymentDAO;
    private final PaymentEventProducer paymentEventProducer;

    @Async
    public void processPaymentAsync(Payment payment) {
        try {
            int generatedNumber = externalPaymentApiClient.generateRandomNumber();
            PaymentStatus finalStatus = (generatedNumber % 2 == 0) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
            payment.setStatus(finalStatus);
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
        } finally {
            paymentDAO.save(payment);
            paymentEventProducer.sendPaymentEvent(payment);
        }
    }
}