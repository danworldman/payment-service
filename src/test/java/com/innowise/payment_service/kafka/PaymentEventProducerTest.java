package com.innowise.payment_service.kafka;

import com.innowise.payment_service.model.event.PaymentCompletedEvent;
import com.innowise.payment_service.testdata.PaymentTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentEventProducerTest extends PaymentTestData {

    private static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    private KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;
    private PaymentEventProducer producer;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        producer = new PaymentEventProducer(kafkaTemplate);
    }

    @Test
    void sendPaymentEvent_shouldCallKafkaTemplate() {
        PaymentCompletedEvent testEvent = new PaymentCompletedEvent(DEFAULT_ORDER_ID, "SUCCESS");

        when(kafkaTemplate.send(any(String.class), any(String.class), any(PaymentCompletedEvent.class)))
                .thenReturn(new CompletableFuture<>());

        producer.sendPaymentEvent(testEvent);

        verify(kafkaTemplate).send(
                eq(PAYMENT_EVENTS_TOPIC),
                eq(String.valueOf(DEFAULT_ORDER_ID)),
                eq(testEvent)
        );
    }
}