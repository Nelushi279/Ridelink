package com.ridelink.farepayment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ridelink.farepayment.config.FareProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FareCalculatorTest {

    private final FareCalculator calculator = new FareCalculator(new FareProperties(
            "LKR", new BigDecimal("150"), new BigDecimal("80"), new BigDecimal("5"), new BigDecimal("250")));

    @Test
    void appliesBaseDistanceAndTimeComponents() {
        FareBreakdown result = calculator.calculate(10, 20);

        assertThat(result.baseFare()).isEqualByComparingTo("150.00");
        assertThat(result.distanceFare()).isEqualByComparingTo("800.00");
        assertThat(result.timeFare()).isEqualByComparingTo("100.00");
        assertThat(result.total()).isEqualByComparingTo("1050.00");
        assertThat(result.currency()).isEqualTo("LKR");
    }

    @Test
    void appliesMinimumFareForVeryShortTrips() {
        FareBreakdown result = calculator.calculate(0.5, 2);

        // 150 + 40 + 10 = 200, below the 250 minimum
        assertThat(result.total()).isEqualByComparingTo("250.00");
    }

    @Test
    void roundsAmountsToTwoDecimalPlaces() {
        FareBreakdown result = calculator.calculate(3.333, 7);

        assertThat(result.distanceFare()).isEqualByComparingTo("266.64");
        assertThat(result.total()).isEqualByComparingTo("451.64");
        assertThat(result.total().scale()).isEqualTo(2);
    }

    @Test
    void acceptsZeroDuration() {
        assertThat(calculator.calculate(5, 0).timeFare()).isEqualByComparingTo("0.00");
    }

    @Test
    void rejectsNonPositiveDistance() {
        assertThatThrownBy(() -> calculator.calculate(0, 10)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeDuration() {
        assertThatThrownBy(() -> calculator.calculate(5, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
