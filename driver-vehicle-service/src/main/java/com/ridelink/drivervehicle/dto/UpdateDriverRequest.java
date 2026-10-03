package com.ridelink.drivervehicle.dto;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
public record UpdateDriverRequest(
 @Size(max=120) @Pattern(regexp="(?s).*\\S.*", message="must not be blank") String fullName,
 @Size(max=30) @Pattern(regexp="(?s).*\\S.*", message="must not be blank") String phoneNumber,
 @Size(max=50) @Pattern(regexp="(?s).*\\S.*", message="must not be blank") String licenseNumber,
 LocalDate licenseExpiryDate,
 @Size(max=120) @Pattern(regexp="(?s).*\\S.*", message="must not be blank") String serviceArea) {
 public boolean isEmpty() { return fullName==null && phoneNumber==null && licenseNumber==null && licenseExpiryDate==null && serviceArea==null; }
}