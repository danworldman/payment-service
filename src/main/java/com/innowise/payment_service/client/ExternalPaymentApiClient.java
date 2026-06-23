package com.innowise.payment_service.client;

import com.innowise.payment_service.exception.PaymentProcessingException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ExternalPaymentApiClient {

    private final WebClient webClient;

    @Value("${external.api.url:http://localhost:8081/evaluate}")
    private String apiUrl;

    @CircuitBreaker(name = "externalApi", fallbackMethod = "fallbackEvaluate")
    public boolean evaluatePayment(BigDecimal amount) {
        try {
            Boolean result = webClient.get()
                    .uri(apiUrl + "?amount=" + amount)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
            return Boolean.TRUE.equals(result);
        } catch (WebClientResponseException e) {
            throw new PaymentProcessingException("External API error: " + e.getStatusCode());
        } catch (Exception e) {
            throw new PaymentProcessingException("External API unavailable", e);
        }
    }

    private boolean fallbackEvaluate(BigDecimal amount, Throwable t) {
        return false;
    }
}