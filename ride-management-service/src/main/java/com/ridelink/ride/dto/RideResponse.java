package com.ridelink.ride.dto;

import com.ridelink.ride.model.RideStatus;
import java.time.Instant;

public record RideResponse(
        String id,
        String passengerAccountId,
        String driverId,
        LocationDto pickupLocation,
        LocationDto dropoffLocation,
        RideStatus status,
        Instant requestedAt,
        Instant assignedAt,
        Instant acceptedAt,
        Instant startedAt,
        Instant completedAt,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt) {
}
