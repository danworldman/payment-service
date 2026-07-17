package com.innowise.payment_service.kafka;

import com.innowise.payment_service.model.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final String TOPIC = "payment-events";
    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;

    public void sendPaymentEvent(PaymentCompletedEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.orderId()), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error("Failed to send PAYMENT_COMPLETED event for orderId={}", event.orderId(), exception);
                    }
                });
    }
}