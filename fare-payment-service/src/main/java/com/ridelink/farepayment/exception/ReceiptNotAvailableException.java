package com.ridelink.farepayment.exception;

import com.ridelink.farepayment.model.PaymentStatus;

public class ReceiptNotAvailableException extends RuntimeException {
    public ReceiptNotAvailableException(PaymentStatus status) {
        super("Receipt is available only for completed payments. Current status: " + status);
    }
}
