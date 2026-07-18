package com.innowise.payment_service.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private static final String PAYMENT_NOT_FOUND_MSG = "Payment not found";
    private static final String ACCESS_DENIED_MSG = "Access denied";
    private static final String VALIDATION_FAILED_MSG = "Validation failed";
    private static final String FIELD_ERROR_MSG = "must not be null";
    private static final String RESOURCE_NOT_FOUND_MSG = "Resource or handler endpoint not found";
    private static final String GENERIC_ERROR_MSG = "An unexpected internal server error occurred: ";

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlePaymentNotFound_shouldReturnNotFoundProblemDetail() {
        PaymentNotFoundException exception = new PaymentNotFoundException(PAYMENT_NOT_FOUND_MSG);

        ResponseEntity<ProblemDetail> response = handler.handlePaymentNotFound(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .satisfies(problemDetail -> {
                    assertThat(problemDetail.getDetail()).isEqualTo(PAYMENT_NOT_FOUND_MSG);
                    assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
                });
    }

    @Test
    void handleAccessDenied_shouldReturnForbiddenProblemDetail() {
        AccessDeniedException exception = new AccessDeniedException(ACCESS_DENIED_MSG);

        ResponseEntity<ProblemDetail> response = handler.handleAccessDenied(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).satisfies(problemDetail ->
                assertThat(problemDetail.getDetail()).isEqualTo(ACCESS_DENIED_MSG)
        );
    }

    @Test
    void handleValidation_shouldReturnBadRequestWithErrorsMap() {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", FIELD_ERROR_MSG);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ProblemDetail> response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail problemDetail = response.getBody();
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getDetail()).isEqualTo(VALIDATION_FAILED_MSG);

        Map<String, String> errors = (Map<String, String>) problemDetail.getProperties().get("errors");
        assertThat(errors).containsEntry("field", FIELD_ERROR_MSG);
    }

    @Test
    void handleConstraintViolation_shouldReturnBadRequestWithErrorsMap() {
        ConstraintViolationException exception = mock(ConstraintViolationException.class);
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);

        when(path.toString()).thenReturn("orderId");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(FIELD_ERROR_MSG);
        when(exception.getConstraintViolations()).thenReturn(Set.of(violation));

        ResponseEntity<ProblemDetail> response = handler.handleConstraintViolation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail problemDetail = response.getBody();
        assertThat(problemDetail).isNotNull();
        assertThat(problemDetail.getDetail()).isEqualTo(VALIDATION_FAILED_MSG);

        Map<String, String> errors = (Map<String, String>) problemDetail.getProperties().get("errors");
        assertThat(errors).containsEntry("orderId", FIELD_ERROR_MSG);
    }

    @Test
    void handleTypeMismatch_shouldReturnBadRequestWithParameterInfo() {
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);

        when(exception.getName()).thenReturn("userId");
        when(exception.getRequiredType()).thenAnswer(invocation -> Long.class);

        ResponseEntity<ProblemDetail> response = handler.handleTypeMismatch(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(problemDetail ->
                assertThat(problemDetail.getDetail()).isEqualTo("Parameter 'userId' should be of type 'Long'")
        );
    }

    @Test
    void handleTypeMismatch_shouldReturnBadRequestWithUnknownType_whenRequiredTypeIsNull() {
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);

        when(exception.getName()).thenReturn("userId");
        when(exception.getRequiredType()).thenReturn(null);

        ResponseEntity<ProblemDetail> response = handler.handleTypeMismatch(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(problemDetail ->
                assertThat(problemDetail.getDetail()).isEqualTo("Parameter 'userId' should be of type 'unknown'")
        );
    }

    @Test
    void handleNotFound_shouldReturnNotFoundGenericMessage() {
        ResponseEntity<ProblemDetail> response = handler.handleNotFound();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(problemDetail ->
                assertThat(problemDetail.getDetail()).isEqualTo(RESOURCE_NOT_FOUND_MSG)
        );
    }

    @Test
    void handleGenericException_shouldReturnInternalServerErrorWithMessage() {
        Exception exception = new RuntimeException("Something went wrong");

        ResponseEntity<ProblemDetail> response = handler.handleGenericException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).satisfies(problemDetail ->
                assertThat(problemDetail.getDetail()).isEqualTo("An unexpected internal server error occurred")
        );
    }

    @Test
    void handlePaymentProcessing_shouldReturnInternalServerErrorWithMessage() {
        PaymentProcessingException exception = new PaymentProcessingException("Payment processing error");

        ResponseEntity<ProblemDetail> response = handler.handlePaymentProcessing(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).satisfies(problemDetail ->
                assertThat(problemDetail.getDetail()).isEqualTo("Payment processing error")
        );
    }

    @Test
    void handleIllegalArgument_shouldReturnBadRequestWithMessage() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");

        ResponseEntity<ProblemDetail> response = handler.handleIllegalArgument(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(problemDetail ->
                assertThat(problemDetail.getDetail()).isEqualTo("Invalid argument")
        );
    }
}