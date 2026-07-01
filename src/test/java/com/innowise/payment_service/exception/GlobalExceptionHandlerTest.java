package com.innowise.payment_service.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlePaymentNotFound_shouldReturnNotFoundProblemDetail() {
        PaymentNotFoundException ex = new PaymentNotFoundException("Payment not found");

        ResponseEntity<ProblemDetail> response = handler.handlePaymentNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Payment not found");
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void handleAccessDenied_shouldReturnForbiddenProblemDetail() {
        AccessDeniedException ex = new AccessDeniedException("Access denied");

        ResponseEntity<ProblemDetail> response = handler.handleAccessDenied(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Access denied");
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleValidation_shouldReturnBadRequestWithErrorsMap() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "must not be null");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ProblemDetail> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail detail = response.getBody();
        assertThat(detail).isNotNull();
        assertThat(detail.getDetail()).isEqualTo("Validation failed");
        Map<String, String> errors = (Map<String, String>) detail.getProperties().get("errors");
        assertThat(errors).containsEntry("field", "must not be null");
    }

    @Test
    @SuppressWarnings("unchecked")
    void handleConstraintViolation_shouldReturnBadRequestWithErrorsMap() {
        ConstraintViolationException ex = mock(ConstraintViolationException.class);
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("orderId");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be null");
        when(ex.getConstraintViolations()).thenReturn(Set.of(violation));

        ResponseEntity<ProblemDetail> response = handler.handleConstraintViolation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail detail = response.getBody();
        assertThat(detail).isNotNull();
        Map<String, String> errors = (Map<String, String>) detail.getProperties().get("errors");
        assertThat(errors).containsEntry("orderId", "must not be null");
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void handleTypeMismatch_shouldReturnBadRequestWithParameterInfo() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("userId");
        when(ex.getRequiredType()).thenReturn((Class) Long.class);

        ResponseEntity<ProblemDetail> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Parameter 'userId' should be of type 'Long'");
    }

    @Test
    void handleTypeMismatch_shouldReturnBadRequestWithUnknownType_whenRequiredTypeIsNull() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("userId");
        when(ex.getRequiredType()).thenReturn(null);

        ResponseEntity<ProblemDetail> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Parameter 'userId' should be of type 'unknown'");
    }

    @Test
    void handleNotFound_shouldReturnNotFoundGenericMessage() {
        Exception ex = new Exception();

        ResponseEntity<ProblemDetail> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("Resource or handler endpoint not found");
    }

    @Test
    void handleGenericException_shouldReturnInternalServerErrorWithMessage() {
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<ProblemDetail> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getDetail()).isEqualTo("An unexpected internal server error occurred: Something went wrong");
    }
}
