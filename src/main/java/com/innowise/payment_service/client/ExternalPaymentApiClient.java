package com.innowise.payment_service.client;

import com.innowise.payment_service.exception.PaymentProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExternalPaymentApiClient {

    private final RestClient restClient;

    @Value("${external.api.url}")
    private String apiUrl;

    public int generateRandomNumber() {
        try {
            Map<?, ?> response = restClient.get()
                    .uri(apiUrl)
                    .retrieve()
                    .body(Map.class);
            if (response != null && response.containsKey("number")) {
                return ((Number) response.get("number")).intValue();
            }
            throw new PaymentProcessingException("Invalid external API response structure");
        } catch (Exception e) {
            throw new PaymentProcessingException("External API unavailable", e);
        }
    }
}