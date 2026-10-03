package com.ridelink.drivervehicle.controller;

import java.net.URI;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import com.ridelink.drivervehicle.dto.*;
import com.ridelink.drivervehicle.exception.ApiError;
import com.ridelink.drivervehicle.service.VehicleService;

@RestController
@RequestMapping("/api/vehicles")
@Tag(name = "Vehicle management")
@ApiResponses({
    @ApiResponse(responseCode = "400", description = "Invalid request data, malformed JSON, or empty PATCH",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "404", description = "Vehicle or driver not found",
        content = @Content(schema = @Schema(implementation = ApiError.class))),
    @ApiResponse(responseCode = "409", description = "Registration number already in use",
        content = @Content(schema = @Schema(implementation = ApiError.class)))
})
public class VehicleController {
    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Register a vehicle for an existing driver")
    @ApiResponse(responseCode = "201", description = "Vehicle created",
        content = @Content(schema = @Schema(implementation = VehicleResponse.class)))
    public ResponseEntity<VehicleResponse> create(@Valid @RequestBody CreateVehicleRequest request) {
        VehicleResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/api/vehicles/" + response.id())).body(response);
    }

    @GetMapping("/{vehicleId}")
    @Operation(summary = "Get a vehicle by ID")
    @ApiResponse(responseCode = "200", description = "Vehicle found",
        content = @Content(schema = @Schema(implementation = VehicleResponse.class)))
    public VehicleResponse get(@PathVariable String vehicleId) {
        return service.getById(vehicleId);
    }

    @GetMapping("/driver/{driverId}")
    @Operation(summary = "List vehicles belonging to a driver",
        description = "An existing driver with no vehicles returns an empty array. An unknown driver returns 404.")
    @ApiResponse(responseCode = "200", description = "Vehicle list, possibly empty",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = VehicleResponse.class))))
    public List<VehicleResponse> getByDriver(@PathVariable String driverId) {
        return service.getByDriverId(driverId);
    }

    @PatchMapping("/{vehicleId}")
    @Operation(summary = "Update vehicle details",
        description = "Omitted or null fields stay unchanged. Unknown and protected fields are rejected; id, driverId and timestamps are server-owned.")
    @ApiResponse(responseCode = "200", description = "Vehicle updated",
        content = @Content(schema = @Schema(implementation = VehicleResponse.class)))
    public VehicleResponse update(@PathVariable String vehicleId, @Valid @RequestBody UpdateVehicleRequest request) {
        return service.update(vehicleId, request);
    }
}
