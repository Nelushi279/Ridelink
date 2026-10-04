package com.ridelink.farepayment.exception;

public class DuplicateFareException extends RuntimeException {
    public DuplicateFareException(String rideId) {
        super("A fare already exists for ride: " + rideId);
    }
}
