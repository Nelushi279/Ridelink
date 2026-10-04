package com.ridelink.farepayment.dto;

import com.ridelink.farepayment.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;

public record ReceiptResponse(
        String receiptNumber,
        String paymentId,
        String rideId,
        String fareId,
        String passengerAccountId,
        double distanceKm,
        int durationMinutes,
        BigDecimal baseFare,
        BigDecimal distanceFare,
        BigDecimal timeFare,
        BigDecimal totalAmount,
        String currency,
        PaymentMethod paymentMethod,
        String transactionReference,
        Instant paidAt) {
}
