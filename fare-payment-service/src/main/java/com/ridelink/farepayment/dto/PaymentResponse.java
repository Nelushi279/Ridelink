package com.ridelink.farepayment.dto;

import com.ridelink.farepayment.model.PaymentMethod;
import com.ridelink.farepayment.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String id,
        String rideId,
        String fareId,
        String passengerAccountId,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        String transactionReference,
        String receiptNumber,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        Instant paidAt) {
}
