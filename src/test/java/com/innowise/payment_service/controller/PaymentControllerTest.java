package com.innowise.payment_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.innowise.payment_service.exception.GlobalExceptionHandler;
import com.innowise.payment_service.model.document.PaymentStatus;
import com.innowise.payment_service.model.dto.PaymentRequestDto;
import com.innowise.payment_service.model.dto.PaymentResponseDto;
import com.innowise.payment_service.service.PaymentService;
import com.innowise.payment_service.testdata.PaymentTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest extends PaymentTestData {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Jwt stubJwt;

    @Mock
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        PaymentController controller = new PaymentController(paymentService);

        HandlerMethodArgumentResolver jwtArgumentResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return stubJwt;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(jwtArgumentResolver)
                .build();
    }

    private void prepareJwt(Long userId, String role) {
        stubJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("user_id", userId)
                .claim("role", role)
                .build();
    }

    @Test
    void createPayment_shouldReturn202Accepted_whenTokenIsValid() throws Exception {
        prepareJwt(DEFAULT_USER_ID, "USER");
        when(paymentService.initiatePayment(any(PaymentRequestDto.class), eq(DEFAULT_USER_ID)))
                .thenReturn(defaultPaymentResponseDto);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultPaymentRequestDto)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(DEFAULT_PAYMENT_ID));
    }

    @Test
    void createPayment_shouldReturnBadRequest_whenValidationFails() throws Exception {
        prepareJwt(DEFAULT_USER_ID, "USER");
        PaymentRequestDto invalidRequest = new PaymentRequestDto(null, new BigDecimal("-10.00"));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_shouldThrowPaymentProcessingException_whenUserIdClaimMissing() throws Exception {
        stubJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("role", "USER")
                .build();

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultPaymentRequestDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected internal server error occurred: Missing user identity in token"));
    }

    @Test
    void getPaymentById_shouldReturnPayment_whenUserOwnsPayment() throws Exception {
        prepareJwt(DEFAULT_USER_ID, "USER");
        when(paymentService.getPaymentById(DEFAULT_PAYMENT_ID)).thenReturn(defaultPaymentResponseDto);

        mockMvc.perform(get("/api/payments/{id}", DEFAULT_PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID));
    }

    @Test
    void getPaymentById_shouldReturnForbidden_whenUserAccessesOtherPayment() throws Exception {
        prepareJwt(DEFAULT_USER_ID, "USER");
        PaymentResponseDto otherPayment = new PaymentResponseDto(
                DEFAULT_PAYMENT_ID, DEFAULT_ORDER_ID, 999L,
                PaymentStatus.PENDING, DEFAULT_TIMESTAMP, DEFAULT_AMOUNT
        );
        when(paymentService.getPaymentById(DEFAULT_PAYMENT_ID)).thenReturn(otherPayment);

        mockMvc.perform(get("/api/payments/{id}", DEFAULT_PAYMENT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPaymentById_shouldReturnPayment_whenAdminAccessesAny() throws Exception {
        prepareJwt(999L, "ADMIN");
        PaymentResponseDto otherPayment = new PaymentResponseDto(
                DEFAULT_PAYMENT_ID, DEFAULT_ORDER_ID, 999L,
                PaymentStatus.PENDING, DEFAULT_TIMESTAMP, DEFAULT_AMOUNT
        );
        when(paymentService.getPaymentById(DEFAULT_PAYMENT_ID)).thenReturn(otherPayment);

        mockMvc.perform(get("/api/payments/{id}", DEFAULT_PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(999L));
    }

    @Test
    void getPayments_shouldUseAuthenticatedUserId_whenUserRole() throws Exception {
        prepareJwt(DEFAULT_USER_ID, "USER");
        when(paymentService.getPaymentsByFilters(eq(DEFAULT_USER_ID), eq(null), eq(null)))
                .thenReturn(List.of(defaultPaymentResponseDto));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(DEFAULT_PAYMENT_ID));
    }

    @Test
    void getPayments_shouldUseProvidedUserId_whenAdminRole() throws Exception {
        prepareJwt(DEFAULT_USER_ID, "ADMIN");
        Long targetUserId = 42L;
        when(paymentService.getPaymentsByFilters(eq(targetUserId), eq(null), eq(null)))
                .thenReturn(List.of(defaultPaymentResponseDto));

        mockMvc.perform(get("/api/payments")
                        .param("userId", targetUserId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void getUserSummary_shouldReturnTotal_whenUserAccessesOwnSummary() throws Exception {
        prepareJwt(DEFAULT_USER_ID, "USER");
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T23:59:59Z");
        BigDecimal total = new BigDecimal("500.00");
        when(paymentService.getTotalSuccessfulPaymentsForUser(eq(DEFAULT_USER_ID), eq(from), eq(to)))
                .thenReturn(total);

        mockMvc.perform(get("/api/payments/users/{userId}/summary", DEFAULT_USER_ID)
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(total.doubleValue()));
    }

    @Test
    void getUserSummary_shouldReturnForbidden_whenUserAccessesOtherSummary() throws Exception {
        prepareJwt(DEFAULT_USER_ID, "USER");
        Long otherUserId = 999L;
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T23:59:59Z");

        mockMvc.perform(get("/api/payments/users/{userId}/summary", otherUserId)
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllSummary_shouldReturnTotal_whenAdminRole() throws Exception {
        prepareJwt(DEFAULT_USER_ID, "ADMIN");
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T23:59:59Z");
        BigDecimal total = new BigDecimal("1000.00");
        when(paymentService.getTotalSuccessfulPaymentsForAll(eq(from), eq(to)))
                .thenReturn(total);

        mockMvc.perform(get("/api/payments/summary")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(total.doubleValue()));
    }
}
