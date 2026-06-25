package com.innowise.payment_service.kafka;

import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;
    private static final String TOPIC = "payment-events";

    public void sendPaymentEvent(Payment payment) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(payment.getOrderId(), payment.getStatus().name());
        kafkaTemplate.send(TOPIC, String.valueOf(payment.getOrderId()), event);
    }
}