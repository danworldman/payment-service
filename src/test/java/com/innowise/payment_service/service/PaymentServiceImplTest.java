package com.innowise.payment_service.service;

import com.innowise.payment_service.dao.PaymentDAO;
import com.innowise.payment_service.exception.PaymentNotFoundException;
import com.innowise.payment_service.mapper.PaymentMapper;
import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.document.PaymentStatus;
import com.innowise.payment_service.model.dto.PaymentResponseDto;
import com.innowise.payment_service.service.impl.PaymentProcessor;
import com.innowise.payment_service.service.impl.PaymentServiceImpl;
import com.innowise.payment_service.testdata.PaymentTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceImplTest extends PaymentTestData {

    private PaymentDAO paymentDAO;
    private PaymentMapper paymentMapper;
    private PaymentProcessor paymentProcessor;
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentDAO = Mockito.mock(PaymentDAO.class);
        paymentMapper = Mockito.mock(PaymentMapper.class);
        paymentProcessor = Mockito.mock(PaymentProcessor.class);
        paymentService = new PaymentServiceImpl(paymentDAO, paymentMapper, paymentProcessor);
    }

    @Test
    void initiatePayment_shouldReturnPaymentResponse_whenValid() {
        when(paymentMapper.toEntity(any(), any())).thenReturn(defaultPendingPayment);
        when(paymentDAO.save(any(Payment.class))).thenReturn(defaultPendingPayment);
        when(paymentMapper.toResponseDto(defaultPendingPayment)).thenReturn(defaultPaymentResponseDto);

        PaymentResponseDto result = paymentService.initiatePayment(defaultPaymentRequestDto, DEFAULT_USER_ID);

        assertThat(result).isNotNull();
        verify(paymentMapper).toEntity(eq(defaultPaymentRequestDto), eq(DEFAULT_USER_ID));
        verify(paymentDAO).save(any(Payment.class));
        verify(paymentProcessor).processPaymentAsync(DEFAULT_PAYMENT_ID);
    }

    @Test
    void getPaymentById_shouldReturnPaymentResponse_whenExists() {
        when(paymentDAO.findById(DEFAULT_PAYMENT_ID)).thenReturn(Optional.of(defaultPendingPayment));
        when(paymentMapper.toResponseDto(defaultPendingPayment)).thenReturn(defaultPaymentResponseDto);

        PaymentResponseDto result = paymentService.getPaymentById(DEFAULT_PAYMENT_ID);

        assertThat(result).isNotNull();
        verify(paymentDAO).findById(DEFAULT_PAYMENT_ID);
    }

    @Test
    void getPaymentById_shouldThrowPaymentNotFoundException_whenDoesNotExist() {
        when(paymentDAO.findById(INVALID_ID)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentById(INVALID_ID));
    }

    @Test
    void getPaymentsByFilters_shouldReturnList_whenAllFiltersPresent() {
        when(paymentDAO.findByDynamicFilters(DEFAULT_USER_ID, DEFAULT_ORDER_ID, PaymentStatus.PENDING))
                .thenReturn(List.of(defaultPendingPayment));
        when(paymentMapper.toResponseDto(defaultPendingPayment)).thenReturn(defaultPaymentResponseDto);

        List<PaymentResponseDto> result =
                paymentService.getPaymentsByFilters(DEFAULT_USER_ID, DEFAULT_ORDER_ID, PaymentStatus.PENDING);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(defaultPaymentResponseDto);
        verify(paymentDAO).findByDynamicFilters(DEFAULT_USER_ID, DEFAULT_ORDER_ID, PaymentStatus.PENDING);
    }

    @Test
    void getPaymentsByFilters_shouldReturnList_whenUserIdAndOrderId() {
        when(paymentDAO.findByDynamicFilters(DEFAULT_USER_ID, DEFAULT_ORDER_ID, null))
                .thenReturn(List.of(defaultPendingPayment));
        when(paymentMapper.toResponseDto(defaultPendingPayment)).thenReturn(defaultPaymentResponseDto);

        List<PaymentResponseDto> result =
                paymentService.getPaymentsByFilters(DEFAULT_USER_ID, DEFAULT_ORDER_ID, null);

        assertThat(result).hasSize(1);
        verify(paymentDAO).findByDynamicFilters(DEFAULT_USER_ID, DEFAULT_ORDER_ID, null);
    }

    @Test
    void getPaymentsByFilters_shouldReturnList_whenUserIdAndStatus() {
        when(paymentDAO.findByDynamicFilters(DEFAULT_USER_ID, null, PaymentStatus.PENDING))
                .thenReturn(List.of(defaultPendingPayment));
        when(paymentMapper.toResponseDto(defaultPendingPayment)).thenReturn(defaultPaymentResponseDto);

        List<PaymentResponseDto> result =
                paymentService.getPaymentsByFilters(DEFAULT_USER_ID, null, PaymentStatus.PENDING);

        assertThat(result).hasSize(1);
        verify(paymentDAO).findByDynamicFilters(DEFAULT_USER_ID, null, PaymentStatus.PENDING);
    }

    @Test
    void getPaymentsByFilters_shouldReturnList_whenOrderIdAndStatus() {
        when(paymentDAO.findByDynamicFilters(null, DEFAULT_ORDER_ID, PaymentStatus.PENDING))
                .thenReturn(List.of(defaultPendingPayment));
        when(paymentMapper.toResponseDto(defaultPendingPayment)).thenReturn(defaultPaymentResponseDto);

        List<PaymentResponseDto> result =
                paymentService.getPaymentsByFilters(null, DEFAULT_ORDER_ID, PaymentStatus.PENDING);

        assertThat(result).hasSize(1);
        verify(paymentDAO).findByDynamicFilters(null, DEFAULT_ORDER_ID, PaymentStatus.PENDING);
    }

    @Test
    void getPaymentsByFilters_shouldReturnList_whenOnlyUserId() {
        when(paymentDAO.findByDynamicFilters(DEFAULT_USER_ID, null, null))
                .thenReturn(List.of(defaultPendingPayment));
        when(paymentMapper.toResponseDto(defaultPendingPayment)).thenReturn(defaultPaymentResponseDto);

        List<PaymentResponseDto> result = paymentService.getPaymentsByFilters(DEFAULT_USER_ID, null, null);

        assertThat(result).hasSize(1);
        verify(paymentDAO).findByDynamicFilters(DEFAULT_USER_ID, null, null);
    }

    @Test
    void getPaymentsByFilters_shouldReturnList_whenOnlyOrderId() {
        when(paymentDAO.findByDynamicFilters(null, DEFAULT_ORDER_ID, null))
                .thenReturn(List.of(defaultPendingPayment));
        when(paymentMapper.toResponseDto(defaultPendingPayment)).thenReturn(defaultPaymentResponseDto);

        List<PaymentResponseDto> result = paymentService.getPaymentsByFilters(null, DEFAULT_ORDER_ID, null);

        assertThat(result).hasSize(1);
        verify(paymentDAO).findByDynamicFilters(null, DEFAULT_ORDER_ID, null);
    }

    @Test
    void getPaymentsByFilters_shouldReturnList_whenOnlyStatus() {
        when(paymentDAO.findByDynamicFilters(null, null, PaymentStatus.PENDING))
                .thenReturn(List.of(defaultPendingPayment));
        when(paymentMapper.toResponseDto(defaultPendingPayment)).thenReturn(defaultPaymentResponseDto);

        List<PaymentResponseDto> result = paymentService.getPaymentsByFilters(null, null, PaymentStatus.PENDING);

        assertThat(result).hasSize(1);
        verify(paymentDAO).findByDynamicFilters(null, null, PaymentStatus.PENDING);
    }

    @Test
    void getPaymentsByFilters_shouldReturnAll_whenNoFilters() {
        when(paymentDAO.findByDynamicFilters(null, null, null))
                .thenReturn(List.of(defaultPendingPayment));
        when(paymentMapper.toResponseDto(defaultPendingPayment)).thenReturn(defaultPaymentResponseDto);

        List<PaymentResponseDto> result = paymentService.getPaymentsByFilters(null, null, null);

        assertThat(result).hasSize(1);
        verify(paymentDAO).findByDynamicFilters(null, null, null);
    }

    @Test
    void getTotalSuccessfulPaymentsForUser_shouldReturnTotalAmount() {
        when(paymentDAO.getTotalSuccessfulPaymentsByUserIdAndDateRange(DEFAULT_USER_ID, DEFAULT_TIMESTAMP, DEFAULT_TIMESTAMP))
                .thenReturn(DEFAULT_AMOUNT);

        BigDecimal result = paymentService.getTotalSuccessfulPaymentsForUser(DEFAULT_USER_ID, DEFAULT_TIMESTAMP, DEFAULT_TIMESTAMP);

        assertThat(result).isEqualTo(DEFAULT_AMOUNT);
        verify(paymentDAO).getTotalSuccessfulPaymentsByUserIdAndDateRange(DEFAULT_USER_ID, DEFAULT_TIMESTAMP, DEFAULT_TIMESTAMP);
    }

    @Test
    void getTotalSuccessfulPaymentsForAll_shouldReturnTotalAmount() {
        when(paymentDAO.getTotalSuccessfulPaymentsByDateRange(DEFAULT_TIMESTAMP, DEFAULT_TIMESTAMP))
                .thenReturn(DEFAULT_AMOUNT);

        BigDecimal result = paymentService.getTotalSuccessfulPaymentsForAll(DEFAULT_TIMESTAMP, DEFAULT_TIMESTAMP);

        assertThat(result).isEqualTo(DEFAULT_AMOUNT);
        verify(paymentDAO).getTotalSuccessfulPaymentsByDateRange(DEFAULT_TIMESTAMP, DEFAULT_TIMESTAMP);
    }

    @Test
    void isPaymentOwnedByUser_shouldReturnTrue_whenOwnerMatches() {
        when(paymentDAO.findById(DEFAULT_PAYMENT_ID)).thenReturn(Optional.of(defaultPendingPayment));

        boolean result = paymentService.isPaymentOwnedByUser(DEFAULT_PAYMENT_ID, DEFAULT_USER_ID);

        assertThat(result).isTrue();
    }

    @Test
    void isPaymentOwnedByUser_shouldReturnFalse_whenOwnerMismatches() {
        when(paymentDAO.findById(DEFAULT_PAYMENT_ID)).thenReturn(Optional.of(defaultPendingPayment));

        boolean result = paymentService.isPaymentOwnedByUser(DEFAULT_PAYMENT_ID, OTHER_USER_ID);

        assertThat(result).isFalse();
    }

    @Test
    void isPaymentOwnedByUser_shouldThrowPaymentNotFoundException_whenPaymentDoesNotExist() {
        when(paymentDAO.findById(INVALID_ID)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.isPaymentOwnedByUser(INVALID_ID, DEFAULT_USER_ID));
    }
}