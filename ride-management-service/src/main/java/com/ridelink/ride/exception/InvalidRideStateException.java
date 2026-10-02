package com.ridelink.ride.exception;

import com.ridelink.ride.model.RideStatus;

public class InvalidRideStateException extends RuntimeException {
    public InvalidRideStateException(RideStatus status) {
        super("Ride cannot be assigned from status " + status);
    }
}
