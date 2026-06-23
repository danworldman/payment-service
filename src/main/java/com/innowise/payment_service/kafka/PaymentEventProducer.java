package com.innowise.payment_service.kafka;

import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    private static final String TOPIC = "CREATE_PAYMENT";

    public void sendPaymentEvent(Payment payment) {
        PaymentEvent event = new PaymentEvent(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getStatus(),
                payment.getPaymentAmount(),
                payment.getTimestamp()
        );
        kafkaTemplate.send(TOPIC, event);
    }
}