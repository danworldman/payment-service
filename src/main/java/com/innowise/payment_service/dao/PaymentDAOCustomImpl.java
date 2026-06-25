package com.innowise.payment_service.dao;

import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.document.PaymentStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import java.math.BigDecimal;
import java.time.Instant;

public class PaymentDAOCustomImpl implements PaymentDAOCustom {

    private final MongoTemplate mongoTemplate;

    public PaymentDAOCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public BigDecimal getTotalSuccessfulPaymentsByUserIdAndDateRange(Long userId, Instant from, Instant to) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("status").is(PaymentStatus.SUCCESS)
                        .and("user_id").is(userId)
                        .and("timestamp").gte(from).lte(to)),
                Aggregation.group().sum("payment_amount").as("total")
        );
        AggregationResults<AggregationResult> results = mongoTemplate.aggregate(aggregation, Payment.class, AggregationResult.class);
        AggregationResult uniqueResult = results.getUniqueMappedResult();
        return uniqueResult != null ? uniqueResult.total() : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getTotalSuccessfulPaymentsByDateRange(Instant from, Instant to) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("status").is(PaymentStatus.SUCCESS)
                        .and("timestamp").gte(from).lte(to)),
                Aggregation.group().sum("payment_amount").as("total")
        );
        AggregationResults<AggregationResult> results = mongoTemplate.aggregate(aggregation, Payment.class, AggregationResult.class);
        AggregationResult uniqueResult = results.getUniqueMappedResult();
        return uniqueResult != null ? uniqueResult.total() : BigDecimal.ZERO;
    }

    private record AggregationResult(BigDecimal total) {}
}