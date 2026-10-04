package com.ridelink.ride.exception;

public class InvalidDriverException extends RuntimeException {
    public InvalidDriverException() {
        super("Driver is not eligible for assignment");
    }
}
