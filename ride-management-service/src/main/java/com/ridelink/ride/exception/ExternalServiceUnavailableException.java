package com.ridelink.ride.exception;

public class ExternalServiceUnavailableException extends RuntimeException {
    public ExternalServiceUnavailableException(String serviceName) {
        super(serviceName + " is unavailable");
    }

    public ExternalServiceUnavailableException(String serviceName, Throwable cause) {
        super(serviceName + " is unavailable", cause);
    }
}
