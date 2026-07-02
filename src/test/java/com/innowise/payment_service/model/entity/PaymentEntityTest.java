package com.innowise.payment_service.model.entity;

import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.dto.PaymentRequestDto;
import com.innowise.payment_service.testdata.PaymentTestData;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEntityTest extends PaymentTestData {

    private final Validator validator;

    PaymentEntityTest() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    @Test
    void whenPaymentIdsAreEqual_thenObjectsAreEqualAndShareHashCode() {
        Payment payment1 = Payment.builder().id(DEFAULT_PAYMENT_ID).build();
        Payment payment2 = Payment.builder().id(DEFAULT_PAYMENT_ID).build();

        assertThat(payment1).isEqualTo(payment2);
        assertThat(payment1.hashCode()).isEqualTo(payment2.hashCode());
    }

    @Test
    void whenPaymentIdsAreDifferent_thenObjectsAreNotEqual() {
        Payment payment1 = Payment.builder().id(DEFAULT_PAYMENT_ID).build();
        Payment payment2 = Payment.builder().id(ANOTHER_PAYMENT_ID).build();

        assertThat(payment1).isNotEqualTo(payment2);
    }

    @Test
    void whenComparedWithNullOrDifferentClass_thenNotEqual() {
        Payment payment = Payment.builder().id(DEFAULT_PAYMENT_ID).build();

        assertThat(payment).isNotEqualTo(null);
        assertThat(payment).isNotEqualTo(ANOTHER_PAYMENT_ID);
    }

    @Test
    void whenComparedWithSameInstance_thenEqual() {
        Payment payment = Payment.builder().id(DEFAULT_PAYMENT_ID).build();

        assertThat(payment).isEqualTo(payment);
    }

    @Test
    void whenPaymentRequestDtoIsValid_thenValidationPasses() {
        Set<ConstraintViolation<PaymentRequestDto>> violations = validator.validate(defaultPaymentRequestDto);

        assertThat(violations).isEmpty();
    }

    @Test
    void whenPaymentAmountIsNegative_thenValidationFails() {
        PaymentRequestDto requestDto = new PaymentRequestDto(DEFAULT_ORDER_ID, NEGATIVE_AMOUNT);
        Set<ConstraintViolation<PaymentRequestDto>> violations = validator.validate(requestDto);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void whenOrderIdIsNull_thenValidationFails() {
        PaymentRequestDto requestDto = new PaymentRequestDto(null, DEFAULT_AMOUNT);
        Set<ConstraintViolation<PaymentRequestDto>> violations = validator.validate(requestDto);

        assertThat(violations).isNotEmpty();
    }

    @Test
    void whenPaymentAmountIsZero_thenValidationFails() {
        PaymentRequestDto requestDto = new PaymentRequestDto(DEFAULT_ORDER_ID, BigDecimal.ZERO);
        Set<ConstraintViolation<PaymentRequestDto>> violations = validator.validate(requestDto);

        assertThat(violations).isNotEmpty();
    }
}