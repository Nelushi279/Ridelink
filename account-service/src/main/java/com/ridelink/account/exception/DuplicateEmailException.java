package com.ridelink.account.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException() {
        super("An account with this email already exists");
    }

    public DuplicateEmailException(Throwable cause) {
        super("An account with this email already exists", cause);
    }
}
