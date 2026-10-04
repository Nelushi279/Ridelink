package com.ridelink.farepayment.service;

import java.math.BigDecimal;

/** Result of applying the fare rule. */
public record FareBreakdown(
        BigDecimal baseFare,
        BigDecimal distanceFare,
        BigDecimal timeFare,
        BigDecimal total,
        String currency) {
}
