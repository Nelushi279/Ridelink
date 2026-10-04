package com.ridelink.farepayment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record FareResponse(
        String id,
        String rideId,
        String passengerAccountId,
        double distanceKm,
        int durationMinutes,
        BigDecimal baseFare,
        BigDecimal distanceFare,
        BigDecimal timeFare,
        BigDecimal totalAmount,
        String currency,
        Instant createdAt,
        Instant updatedAt) {
}
