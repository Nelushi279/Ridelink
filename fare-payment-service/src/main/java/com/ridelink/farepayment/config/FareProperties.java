package com.ridelink.farepayment.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Rates used by the documented fare rule. Values come from application.yml (ridelink.fare.*). */
@ConfigurationProperties(prefix = "ridelink.fare")
public record FareProperties(
        String currency,
        BigDecimal baseFare,
        BigDecimal perKmRate,
        BigDecimal perMinuteRate,
        BigDecimal minimumFare) {
}
