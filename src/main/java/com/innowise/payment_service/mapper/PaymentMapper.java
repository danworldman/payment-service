package com.innowise.payment_service.mapper;

import com.innowise.payment_service.model.document.Payment;
import com.innowise.payment_service.model.dto.PaymentResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    PaymentResponseDto toResponseDto(Payment payment);
}