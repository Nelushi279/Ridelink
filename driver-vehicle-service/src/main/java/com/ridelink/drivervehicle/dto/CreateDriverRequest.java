package com.ridelink.drivervehicle.dto;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
public record CreateDriverRequest(
 @NotBlank @Size(max=128) String accountId,
 @NotBlank @Size(max=120) String fullName,
 @NotBlank @Size(max=30) String phoneNumber,
 @NotBlank @Size(max=50) String licenseNumber,
 @NotNull LocalDate licenseExpiryDate,
 @NotBlank @Size(max=120) String serviceArea) {}