package com.innowise.payment_service.service.impl;

import com.innowise.payment_service.client.ExternalPaymentApiClient;
import com.innowise.payment_service.dao.PaymentDAO;
import com.innowise.payment_service.kafka.PaymentEventProducer;
import com.innowise.payment_service.model.document.PaymentStatus;
import com.innowise.payment_service.model.event.PaymentCompletedEvent;
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
    public void processPaymentAsync(String paymentId) {
        paymentDAO.findById(paymentId).ifPresent(payment -> {
            try {
                int randomNumber = externalPaymentApiClient.generateRandomNumber();
                PaymentStatus finalStatus = (randomNumber % 2 == 0) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
                payment.setStatus(finalStatus);
            } catch (Exception exception) {
                payment.setStatus(PaymentStatus.FAILED);
            } finally {
                paymentDAO.save(payment);
                PaymentCompletedEvent event = new PaymentCompletedEvent(payment.getOrderId(), payment.getStatus().name());
                paymentEventProducer.sendPaymentEvent(event);
            }
        });
    }
}