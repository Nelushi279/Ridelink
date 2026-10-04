package com.ridelink.ride.exception;

public class PassengerAccountNotFoundException extends RuntimeException {
    public PassengerAccountNotFoundException(String accountId) {
        super("Passenger account not found: " + accountId);
    }
}
