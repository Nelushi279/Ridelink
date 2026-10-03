package com.ridelink.drivervehicle.controller;
import java.net.URI;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import com.ridelink.drivervehicle.dto.*;
import com.ridelink.drivervehicle.exception.ApiError;
import com.ridelink.drivervehicle.service.DriverService;
@RestController
@RequestMapping("/api/drivers")
@Tag(name="Driver profiles")
public class DriverController {
 private final DriverService service;
 public DriverController(DriverService service) { this.service=service; }
 @ApiResponses({
 @ApiResponse(responseCode="400",description="Invalid request",content=@Content(schema=@Schema(implementation=ApiError.class))),
 @ApiResponse(responseCode="404",description="Driver not found",content=@Content(schema=@Schema(implementation=ApiError.class))),
 @ApiResponse(responseCode="409",description="Duplicate account or license",content=@Content(schema=@Schema(implementation=ApiError.class)))
})
 @PostMapping
 @Operation(summary="Create a PENDING driver profile")
 @ApiResponse(responseCode="201",description="Driver created",content=@Content(schema=@Schema(implementation=DriverResponse.class)))
 public ResponseEntity<DriverResponse> create(@Valid @RequestBody CreateDriverRequest request) {
  DriverResponse response=service.create(request);
  return ResponseEntity.created(URI.create("/api/drivers/"+response.id())).body(response);
 }
 @ApiResponses({
 @ApiResponse(responseCode="400",description="Invalid request",content=@Content(schema=@Schema(implementation=ApiError.class))),
 @ApiResponse(responseCode="404",description="Driver not found",content=@Content(schema=@Schema(implementation=ApiError.class))),
 @ApiResponse(responseCode="409",description="Duplicate account or license",content=@Content(schema=@Schema(implementation=ApiError.class)))
})
 @GetMapping("/{driverId}")
 @Operation(summary="Get a driver profile by driver ID")
 @ApiResponse(responseCode="200",description="Driver found")
 public DriverResponse get(@PathVariable String driverId) { return service.getById(driverId); }
 @ApiResponses({
 @ApiResponse(responseCode="400",description="Invalid request",content=@Content(schema=@Schema(implementation=ApiError.class))),
 @ApiResponse(responseCode="404",description="Driver not found",content=@Content(schema=@Schema(implementation=ApiError.class))),
 @ApiResponse(responseCode="409",description="Duplicate account or license",content=@Content(schema=@Schema(implementation=ApiError.class)))
})
 @GetMapping("/account/{accountId}")
 @Operation(summary="Get a driver profile by Account Service identity")
 @ApiResponse(responseCode="200",description="Driver found")
 public DriverResponse getByAccount(@PathVariable String accountId) { return service.getByAccountId(accountId); }
 @ApiResponses({
 @ApiResponse(responseCode="400",description="Invalid request",content=@Content(schema=@Schema(implementation=ApiError.class))),
 @ApiResponse(responseCode="404",description="Driver not found",content=@Content(schema=@Schema(implementation=ApiError.class))),
 @ApiResponse(responseCode="409",description="Duplicate account or license",content=@Content(schema=@Schema(implementation=ApiError.class)))
})
 @PatchMapping("/{driverId}")
 @Operation(summary="Update profile fields; null fields are omitted, protected and unknown fields are rejected")
 @ApiResponse(responseCode="200",description="Driver updated")
 public DriverResponse update(@PathVariable String driverId,@Valid @RequestBody UpdateDriverRequest request) { return service.update(driverId,request); }
 @PatchMapping("/{driverId}/status")
 @Operation(summary = "Set driver status",
  description = "Supports PENDING, ACTIVE, INACTIVE and SUSPENDED. Becoming ACTIVE requires a registered vehicle. Non-ACTIVE statuses force UNAVAILABLE; activation does not opt into availability. Repeating a consistent current status returns the unchanged profile. Authentication and authorization are not implemented.")
 @ApiResponses({
  @ApiResponse(responseCode = "200", description = "Status updated or already current", content = @Content(schema = @Schema(implementation = DriverResponse.class))),
  @ApiResponse(responseCode = "400", description = "Missing, null or malformed status", content = @Content(schema = @Schema(implementation = ApiError.class))),
  @ApiResponse(responseCode = "404", description = "Driver not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
  @ApiResponse(responseCode = "409", description = "Activation requires a registered vehicle", content = @Content(schema = @Schema(implementation = ApiError.class)))
 })
 public DriverResponse updateStatus(
  @Parameter(description = "Driver profile ID", required = true) @PathVariable String driverId,
  @Valid @RequestBody UpdateDriverStatusRequest request) {
  return service.updateStatus(driverId, request);
 }
 @PatchMapping("/{driverId}/availability")
 @Operation(summary = "Set driver availability",
  description = "AVAILABLE requires ACTIVE status. UNAVAILABLE is allowed for any status. Repeating unchanged availability preserves timestamps. Authentication and authorization are not implemented.")
 @ApiResponses({
  @ApiResponse(responseCode = "200", description = "Availability updated or already current", content = @Content(schema = @Schema(implementation = DriverResponse.class))),
  @ApiResponse(responseCode = "400", description = "Missing, null or malformed availability", content = @Content(schema = @Schema(implementation = ApiError.class))),
  @ApiResponse(responseCode = "404", description = "Driver not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
  @ApiResponse(responseCode = "409", description = "Only ACTIVE drivers can become available", content = @Content(schema = @Schema(implementation = ApiError.class)))
 })
 public DriverResponse updateAvailability(
  @Parameter(description = "Driver profile ID", required = true) @PathVariable String driverId,
  @Valid @RequestBody UpdateDriverAvailabilityRequest request) {
  return service.updateAvailability(driverId, request);
 }
 @PatchMapping("/{driverId}/location")
 @Operation(summary = "Report current driver location",
  description = "Requires ACTIVE status and AVAILABLE availability. Stores only the latest coordinates and refreshes both timestamps. Authentication and authorization are not implemented.")
 @ApiResponses({
  @ApiResponse(responseCode = "200", description = "Location updated", content = @Content(schema = @Schema(implementation = DriverLocationResponse.class))),
  @ApiResponse(responseCode = "400", description = "Missing, invalid or malformed coordinates", content = @Content(schema = @Schema(implementation = ApiError.class))),
  @ApiResponse(responseCode = "404", description = "Driver not found", content = @Content(schema = @Schema(implementation = ApiError.class))),
  @ApiResponse(responseCode = "409", description = "Only ACTIVE and AVAILABLE drivers can update location", content = @Content(schema = @Schema(implementation = ApiError.class)))
 })
 public DriverLocationResponse updateLocation(
  @Parameter(description = "Driver profile ID", required = true) @PathVariable String driverId,
  @Valid @RequestBody UpdateDriverLocationRequest request) {
  return service.updateLocation(driverId, request);
 }
 @GetMapping("/{driverId}/location")
 @Operation(summary = "Get the last known driver location",
  description = "Returns 404 if the driver does not exist or has no reported location. Last known coordinates remain readable after status or availability changes.")
 @ApiResponses({
  @ApiResponse(responseCode = "200", description = "Last known location", content = @Content(schema = @Schema(implementation = DriverLocationResponse.class))),
  @ApiResponse(responseCode = "404", description = "Driver or reported location not found", content = @Content(schema = @Schema(implementation = ApiError.class)))
 })
 public DriverLocationResponse getLocation(
  @Parameter(description = "Driver profile ID", required = true) @PathVariable String driverId) {
  return service.getLocation(driverId);
 }
}