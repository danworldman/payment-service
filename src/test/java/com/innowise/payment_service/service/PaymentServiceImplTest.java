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
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PaymentServiceImplTest extends PaymentTestData {

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
        when(paymentDAO.save(Mockito.any(Payment.class))).thenReturn(defaultPendingPayment);
        when(paymentMapper.toResponseDto(defaultPendingPayment)).thenReturn(defaultPaymentResponseDto);

        PaymentResponseDto result = paymentService.initiatePayment(defaultPaymentRequestDto, DEFAULT_USER_ID);

        assertThat(result).isNotNull();
        verify(paymentDAO).save(Mockito.any(Payment.class));
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
        when(paymentDAO.findById("INVALID")).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> {
            paymentService.getPaymentById("INVALID");
        });
    }

    @Test
    void getPaymentsByFilters_shouldCallCorrectDaoMethod_whenAllFiltersPresent() {
        when(paymentDAO.findByUserIdAndOrderIdAndStatus(DEFAULT_USER_ID, DEFAULT_ORDER_ID, PaymentStatus.PENDING))
                .thenReturn(List.of(defaultPendingPayment));

        paymentService.getPaymentsByFilters(DEFAULT_USER_ID, DEFAULT_ORDER_ID, PaymentStatus.PENDING);

        verify(paymentDAO).findByUserIdAndOrderIdAndStatus(DEFAULT_USER_ID, DEFAULT_ORDER_ID, PaymentStatus.PENDING);
    }

    @Test
    void getPaymentsByFilters_shouldCallFindByUserIdAndOrderId_whenStatusMissing() {
        when(paymentDAO.findByUserIdAndOrderId(DEFAULT_USER_ID, DEFAULT_ORDER_ID))
                .thenReturn(List.of(defaultPendingPayment));

        paymentService.getPaymentsByFilters(DEFAULT_USER_ID, DEFAULT_ORDER_ID, null);

        verify(paymentDAO).findByUserIdAndOrderId(DEFAULT_USER_ID, DEFAULT_ORDER_ID);
    }

    @Test
    void getPaymentsByFilters_shouldCallFindByUserIdAndStatus_whenOrderIdMissing() {
        when(paymentDAO.findByUserIdAndStatus(DEFAULT_USER_ID, PaymentStatus.PENDING))
                .thenReturn(List.of(defaultPendingPayment));

        paymentService.getPaymentsByFilters(DEFAULT_USER_ID, null, PaymentStatus.PENDING);

        verify(paymentDAO).findByUserIdAndStatus(DEFAULT_USER_ID, PaymentStatus.PENDING);
    }

    @Test
    void getPaymentsByFilters_shouldCallFindByOrderIdAndStatus_whenUserIdMissing() {
        when(paymentDAO.findByOrderIdAndStatus(DEFAULT_ORDER_ID, PaymentStatus.PENDING))
                .thenReturn(List.of(defaultPendingPayment));

        paymentService.getPaymentsByFilters(null, DEFAULT_ORDER_ID, PaymentStatus.PENDING);

        verify(paymentDAO).findByOrderIdAndStatus(DEFAULT_ORDER_ID, PaymentStatus.PENDING);
    }

    @Test
    void getPaymentsByFilters_shouldCallFindByUserId_whenOnlyUserIdIsPresent() {
        when(paymentDAO.findByUserId(DEFAULT_USER_ID)).thenReturn(List.of(defaultPendingPayment));

        paymentService.getPaymentsByFilters(DEFAULT_USER_ID, null, null);

        verify(paymentDAO).findByUserId(DEFAULT_USER_ID);
    }

    @Test
    void getPaymentsByFilters_shouldCallFindByOrderId_whenOnlyOrderIdIsPresent() {
        when(paymentDAO.findByOrderId(DEFAULT_ORDER_ID)).thenReturn(List.of(defaultPendingPayment));

        paymentService.getPaymentsByFilters(null, DEFAULT_ORDER_ID, null);

        verify(paymentDAO).findByOrderId(DEFAULT_ORDER_ID);
    }

    @Test
    void getPaymentsByFilters_shouldCallFindByStatus_whenOnlyStatusIsPresent() {
        when(paymentDAO.findByStatus(PaymentStatus.PENDING)).thenReturn(List.of(defaultPendingPayment));

        paymentService.getPaymentsByFilters(null, null, PaymentStatus.PENDING);

        verify(paymentDAO).findByStatus(PaymentStatus.PENDING);
    }

    @Test
    void getPaymentsByFilters_shouldCallFindAll_whenNoFiltersPassed() {
        when(paymentDAO.findAll()).thenReturn(List.of(defaultPendingPayment));

        paymentService.getPaymentsByFilters(null, null, null);

        verify(paymentDAO).findAll();
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

        boolean result = paymentService.isPaymentOwnedByUser(DEFAULT_PAYMENT_ID, 999L);

        assertThat(result).isFalse();
    }

    @Test
    void isPaymentOwnedByUser_shouldThrowPaymentNotFoundException_whenPaymentDoesNotExist() {
        when(paymentDAO.findById("INVALID")).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> {
            paymentService.isPaymentOwnedByUser("INVALID", DEFAULT_USER_ID);
        });
    }
}
