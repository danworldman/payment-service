package com.innowise.payment_service.dao;

import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.document.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PaymentDAOCustomImpl implements PaymentDAOCustom {

    private static final String FIELD_PAYMENT_AMOUNT = "payment_amount";
    private static final String FIELD_TOTAL = "total";

    private final MongoTemplate mongoTemplate;

    @Override
    public BigDecimal getTotalSuccessfulPaymentsByUserIdAndDateRange(Long userId, Instant from, Instant to) {
        Criteria criteria = Criteria.where("status").is(PaymentStatus.SUCCESS)
                .and("user_id")
                .is(userId)
                .and("timestamp")
                .gte(from)
                .lte(to);

        return aggregateTotal(criteria);
    }

    @Override
    public BigDecimal getTotalSuccessfulPaymentsByDateRange(Instant from, Instant to) {
        Criteria criteria = Criteria.where("status").is(PaymentStatus.SUCCESS)
                .and("timestamp")
                .gte(from)
                .lte(to);

        return aggregateTotal(criteria);
    }

    @Override
    public List<Payment> findByDynamicFilters(Long userId, Long orderId, PaymentStatus status) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (userId != null) {
            criteriaList.add(Criteria.where("user_id").is(userId));
        }
        if (orderId != null) {
            criteriaList.add(Criteria.where("order_id").is(orderId));
        }
        if (status != null) {
            criteriaList.add(Criteria.where("status").is(status));
        }

        if (criteriaList.isEmpty()) {
            return mongoTemplate.findAll(Payment.class);
        }

        Criteria criteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        Query query = new Query(criteria);

        return mongoTemplate.find(query, Payment.class);
    }

    private BigDecimal aggregateTotal(Criteria criteria) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group().sum(FIELD_PAYMENT_AMOUNT).as(FIELD_TOTAL)
        );

        AggregationResults<AggregationResult> results =
                mongoTemplate.aggregate(aggregation, Payment.class, AggregationResult.class);

        AggregationResult uniqueResult = results.getUniqueMappedResult();
        return uniqueResult != null ? uniqueResult.total() : BigDecimal.ZERO;
    }

    private record AggregationResult(BigDecimal total) {
    }
}