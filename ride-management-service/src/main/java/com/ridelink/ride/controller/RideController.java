package com.ridelink.ride.controller;

import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.ApiError;
import com.ridelink.ride.service.RideService;
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
@RequestMapping("/api/rides")
@Tag(name = "Rides", description = "Create and retrieve ride requests")
public class RideController {
    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    @PostMapping
    @Operation(summary = "Create a ride request")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Ride request created"),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<RideResponse> createRide(@Valid @RequestBody CreateRideRequest request) {
        RideResponse response = rideService.createRide(request);
        return ResponseEntity.created(URI.create("/api/rides/" + response.id())).body(response);
    }

    @GetMapping("/{rideId}")
    @Operation(summary = "Get a ride by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ride found"),
        @ApiResponse(responseCode = "404", description = "Ride not found",
                content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public RideResponse getRide(@PathVariable String rideId) {
        return rideService.getRideById(rideId);
    }

    @GetMapping("/passenger/{passengerAccountId}")
    @Operation(summary = "Get all rides belonging to a passenger")
    @ApiResponse(responseCode = "200", description = "Passenger rides returned")
    public List<RideResponse> getPassengerRides(@PathVariable String passengerAccountId) {
        return rideService.getRidesByPassenger(passengerAccountId);
    }
}
