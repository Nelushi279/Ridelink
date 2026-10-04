package com.ridelink.farepayment.dto;

import java.math.BigDecimal;

public record FareEstimateResponse(
        String pickupLocation,
        String dropoffLocation,
        double distanceKm,
        int durationMinutes,
        BigDecimal baseFare,
        BigDecimal distanceFare,
        BigDecimal timeFare,
        BigDecimal estimatedTotal,
        String currency) {
}
