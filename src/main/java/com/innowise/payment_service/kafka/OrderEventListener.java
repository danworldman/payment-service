package com.innowise.payment_service.kafka;

import com.innowise.payment_service.model.dto.PaymentRequestDto;
import com.innowise.payment_service.model.event.OrderCreatedEvent;
import com.innowise.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order-events", groupId = "payment-service")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received CREATE_ORDER event for orderId={}", event.orderId());
        PaymentRequestDto requestDto = new PaymentRequestDto(event.orderId(), event.totalPrice());
        paymentService.initiatePayment(requestDto, event.userId());
    }
}