package com.ridelink.ride.exception;

public class InvalidPassengerException extends RuntimeException {
    public InvalidPassengerException() {
        super("Account must be an ACTIVE PASSENGER");
    }
}
