package com.ridelink.farepayment.dto;

import com.ridelink.farepayment.model.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotBlank(message = "must not be blank") String fareId,
        @NotNull(message = "must not be null") PaymentMethod paymentMethod,
        @Schema(description = "Set to true to simulate a declined payment. Defaults to false.")
        Boolean simulateFailure) {
}
