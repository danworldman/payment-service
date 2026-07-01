package com.innowise.payment_service.dao;

import com.innowise.payment_service.model.document.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentDAOCustomImplTest {

    private MongoTemplate mongoTemplate;
    private PaymentDAOCustomImpl paymentDAOCustom;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        paymentDAOCustom = new PaymentDAOCustomImpl(mongoTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTotalSuccessfulPaymentsByUserIdAndDateRange_shouldReturnZero_whenResultIsNull() {
        Instant now = Instant.now();
        AggregationResults<Object> aggregationResults = mock(AggregationResults.class);
        when(aggregationResults.getUniqueMappedResult()).thenReturn(null);

        when(mongoTemplate.aggregate(any(Aggregation.class), eq(Payment.class), any(Class.class)))
                .thenReturn(aggregationResults);

        BigDecimal result = paymentDAOCustom.getTotalSuccessfulPaymentsByUserIdAndDateRange(1L, now, now);

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTotalSuccessfulPaymentsByDateRange_shouldReturnZero_whenResultIsNull() {
        Instant now = Instant.now();
        AggregationResults<Object> aggregationResults = mock(AggregationResults.class);
        when(aggregationResults.getUniqueMappedResult()).thenReturn(null);

        when(mongoTemplate.aggregate(any(Aggregation.class), eq(Payment.class), any(Class.class)))
                .thenReturn(aggregationResults);

        BigDecimal result = paymentDAOCustom.getTotalSuccessfulPaymentsByDateRange(now, now);

        assertThat(result).isEqualTo(BigDecimal.ZERO);
    }
}