package com.ridelink.farepayment.controller;

import com.ridelink.farepayment.dto.CalculateFareRequest;
import com.ridelink.farepayment.dto.FareEstimateRequest;
import com.ridelink.farepayment.dto.FareEstimateResponse;
import com.ridelink.farepayment.dto.FareResponse;
import com.ridelink.farepayment.exception.ApiError;
import com.ridelink.farepayment.service.FareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fares")
@Tag(name = "Fares", description = "Fare estimation and final fare calculation")
public class FareController {
    private final FareService fareService;

    public FareController(FareService fareService) {
        this.fareService = fareService;
    }

    @PostMapping("/estimate")
    @Operation(summary = "Estimate a fare for a pickup and drop-off (not stored)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fare estimate returned"),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public FareEstimateResponse estimateFare(@Valid @RequestBody FareEstimateRequest request) {
        return fareService.estimate(request);
    }

    @PostMapping("/calculate")
    @Operation(summary = "Calculate and store the final fare of a ride")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Fare created"),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
                content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "409", description = "A fare already exists for the ride",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FareResponse> calculateFare(@Valid @RequestBody CalculateFareRequest request) {
        FareResponse response = fareService.calculateFinalFare(request);
        return ResponseEntity.created(URI.create("/api/fares/" + response.id())).body(response);
    }

    @GetMapping("/{fareId}")
    @Operation(summary = "Get a fare by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fare found"),
        @ApiResponse(responseCode = "404", description = "Fare not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public FareResponse getFare(@PathVariable String fareId) {
        return fareService.getById(fareId);
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get the fare of a ride")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fare found"),
        @ApiResponse(responseCode = "404", description = "No fare exists for the ride",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public FareResponse getFareByRide(@PathVariable String rideId) {
        return fareService.getByRideId(rideId);
    }
}
