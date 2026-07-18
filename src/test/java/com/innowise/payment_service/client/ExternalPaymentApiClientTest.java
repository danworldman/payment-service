package com.innowise.payment_service.client;

import com.innowise.payment_service.exception.PaymentProcessingException;
import com.innowise.payment_service.testdata.PaymentTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalPaymentApiClientTest extends PaymentTestData {

    private static final String INVALID_API_RESPONSE_MSG = "Invalid external API response structure";
    private static final String API_UNAVAILABLE_MSG = "External API unavailable";

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private ExternalPaymentApiClient client;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(client, "externalApiUrl", "http://example.com");
    }

    @Test
    void generateRandomNumber_shouldReturnNumber_whenApiResponseContainsNumber() {
        Map<String, Object> response = Map.of("number", 1);
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(response);

        int result = client.generateRandomNumber();

        assertThat(result).isEqualTo(1);
    }

    @Test
    void generateRandomNumber_shouldThrowPaymentProcessingException_whenResponseMissingNumber() {
        Map<String, Object> response = Map.of();
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(response);

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class,
                () -> client.generateRandomNumber());

        assertThat(exception.getMessage()).isEqualTo(INVALID_API_RESPONSE_MSG);
    }

    @Test
    void generateRandomNumber_shouldThrowPaymentProcessingException_whenRestClientThrowsException() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenThrow(new RuntimeException("Network error"));

        PaymentProcessingException exception = assertThrows(PaymentProcessingException.class,
                () -> client.generateRandomNumber());

        assertThat(exception.getMessage()).isEqualTo(API_UNAVAILABLE_MSG);
        assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
    }
}