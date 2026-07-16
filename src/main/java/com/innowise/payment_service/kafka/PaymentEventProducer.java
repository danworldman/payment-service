package com.innowise.payment_service.kafka;

import com.innowise.payment_service.model.event.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private static final String TOPIC = "payment-events";
    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;

    public void sendPaymentEvent(PaymentCompletedEvent event) {
        CompletableFuture<SendResult<String, PaymentCompletedEvent>> future =
                kafkaTemplate.send(TOPIC, String.valueOf(event.orderId()), event);

        future.whenComplete((result, exception) -> {
            if (exception != null) {
                return;
            }
        });
    }
}