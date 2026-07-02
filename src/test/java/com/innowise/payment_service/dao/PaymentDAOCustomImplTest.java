package com.innowise.payment_service.dao;

import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.document.PaymentStatus;
import com.innowise.payment_service.testdata.PaymentTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentDAOCustomImplTest extends PaymentTestData {

    private MongoTemplate mongoTemplate;
    private PaymentDAOCustomImpl paymentDAOCustom;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        paymentDAOCustom = new PaymentDAOCustomImpl(mongoTemplate);
    }

    @Test
    void getTotalSuccessfulPaymentsByUserIdAndDateRange_shouldReturnZero_whenResultIsNull() {
        Instant from = DEFAULT_TIMESTAMP.minusSeconds(60);
        Instant to = DEFAULT_TIMESTAMP;
        AggregationResults<Object> aggregationResults = mock(AggregationResults.class);
        when(aggregationResults.getUniqueMappedResult()).thenReturn(null);

        when(mongoTemplate.aggregate(any(Aggregation.class), eq(Payment.class), any(Class.class)))
                .thenReturn(aggregationResults);

        BigDecimal result = paymentDAOCustom.getTotalSuccessfulPaymentsByUserIdAndDateRange(DEFAULT_USER_ID, from, to);

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getTotalSuccessfulPaymentsByDateRange_shouldReturnZero_whenResultIsNull() {
        Instant from = DEFAULT_TIMESTAMP.minusSeconds(60);
        Instant to = DEFAULT_TIMESTAMP;
        AggregationResults<Object> aggregationResults = mock(AggregationResults.class);
        when(aggregationResults.getUniqueMappedResult()).thenReturn(null);

        when(mongoTemplate.aggregate(any(Aggregation.class), eq(Payment.class), any(Class.class)))
                .thenReturn(aggregationResults);

        BigDecimal result = paymentDAOCustom.getTotalSuccessfulPaymentsByDateRange(from, to);

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void findByDynamicFilters_shouldReturnAll_whenNoFilters() {
        List<Payment> expected = List.of(defaultPendingPayment, defaultSuccessPayment);
        when(mongoTemplate.findAll(Payment.class)).thenReturn(expected);

        List<Payment> result = paymentDAOCustom.findByDynamicFilters(null, null, null);

        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void findByDynamicFilters_shouldFilterByUserId() {
        List<Payment> expected = List.of(defaultPendingPayment);
        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(expected);

        List<Payment> result = paymentDAOCustom.findByDynamicFilters(DEFAULT_USER_ID, null, null);

        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void findByDynamicFilters_shouldFilterByOrderId() {
        List<Payment> expected = List.of(defaultPendingPayment);
        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(expected);

        List<Payment> result = paymentDAOCustom.findByDynamicFilters(null, DEFAULT_ORDER_ID, null);

        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void findByDynamicFilters_shouldFilterByStatus() {
        List<Payment> expected = List.of(defaultPendingPayment);
        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(expected);

        List<Payment> result = paymentDAOCustom.findByDynamicFilters(null, null, PaymentStatus.PENDING);

        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void findByDynamicFilters_shouldFilterByUserIdAndOrderId() {
        List<Payment> expected = List.of(defaultPendingPayment);
        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(expected);

        List<Payment> result = paymentDAOCustom.findByDynamicFilters(DEFAULT_USER_ID, DEFAULT_ORDER_ID, null);

        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void findByDynamicFilters_shouldFilterByUserIdAndStatus() {
        List<Payment> expected = List.of(defaultPendingPayment);
        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(expected);

        List<Payment> result = paymentDAOCustom.findByDynamicFilters(DEFAULT_USER_ID, null, PaymentStatus.PENDING);

        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void findByDynamicFilters_shouldFilterByOrderIdAndStatus() {
        List<Payment> expected = List.of(defaultPendingPayment);
        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(expected);

        List<Payment> result = paymentDAOCustom.findByDynamicFilters(null, DEFAULT_ORDER_ID, PaymentStatus.PENDING);

        assertThat(result).containsExactlyElementsOf(expected);
    }

    @Test
    void findByDynamicFilters_shouldFilterByUserIdAndOrderIdAndStatus() {
        List<Payment> expected = List.of(defaultPendingPayment);
        when(mongoTemplate.find(any(Query.class), eq(Payment.class))).thenReturn(expected);

        List<Payment> result = paymentDAOCustom.findByDynamicFilters(DEFAULT_USER_ID, DEFAULT_ORDER_ID, PaymentStatus.PENDING);

        assertThat(result).containsExactlyElementsOf(expected);
    }
}