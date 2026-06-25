package com.innowise.payment_service.controller;

import com.innowise.payment_service.model.document.PaymentStatus;
import com.innowise.payment_service.model.dto.PaymentRequestDto;
import com.innowise.payment_service.model.dto.PaymentResponseDto;
import com.innowise.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PaymentResponseDto> createPayment(
            @Valid @RequestBody PaymentRequestDto requestDto,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getClaim("user_id").toString());
        PaymentResponseDto response = paymentService.initiatePayment(requestDto, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<PaymentResponseDto> getPaymentById(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(jwt.getClaim("user_id").toString());
        boolean isAdmin = jwt.getClaimAsStringList("roles") != null && jwt.getClaimAsStringList("roles").contains("ADMIN");

        PaymentResponseDto payment = paymentService.getPaymentById(id);
        if (!isAdmin && !payment.userId().equals(userId)) {
            throw new AccessDeniedException("Access denied to this payment resource");
        }
        return ResponseEntity.ok(payment);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<PaymentResponseDto>> getPayments(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal Jwt jwt) {
        Long authenticatedUserId = Long.valueOf(jwt.getClaim("user_id").toString());
        boolean isAdmin = jwt.getClaimAsStringList("roles") != null && jwt.getClaimAsStringList("roles").contains("ADMIN");
        Long finalUserId = isAdmin ? userId : authenticatedUserId;

        List<PaymentResponseDto> payments = paymentService.getPaymentsByFilters(finalUserId, orderId, status);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/users/{userId}/summary")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<BigDecimal> getUserSummary(
            @PathVariable Long userId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @AuthenticationPrincipal Jwt jwt) {
        Long authenticatedUserId = Long.valueOf(jwt.getClaim("user_id").toString());
        boolean isAdmin = jwt.getClaimAsStringList("roles") != null && jwt.getClaimAsStringList("roles").contains("ADMIN");

        if (!isAdmin && !userId.equals(authenticatedUserId)) {
            throw new AccessDeniedException("Access denied to other user summary");
        }
        BigDecimal total = paymentService.getTotalSuccessfulPaymentsForUser(userId, from, to);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BigDecimal> getAllSummary(
            @RequestParam Instant from,
            @RequestParam Instant to) {
        BigDecimal total = paymentService.getTotalSuccessfulPaymentsForAll(from, to);
        return ResponseEntity.ok(total);
    }
}