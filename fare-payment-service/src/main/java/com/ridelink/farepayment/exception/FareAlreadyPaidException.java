package com.ridelink.farepayment.exception;

public class FareAlreadyPaidException extends RuntimeException {
    public FareAlreadyPaidException(String fareId) {
        super("Fare has already been paid: " + fareId);
    }
}
