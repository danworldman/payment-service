package com.innowise.payment_service.client;

import com.innowise.payment_service.exception.PaymentProcessingException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;

@Component
public class ExternalPaymentApiClient {

    private final RestClient restClient;

    @Value("${external.api.url}")
    private String externalApiUrl;

    public ExternalPaymentApiClient(@Qualifier("cleanRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public int generateRandomNumber() {
        try {
            Map<?, ?> apiResponse = restClient.get()
                    .uri(externalApiUrl)
                    .retrieve()
                    .body(Map.class);
            if (apiResponse != null && apiResponse.containsKey("number")) {
                return ((Number) apiResponse.get("number")).intValue();
            }
            throw new PaymentProcessingException("Invalid external API response structure");
        } catch (PaymentProcessingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PaymentProcessingException("External API unavailable", exception);
        }
    }
}