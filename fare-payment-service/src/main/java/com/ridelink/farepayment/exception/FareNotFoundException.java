package com.ridelink.farepayment.exception;

public class FareNotFoundException extends RuntimeException {
    public FareNotFoundException(String fareId) {
        super("Fare not found: " + fareId);
    }

    private FareNotFoundException(String subject, String id) {
        super("Fare not found for " + subject + ": " + id);
    }

    public static FareNotFoundException forRide(String rideId) {
        return new FareNotFoundException("ride", rideId);
    }
}
