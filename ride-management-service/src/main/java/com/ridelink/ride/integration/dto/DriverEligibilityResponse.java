package com.ridelink.ride.integration.dto;

public record DriverEligibilityResponse(
        String driverId,
        String status,
        String availability,
        Boolean hasRegisteredVehicle,
        Boolean eligible) {
}
