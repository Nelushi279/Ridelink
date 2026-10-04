package com.ridelink.farepayment.service;

import com.ridelink.farepayment.config.FareProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * The single place where the RideLink fare rule lives.
 *
 * <pre>
 * distanceFare = distanceKm      x perKmRate
 * timeFare     = durationMinutes x perMinuteRate
 * total        = max(baseFare + distanceFare + timeFare, minimumFare)
 * </pre>
 *
 * All amounts are rounded to 2 decimal places (HALF_UP). The same rule is used for
 * both the estimate and the final fare; only the inputs differ (planned vs. actual trip).
 */
@Component
public class FareCalculator {

    private static final int SCALE = 2;

    private final FareProperties properties;

    public FareCalculator(FareProperties properties) {
        this.properties = properties;
    }

    public FareBreakdown calculate(double distanceKm, int durationMinutes) {
        if (distanceKm <= 0) {
            throw new IllegalArgumentException("distanceKm must be greater than 0");
        }
        if (durationMinutes < 0) {
            throw new IllegalArgumentException("durationMinutes must not be negative");
        }
        BigDecimal baseFare = scale(properties.baseFare());
        BigDecimal distanceFare = scale(properties.perKmRate().multiply(BigDecimal.valueOf(distanceKm)));
        BigDecimal timeFare = scale(properties.perMinuteRate().multiply(BigDecimal.valueOf(durationMinutes)));
        BigDecimal total = baseFare.add(distanceFare).add(timeFare).max(scale(properties.minimumFare()));
        return new FareBreakdown(baseFare, distanceFare, timeFare, total, properties.currency());
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
