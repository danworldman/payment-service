package com.innowise.payment_service.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.document.PaymentStatus;
import com.innowise.payment_service.model.dto.PaymentRequestDto;
import com.innowise.payment_service.model.dto.PaymentResponseDto;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentControllerIntegrationTest extends BaseIntegrationTest {

    private HttpHeaders createAuthHeaders(String role) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + generateTestToken(DEFAULT_USER_ID, role));

        return headers;
    }

    @Test
    void createPayment_shouldReturn202Accepted_andTransitionToSuccess_whenExternalApiReturnsEvenNumber() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/api/numbers"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"number\":" + EVEN_NUMBER + "}")));

        HttpEntity<PaymentRequestDto> entity = new HttpEntity<>(defaultPaymentRequestDto, createAuthHeaders(USER_ROLE));

        ResponseEntity<PaymentResponseDto> response = restTemplate.postForEntity(
                baseUrl() + "/api/payments", entity, PaymentResponseDto.class
        );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        PaymentResponseDto body = response.getBody();
        assertNotNull(body);
        assertEquals(PaymentStatus.PENDING, body.status());

        Awaitility.await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    List<Payment> list = paymentDAO.findByUserId(DEFAULT_USER_ID);
                    assertFalse(list.isEmpty());
                    assertEquals(PaymentStatus.SUCCESS, list.getFirst().getStatus());
                });
    }

    @Test
    void createPayment_shouldReturn202Accepted_andTransitionToFailed_whenExternalApiReturnsOddNumber() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/api/numbers"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"number\":" + ODD_NUMBER + "}")));

        HttpEntity<PaymentRequestDto> entity = new HttpEntity<>(defaultPaymentRequestDto, createAuthHeaders(USER_ROLE));

        ResponseEntity<PaymentResponseDto> response = restTemplate.postForEntity(
                baseUrl() + "/api/payments", entity, PaymentResponseDto.class
        );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

        Awaitility.await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    List<Payment> list = paymentDAO.findByUserId(DEFAULT_USER_ID);
                    assertFalse(list.isEmpty());
                    assertEquals(PaymentStatus.FAILED, list.getFirst().getStatus());
                });
    }

    @Test
    void createPayment_shouldReturn202Accepted_andTransitionToFailed_whenExternalApiThrowsError() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/api/numbers"))
                .willReturn(WireMock.aResponse().withStatus(500)));

        HttpEntity<PaymentRequestDto> entity = new HttpEntity<>(defaultPaymentRequestDto, createAuthHeaders(USER_ROLE));

        ResponseEntity<PaymentResponseDto> response = restTemplate.postForEntity(
                baseUrl() + "/api/payments", entity, PaymentResponseDto.class
        );

        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());

        Awaitility.await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    List<Payment> list = paymentDAO.findByUserId(DEFAULT_USER_ID);
                    assertFalse(list.isEmpty());
                    assertEquals(PaymentStatus.FAILED, list.getFirst().getStatus());
                });
    }

    @Test
    void createPayment_shouldReturn400BadRequest_whenPaymentAmountIsNegative() {
        PaymentRequestDto invalidRequest = new PaymentRequestDto(DEFAULT_ORDER_ID, NEGATIVE_AMOUNT);
        HttpEntity<PaymentRequestDto> entity = new HttpEntity<>(invalidRequest, createAuthHeaders(USER_ROLE));

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/api/payments", entity, String.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
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

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(USER_ROLE));
        String url = baseUrl() + "/api/payments/" + saved.getId();

        ResponseEntity<PaymentResponseDto> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, PaymentResponseDto.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(saved.getId(), response.getBody().id());
    }

    @Test
    void getPaymentById_shouldReturn404NotFound_whenPaymentDoesNotExist() {
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(USER_ROLE));
        String url = baseUrl() + "/api/payments/" + NON_EXISTENT_ID;

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Payment not found"));
    }

    @Test
    void getPaymentById_shouldReturn403Forbidden_whenUserIsNotOwner() {
        Payment foreignPayment = Payment.builder()
                .orderId(DEFAULT_ORDER_ID)
                .userId(OTHER_USER_ID)
                .status(PaymentStatus.PENDING)
                .paymentAmount(DEFAULT_AMOUNT)
                .build();
        Payment saved = paymentDAO.save(foreignPayment);

        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(USER_ROLE));
        String url = baseUrl() + "/api/payments/" + saved.getId();

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getPayments_shouldReturnFilteredPayments_forSpecificUser() {
        Long alternativeUserId = 2L;
        Payment payment1 = Payment.builder()
                .orderId(10L)
                .userId(DEFAULT_USER_ID)
                .status(PaymentStatus.SUCCESS)
                .paymentAmount(DEFAULT_AMOUNT)
                .build();
        Payment payment2 = Payment.builder()
                .orderId(2L)
                .userId(alternativeUserId)
                .status(PaymentStatus.SUCCESS)
                .paymentAmount(DEFAULT_AMOUNT)
                .build();
        paymentDAO.save(payment1);
        paymentDAO.save(payment2);

        String url = baseUrl() + "/api/payments?userId=" + DEFAULT_USER_ID;
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(USER_ROLE));

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("\"userId\":" + DEFAULT_USER_ID));
        assertFalse(body.contains("\"userId\":" + alternativeUserId));
    }

    @Test
    void getUserSummary_shouldReturn403Forbidden_whenRequestingOtherUserSummary() {
        Long alternativeUserId = 2L;
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(USER_ROLE));
        String url = baseUrl() + "/api/payments/users/" + alternativeUserId + "/summary?from=" + SUMMARY_FROM + "&to=" + SUMMARY_TO;

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getAllSummary_shouldReturn403Forbidden_whenUserIsNotAdmin() {
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(USER_ROLE));
        String url = baseUrl() + "/api/payments/summary?from=" + SUMMARY_FROM + "&to=" + SUMMARY_TO;

        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void createPayment_shouldReturn401_whenTokenHasInvalidSignature() {
        String invalidToken = generateTestToken(DEFAULT_USER_ID, USER_ROLE) + "invalid";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken);
        HttpEntity<PaymentRequestDto> entity = new HttpEntity<>(defaultPaymentRequestDto, headers);

        ResponseEntity<PaymentResponseDto> response = restTemplate.postForEntity(
                baseUrl() + "/api/payments", entity, PaymentResponseDto.class
        );
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void createPayment_shouldReturn403_whenTokenHasNoRole() {
        String tokenWithoutRole = generateTestToken(DEFAULT_USER_ID, "");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenWithoutRole);
        HttpEntity<PaymentRequestDto> entity = new HttpEntity<>(defaultPaymentRequestDto, headers);

        ResponseEntity<PaymentResponseDto> response = restTemplate.postForEntity(
                baseUrl() + "/api/payments", entity, PaymentResponseDto.class
        );
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}