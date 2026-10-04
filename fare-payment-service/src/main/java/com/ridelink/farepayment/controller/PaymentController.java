package com.ridelink.farepayment.controller;

import com.ridelink.farepayment.dto.CreatePaymentRequest;
import com.ridelink.farepayment.dto.PaymentResponse;
import com.ridelink.farepayment.dto.ReceiptResponse;
import com.ridelink.farepayment.exception.ApiError;
import com.ridelink.farepayment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Simulated payments and payment status")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Record a simulated payment for a fare")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Payment recorded as COMPLETED or FAILED"),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Fare not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Fare has already been paid",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.created(URI.create("/api/payments/" + response.id())).body(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get a payment and its status by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Payment found"),
        @ApiResponse(responseCode = "404", description = "Payment not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public PaymentResponse getPayment(@PathVariable String paymentId) {
        return paymentService.getById(paymentId);
    }

    @GetMapping("/{paymentId}/receipt")
    @Operation(summary = "Get the receipt of a completed payment")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Receipt returned"),
        @ApiResponse(responseCode = "404", description = "Payment not found",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "Payment is not COMPLETED",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ReceiptResponse getReceipt(@PathVariable String paymentId) {
        return paymentService.getReceipt(paymentId);
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get all payment attempts of a ride, newest first")
    @ApiResponse(responseCode = "200", description = "Ride payments returned")
    public List<PaymentResponse> getRidePayments(@PathVariable String rideId) {
        return paymentService.getByRideId(rideId);
    }

    @GetMapping("/passenger/{passengerAccountId}")
    @Operation(summary = "Get the payment history of a passenger, newest first")
    @ApiResponse(responseCode = "200", description = "Passenger payments returned")
    public List<PaymentResponse> getPassengerPayments(@PathVariable String passengerAccountId) {
        return paymentService.getByPassenger(passengerAccountId);
    }
}
