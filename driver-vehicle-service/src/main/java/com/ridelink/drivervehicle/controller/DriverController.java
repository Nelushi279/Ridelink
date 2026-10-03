package com.ridelink.drivervehicle.controller;
import java.net.URI;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.media.*;
import com.ridelink.drivervehicle.dto.*;
import com.ridelink.drivervehicle.exception.ApiError;
import com.ridelink.drivervehicle.service.DriverService;
@RestController
@RequestMapping("/api/drivers")
@Tag(name="Driver profiles")
@ApiResponses({
 @ApiResponse(responseCode="400",description="Invalid request",content=@Content(schema=@Schema(implementation=ApiError.class))),
 @ApiResponse(responseCode="404",description="Driver not found",content=@Content(schema=@Schema(implementation=ApiError.class))),
 @ApiResponse(responseCode="409",description="Duplicate account or license",content=@Content(schema=@Schema(implementation=ApiError.class)))
})
public class DriverController {
 private final DriverService service;
 public DriverController(DriverService service) { this.service=service; }
 @PostMapping
 @Operation(summary="Create a PENDING driver profile")
 @ApiResponse(responseCode="201",description="Driver created",content=@Content(schema=@Schema(implementation=DriverResponse.class)))
 public ResponseEntity<DriverResponse> create(@Valid @RequestBody CreateDriverRequest request) {
  DriverResponse response=service.create(request);
  return ResponseEntity.created(URI.create("/api/drivers/"+response.id())).body(response);
 }
 @GetMapping("/{driverId}")
 @Operation(summary="Get a driver profile by driver ID")
 @ApiResponse(responseCode="200",description="Driver found")
 public DriverResponse get(@PathVariable String driverId) { return service.getById(driverId); }
 @GetMapping("/account/{accountId}")
 @Operation(summary="Get a driver profile by Account Service identity")
 @ApiResponse(responseCode="200",description="Driver found")
 public DriverResponse getByAccount(@PathVariable String accountId) { return service.getByAccountId(accountId); }
 @PatchMapping("/{driverId}")
 @Operation(summary="Update profile fields; null fields are omitted, protected and unknown fields are rejected")
 @ApiResponse(responseCode="200",description="Driver updated")
 public DriverResponse update(@PathVariable String driverId,@Valid @RequestBody UpdateDriverRequest request) { return service.update(driverId,request); }
}