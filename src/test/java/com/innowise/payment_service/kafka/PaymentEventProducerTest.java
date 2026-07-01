package com.innowise.payment_service.kafka;

import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.document.PaymentStatus;
import com.innowise.payment_service.model.event.PaymentCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentEventProducerTest {

    private KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;
    private PaymentEventProducer producer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        producer = new PaymentEventProducer(kafkaTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void sendPaymentEvent_shouldCallKafkaTemplate() {
        Payment payment = Payment.builder()
                .id("PAY-1")
                .orderId(100L)
                .userId(1L)
                .status(PaymentStatus.SUCCESS)
                .timestamp(Instant.now())
                .paymentAmount(BigDecimal.TEN)
                .build();

        when(kafkaTemplate.send(any(String.class), any(String.class), any(PaymentCompletedEvent.class)))
                .thenReturn(new CompletableFuture<>());

        producer.sendPaymentEvent(payment);

        verify(kafkaTemplate).send(eq("payment-events"), eq("100"), any(PaymentCompletedEvent.class));
    }
}
