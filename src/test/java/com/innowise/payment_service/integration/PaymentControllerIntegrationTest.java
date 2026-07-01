package com.innowise.payment_service.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.document.PaymentStatus;
import com.innowise.payment_service.model.dto.PaymentRequestDto;
import com.innowise.payment_service.model.dto.PaymentResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PaymentControllerIntegrationTest extends BaseIntegrationTest {

    private HttpHeaders createAuthHeaders(Long userId, String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + generateTestToken(userId, role));
        return headers;
    }

    @Test
    void createPayment_shouldReturn202Accepted_andTransitionToSuccess_whenExternalApiReturnsEvenNumber() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/api/numbers"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"number\":42}")));

        HttpEntity<PaymentRequestDto> entity = new HttpEntity<>(defaultPaymentRequestDto, createAuthHeaders(DEFAULT_USER_ID, "USER"));

        ResponseEntity<PaymentResponseDto> response = restTemplate.postForEntity(
                baseUrl() + "/api/payments", entity, PaymentResponseDto.class
        );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        PaymentResponseDto body = response.getBody();
        assertNotNull(body);
        assertEquals(PaymentStatus.PENDING, body.status());

        org.awaitility.Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<Payment> list = paymentDAO.findByUserId(DEFAULT_USER_ID);
                    assertFalse(list.isEmpty());
                    assertEquals(PaymentStatus.SUCCESS, list.get(0).getStatus());
                });
    }

    @Test
    void createPayment_shouldReturn202Accepted_andTransitionToFailed_whenExternalApiReturnsOddNumber() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/api/numbers"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"number\":13}")));

        HttpEntity<PaymentRequestDto> entity = new HttpEntity<>(defaultPaymentRequestDto, createAuthHeaders(DEFAULT_USER_ID, "USER"));

        ResponseEntity<PaymentResponseDto> response = restTemplate.postForEntity(
                baseUrl() + "/api/payments", entity, PaymentResponseDto.class
        );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

        org.awaitility.Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<Payment> list = paymentDAO.findByUserId(DEFAULT_USER_ID);
                    assertFalse(list.isEmpty());
                    assertEquals(PaymentStatus.FAILED, list.get(0).getStatus());
                });
    }

    @Test
    void createPayment_shouldReturn202Accepted_andTransitionToFailed_whenExternalApiThrowsError() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/api/numbers"))
                .willReturn(WireMock.aResponse().withStatus(500)));

        HttpEntity<PaymentRequestDto> entity = new HttpEntity<>(defaultPaymentRequestDto, createAuthHeaders(DEFAULT_USER_ID, "USER"));

        ResponseEntity<PaymentResponseDto> response = restTemplate.postForEntity(
                baseUrl() + "/api/payments", entity, PaymentResponseDto.class
        );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

        org.awaitility.Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    List<Payment> list = paymentDAO.findByUserId(DEFAULT_USER_ID);
                    assertFalse(list.isEmpty());
                    assertEquals(PaymentStatus.FAILED, list.get(0).getStatus());
                });
    }

    @Test
    void createPayment_shouldReturn400BadRequest_whenPaymentAmountIsNegative() {
        PaymentRequestDto invalidRequest = new PaymentRequestDto(DEFAULT_ORDER_ID, new BigDecimal("-50.00"));
        HttpEntity<PaymentRequestDto> entity = new HttpEntity<>(invalidRequest, createAuthHeaders(DEFAULT_USER_ID, "USER"));

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            restTemplate.postForEntity(baseUrl() + "/api/payments", entity, PaymentResponseDto.class);
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void getPaymentById_shouldReturnPayment_whenUserIsOwner() {
        Payment initialPayment = Payment.builder()
                .orderId(DEFAULT_ORDER_ID)
                .userId(DEFAULT_USER_ID)
                .status(PaymentStatus.PENDING)
                .paymentAmount(DEFAULT_AMOUNT)
                .build();
        Payment saved = paymentDAO.save(initialPayment);

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(DEFAULT_USER_ID, "USER"));

        ResponseEntity<PaymentResponseDto> response = restTemplate.exchange(
                baseUrl() + "/api/payments/" + saved.getId(), HttpMethod.GET, entity, PaymentResponseDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(saved.getId(), response.getBody().id());
    }

    @Test
    void getPaymentById_shouldReturn404NotFound_whenPaymentDoesNotExist() {
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(DEFAULT_USER_ID, "USER"));

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            restTemplate.exchange(baseUrl() + "/api/payments/" + NON_EXISTENT_ID, HttpMethod.GET, entity, PaymentResponseDto.class);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getResponseBodyAsString().contains("Payment not found"));
    }

    @Test
    void getPaymentById_shouldReturn403Forbidden_whenUserIsNotOwner() {
        Payment foreignPayment = Payment.builder()
                .orderId(DEFAULT_ORDER_ID)
                .userId(999L)
                .status(PaymentStatus.PENDING)
                .paymentAmount(DEFAULT_AMOUNT)
                .build();
        Payment saved = paymentDAO.save(foreignPayment);

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(DEFAULT_USER_ID, "USER"));

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            restTemplate.exchange(baseUrl() + "/api/payments/" + saved.getId(), HttpMethod.GET, entity, PaymentResponseDto.class);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void getPayments_shouldReturnFilteredPayments_forSpecificUser() {
        Long alternativeUserId = 20L;
        Payment p1 = Payment.builder().orderId(10L).userId(DEFAULT_USER_ID).status(PaymentStatus.SUCCESS).build();
        Payment p2 = Payment.builder().orderId(20L).userId(alternativeUserId).status(PaymentStatus.SUCCESS).build();
        paymentDAO.save(p1);
        paymentDAO.save(p2);

        String url = baseUrl() + "/api/payments?userId=" + DEFAULT_USER_ID;
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(DEFAULT_USER_ID, "USER"));

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("\"userId\":" + DEFAULT_USER_ID));
        assertFalse(body.contains("\"userId\":" + alternativeUserId));
    }

    @Test
    void getPayments_shouldReturnAllPayments_whenUserIsAdmin() {
        Long alternativeUserId = 20L;
        Payment p1 = Payment.builder().orderId(10L).userId(DEFAULT_USER_ID).status(PaymentStatus.SUCCESS).build();
        Payment p2 = Payment.builder().orderId(20L).userId(alternativeUserId).status(PaymentStatus.SUCCESS).build();
        paymentDAO.save(p1);
        paymentDAO.save(p2);

        String url = baseUrl() + "/api/payments";
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(DEFAULT_USER_ID, "ADMIN"));

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("\"userId\":" + DEFAULT_USER_ID));
        assertTrue(body.contains("\"userId\":" + alternativeUserId));
    }

    @Test
    void getUserSummary_shouldReturn403Forbidden_whenRequestingOtherUserSummary() {
        Long alternativeUserId = 20L;
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(DEFAULT_USER_ID, "USER"));

        String url = baseUrl() + "/api/payments/users/" + alternativeUserId + "/summary?from=2026-06-01T00:00:00Z&to=2026-07-02T00:00:00Z";
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            restTemplate.exchange(url, HttpMethod.GET, entity, BigDecimal.class);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void getAllSummary_shouldReturn403Forbidden_whenUserIsNotAdmin() {
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(DEFAULT_USER_ID, "USER"));

        String url = baseUrl() + "/api/payments/summary?from=2026-06-01T00:00:00Z&to=2026-07-02T00:00:00Z";
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            restTemplate.exchange(url, HttpMethod.GET, entity, BigDecimal.class);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void createPayment_shouldReturn401_whenTokenHasInvalidSignature() {
        String invalidToken = generateTestToken(DEFAULT_USER_ID, "USER") + "invalid";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken);
        HttpEntity<PaymentRequestDto> entity = new HttpEntity<>(defaultPaymentRequestDto, headers);

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            restTemplate.postForEntity(baseUrl() + "/api/payments", entity, PaymentResponseDto.class);
        });
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void createPayment_shouldReturn403_whenTokenHasNoRole() {
        String tokenWithoutRole = generateTestToken(DEFAULT_USER_ID, null);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutRole);
        HttpEntity<PaymentRequestDto> entity = new HttpEntity<>(defaultPaymentRequestDto, headers);

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () -> {
            restTemplate.postForEntity(baseUrl() + "/api/payments", entity, PaymentResponseDto.class);
        });
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }
}