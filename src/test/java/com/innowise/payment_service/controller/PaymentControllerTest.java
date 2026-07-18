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

    @Test
    void createPayment_shouldReturn202Accepted_whenTokenIsValid() throws Exception {
        prepareJwt(DEFAULT_USER_ID, USER_ROLE);
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
        prepareJwt(DEFAULT_USER_ID, USER_ROLE);
        PaymentRequestDto invalidRequest = new PaymentRequestDto(null, NEGATIVE_AMOUNT);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_shouldThrowPaymentProcessingException_whenUserIdClaimMissing() throws Exception {
        stubJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("role", "USER") // нет user_id
                .build();

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defaultPaymentRequestDto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Missing user identity in token"));
    }

    @Test
    void getPaymentById_shouldReturnPayment_whenUserOwnsPayment() throws Exception {
        prepareJwt(DEFAULT_USER_ID, USER_ROLE);
        when(paymentService.getPaymentById(DEFAULT_PAYMENT_ID)).thenReturn(defaultPaymentResponseDto);

        mockMvc.perform(get("/api/payments/{id}", DEFAULT_PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(DEFAULT_USER_ID));
    }

    @Test
    void getPaymentById_shouldReturnForbidden_whenUserAccessesOtherPayment() throws Exception {
        prepareJwt(DEFAULT_USER_ID, USER_ROLE);
        PaymentResponseDto otherPayment = new PaymentResponseDto(
                DEFAULT_PAYMENT_ID, DEFAULT_ORDER_ID, OTHER_USER_ID,
                PaymentStatus.PENDING, DEFAULT_TIMESTAMP, DEFAULT_AMOUNT
        );
        when(paymentService.getPaymentById(DEFAULT_PAYMENT_ID)).thenReturn(otherPayment);

        mockMvc.perform(get("/api/payments/{id}", DEFAULT_PAYMENT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPaymentById_shouldReturnPayment_whenAdminAccessesAny() throws Exception {
        prepareJwt(OTHER_USER_ID, ADMIN_ROLE);
        PaymentResponseDto otherPayment = new PaymentResponseDto(
                DEFAULT_PAYMENT_ID, DEFAULT_ORDER_ID, OTHER_USER_ID,
                PaymentStatus.PENDING, DEFAULT_TIMESTAMP, DEFAULT_AMOUNT
        );
        when(paymentService.getPaymentById(DEFAULT_PAYMENT_ID)).thenReturn(otherPayment);

        mockMvc.perform(get("/api/payments/{id}", DEFAULT_PAYMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(OTHER_USER_ID));
    }

    @Test
    void getPayments_shouldUseAuthenticatedUserId_whenUserRole() throws Exception {
        prepareJwt(DEFAULT_USER_ID, USER_ROLE);
        when(paymentService.getPaymentsByFilters(eq(DEFAULT_USER_ID), eq(null), eq(null)))
                .thenReturn(List.of(defaultPaymentResponseDto));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(DEFAULT_PAYMENT_ID));
    }

    @Test
    void getPayments_shouldUseProvidedUserId_whenAdminRole() throws Exception {
        prepareJwt(DEFAULT_USER_ID, ADMIN_ROLE);
        when(paymentService.getPaymentsByFilters(eq(OTHER_USER_ID), eq(null), eq(null)))
                .thenReturn(List.of(defaultPaymentResponseDto));

        mockMvc.perform(get("/api/payments")
                        .param("userId", OTHER_USER_ID.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void getUserSummary_shouldReturnTotal_whenUserAccessesOwnSummary() throws Exception {
        prepareJwt(DEFAULT_USER_ID, USER_ROLE);
        when(paymentService.getTotalSuccessfulPaymentsForUser(eq(DEFAULT_USER_ID), eq(SUMMARY_FROM), eq(SUMMARY_TO)))
                .thenReturn(TOTAL_AMOUNT);

        mockMvc.perform(get("/api/payments/users/{userId}/summary", DEFAULT_USER_ID)
                        .param("from", SUMMARY_FROM.toString())
                        .param("to", SUMMARY_TO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(TOTAL_AMOUNT.doubleValue()));
    }

    @Test
    void getUserSummary_shouldReturnForbidden_whenUserAccessesOtherSummary() throws Exception {
        prepareJwt(DEFAULT_USER_ID, USER_ROLE);
        mockMvc.perform(get("/api/payments/users/{userId}/summary", OTHER_USER_ID)
                        .param("from", SUMMARY_FROM.toString())
                        .param("to", SUMMARY_TO.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllSummary_shouldReturnTotal_whenAdminRole() throws Exception {
        prepareJwt(DEFAULT_USER_ID, ADMIN_ROLE);
        when(paymentService.getTotalSuccessfulPaymentsForAll(eq(SUMMARY_FROM), eq(SUMMARY_TO)))
                .thenReturn(TOTAL_AMOUNT);

        mockMvc.perform(get("/api/payments/summary")
                        .param("from", SUMMARY_FROM.toString())
                        .param("to", SUMMARY_TO.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(TOTAL_AMOUNT.doubleValue()));
    }

    private void prepareJwt(Long userId, String role) {
        stubJwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("user_id", userId)
                .claim("role", role)
                .build();
    }
}