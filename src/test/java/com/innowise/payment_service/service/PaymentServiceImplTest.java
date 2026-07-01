package com.innowise.payment_service.service;

import com.innowise.payment_service.dao.PaymentDAO;
import com.innowise.payment_service.exception.PaymentNotFoundException;
import com.innowise.payment_service.mapper.PaymentMapper;
import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.document.PaymentStatus;
import com.innowise.payment_service.service.impl.PaymentProcessor;
import com.innowise.payment_service.service.impl.PaymentServiceImpl;
import com.innowise.payment_service.testdata.PaymentTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
    void whenInitiatePayment_thenSaveAndTriggerAsyncProcessing() {
        when(paymentDAO.save(Mockito.any(Payment.class))).thenReturn(defaultPendingPayment);
        paymentService.initiatePayment(defaultPaymentRequestDto, DEFAULT_USER_ID);
        verify(paymentDAO).save(Mockito.any(Payment.class));
        verify(paymentProcessor).processPaymentAsync(defaultPendingPayment);
    }

    @Test
    void whenPaymentNotFound_thenThrowPaymentNotFoundException() {
        when(paymentDAO.findById("INVALID")).thenReturn(Optional.empty());
        assertThrows(PaymentNotFoundException.class, () -> {
            paymentService.getPaymentById("INVALID");
        });
    }

    @Test
    void whenFiltersAreAllPresent_thenCallCorrectDaoMethod() {
        when(paymentDAO.findByUserIdAndOrderIdAndStatus(DEFAULT_USER_ID, DEFAULT_ORDER_ID, PaymentStatus.PENDING))
                .thenReturn(List.of(defaultPendingPayment));
        paymentService.getPaymentsByFilters(DEFAULT_USER_ID, DEFAULT_ORDER_ID, PaymentStatus.PENDING);
        verify(paymentDAO).findByUserIdAndOrderIdAndStatus(DEFAULT_USER_ID, DEFAULT_ORDER_ID, PaymentStatus.PENDING);
    }

    @Test
    void whenOnlyUserIdIsPresent_thenCallFindByUserId() {
        when(paymentDAO.findByUserId(DEFAULT_USER_ID)).thenReturn(List.of(defaultPendingPayment));
        paymentService.getPaymentsByFilters(DEFAULT_USER_ID, null, null);
        verify(paymentDAO).findByUserId(DEFAULT_USER_ID);
    }

    @Test
    void whenNoFilters_thenCallFindAll() {
        when(paymentDAO.findAll()).thenReturn(List.of(defaultPendingPayment));
        paymentService.getPaymentsByFilters(null, null, null);
        verify(paymentDAO).findAll();
    }
}