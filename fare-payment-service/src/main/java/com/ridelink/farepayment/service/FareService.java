package com.ridelink.farepayment.service;

import com.ridelink.farepayment.dto.CalculateFareRequest;
import com.ridelink.farepayment.dto.FareEstimateRequest;
import com.ridelink.farepayment.dto.FareEstimateResponse;
import com.ridelink.farepayment.dto.FareResponse;
import com.ridelink.farepayment.exception.DuplicateFareException;
import com.ridelink.farepayment.exception.FareNotFoundException;
import com.ridelink.farepayment.model.Fare;
import com.ridelink.farepayment.repository.FareRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class FareService {

    private final FareRepository fareRepository;
    private final FareCalculator fareCalculator;

    public FareService(FareRepository fareRepository, FareCalculator fareCalculator) {
        this.fareRepository = fareRepository;
        this.fareCalculator = fareCalculator;
    }

    /** Estimate only: nothing is stored. */
    public FareEstimateResponse estimate(FareEstimateRequest request) {
        FareBreakdown breakdown = fareCalculator.calculate(request.distanceKm(), request.durationMinutes());
        return new FareEstimateResponse(
                request.pickupLocation(),
                request.dropoffLocation(),
                request.distanceKm(),
                request.durationMinutes(),
                breakdown.baseFare(),
                breakdown.distanceFare(),
                breakdown.timeFare(),
                breakdown.total(),
                breakdown.currency());
    }

    /** Calculates and stores the final fare of a completed ride. One fare per ride. */
    public FareResponse calculateFinalFare(CalculateFareRequest request) {
        if (fareRepository.existsByRideId(request.rideId())) {
            throw new DuplicateFareException(request.rideId());
        }
        FareBreakdown breakdown = fareCalculator.calculate(request.distanceKm(), request.durationMinutes());
        Instant now = Instant.now();

        Fare fare = new Fare();
        fare.setRideId(request.rideId());
        fare.setPassengerAccountId(request.passengerAccountId());
        fare.setDistanceKm(request.distanceKm());
        fare.setDurationMinutes(request.durationMinutes());
        fare.setBaseFare(breakdown.baseFare());
        fare.setDistanceFare(breakdown.distanceFare());
        fare.setTimeFare(breakdown.timeFare());
        fare.setTotalAmount(breakdown.total());
        fare.setCurrency(breakdown.currency());
        fare.setCreatedAt(now);
        fare.setUpdatedAt(now);

        return toResponse(fareRepository.save(fare));
    }

    public FareResponse getById(String fareId) {
        return toResponse(findFare(fareId));
    }

    public FareResponse getByRideId(String rideId) {
        return toResponse(fareRepository.findByRideId(rideId)
                .orElseThrow(() -> FareNotFoundException.forRide(rideId)));
    }

    /** Returns the stored fare entity; keeps fare lookups in one place. */
    public Fare findFare(String fareId) {
        return fareRepository.findById(fareId)
                .orElseThrow(() -> new FareNotFoundException(fareId));
    }

    private FareResponse toResponse(Fare fare) {
        return new FareResponse(
                fare.getId(),
                fare.getRideId(),
                fare.getPassengerAccountId(),
                fare.getDistanceKm(),
                fare.getDurationMinutes(),
                fare.getBaseFare(),
                fare.getDistanceFare(),
                fare.getTimeFare(),
                fare.getTotalAmount(),
                fare.getCurrency(),
                fare.getCreatedAt(),
                fare.getUpdatedAt());
    }
}
