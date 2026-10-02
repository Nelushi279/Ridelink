package com.ridelink.account.exception;

public class InvalidProfileException extends RuntimeException {

    public InvalidProfileException() {
        super("At least one profile field must be supplied");
    }
}
