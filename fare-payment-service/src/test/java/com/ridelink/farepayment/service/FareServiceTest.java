package com.ridelink.farepayment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ridelink.farepayment.config.FareProperties;
import com.ridelink.farepayment.dto.CalculateFareRequest;
import com.ridelink.farepayment.dto.FareEstimateRequest;
import com.ridelink.farepayment.dto.FareEstimateResponse;
import com.ridelink.farepayment.dto.FareResponse;
import com.ridelink.farepayment.exception.DuplicateFareException;
import com.ridelink.farepayment.exception.FareNotFoundException;
import com.ridelink.farepayment.model.Fare;
import com.ridelink.farepayment.repository.FareRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FareServiceTest {

    @Mock
    private FareRepository fareRepository;

    private FareService fareService;

    @BeforeEach
    void setUp() {
        FareCalculator calculator = new FareCalculator(new FareProperties(
                "LKR", new BigDecimal("150"), new BigDecimal("80"), new BigDecimal("5"), new BigDecimal("250")));
        fareService = new FareService(fareRepository, calculator);
    }

    @Test
    void estimateReturnsBreakdownWithoutSaving() {
        FareEstimateResponse response =
                fareService.estimate(new FareEstimateRequest("Malabe", "Colombo Fort", 10.0, 20));

        assertThat(response.estimatedTotal()).isEqualByComparingTo("1050.00");
        assertThat(response.pickupLocation()).isEqualTo("Malabe");
        verify(fareRepository, never()).save(any());
    }

    @Test
    void calculateFinalFareStoresFareWithServerSideFields() {
        when(fareRepository.existsByRideId("ride-1")).thenReturn(false);
        when(fareRepository.save(any(Fare.class))).thenAnswer(invocation -> {
            Fare saved = invocation.getArgument(0);
            saved.setId("fare-1");
            return saved;
        });

        FareResponse response =
                fareService.calculateFinalFare(new CalculateFareRequest("ride-1", "acc-1", 10.0, 20));

        assertThat(response.id()).isEqualTo("fare-1");
        assertThat(response.rideId()).isEqualTo("ride-1");
        assertThat(response.totalAmount()).isEqualByComparingTo("1050.00");
        assertThat(response.currency()).isEqualTo("LKR");
        assertThat(response.createdAt()).isNotNull().isEqualTo(response.updatedAt());
    }

    @Test
    void calculateFinalFareRejectsDuplicateRide() {
        when(fareRepository.existsByRideId("ride-1")).thenReturn(true);

        assertThatThrownBy(() ->
                fareService.calculateFinalFare(new CalculateFareRequest("ride-1", "acc-1", 10.0, 20)))
                .isInstanceOf(DuplicateFareException.class);
        verify(fareRepository, never()).save(any());
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(fareRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fareService.getById("nope")).isInstanceOf(FareNotFoundException.class);
    }

    @Test
    void getByRideIdThrowsWhenMissing() {
        when(fareRepository.findByRideId("ride-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fareService.getByRideId("ride-x")).isInstanceOf(FareNotFoundException.class);
    }
}
