package com.innowise.payment_service.kafka;

import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.event.PaymentCompletedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;
    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";

    public PaymentEventProducer(KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendPaymentEvent(Payment payment) {
        PaymentCompletedEvent paymentCompletedEvent = new PaymentCompletedEvent(
                payment.getOrderId(),
                payment.getStatus().name()
        );
        kafkaTemplate.send(
                PAYMENT_EVENTS_TOPIC,
                String.valueOf(payment.getOrderId()),
                paymentCompletedEvent
        );
    }
}