package com.ridelink.drivervehicle.service;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;
import com.ridelink.drivervehicle.dto.*;
import com.ridelink.drivervehicle.model.*;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import com.ridelink.drivervehicle.exception.*;
@Service
public class DriverService {
 private final DriverRepository repository;
 private final VehicleRepository vehicles;
 public DriverService(DriverRepository repository, VehicleRepository vehicles) {
  this.repository=repository;
  this.vehicles=vehicles;
 }
 public DriverResponse create(CreateDriverRequest r) {
  validateExpiry(r.licenseExpiryDate());
  if(repository.existsByAccountId(r.accountId().trim())) throw new DuplicateDriverException("A driver profile already exists for this accountId");
  if(repository.existsByLicenseNumber(r.licenseNumber().trim())) throw new DuplicateDriverException("License number is already in use");
  Driver d=new Driver();
  d.setAccountId(r.accountId().trim()); d.setFullName(r.fullName().trim()); d.setPhoneNumber(r.phoneNumber().trim());
  d.setLicenseNumber(r.licenseNumber().trim()); d.setLicenseExpiryDate(r.licenseExpiryDate()); d.setServiceArea(r.serviceArea().trim());
  d.setStatus(DriverStatus.PENDING);
  d.setAvailability(DriverAvailability.UNAVAILABLE);
  Instant now=Instant.now(); d.setCreatedAt(now); d.setUpdatedAt(now);
  return response(save(d));
 }
 public DriverResponse getById(String id) { return response(find(id)); }
 public DriverResponse getByAccountId(String accountId) {
  return response(repository.findByAccountId(accountId).orElseThrow(() -> new DriverNotFoundException("Driver not found")));
 }
 public DriverResponse update(String id,UpdateDriverRequest r) {
  if(r.isEmpty()) throw new InvalidDriverProfileException("At least one profile field is required");
  Driver d=find(id);
  if(r.licenseExpiryDate()!=null) validateExpiry(r.licenseExpiryDate());
  if(r.licenseNumber()!=null && !r.licenseNumber().trim().equals(d.getLicenseNumber()) && repository.existsByLicenseNumber(r.licenseNumber().trim()))
   throw new DuplicateDriverException("License number is already in use");
  if(r.fullName()!=null) d.setFullName(r.fullName().trim());
  if(r.phoneNumber()!=null) d.setPhoneNumber(r.phoneNumber().trim());
  if(r.licenseNumber()!=null) d.setLicenseNumber(r.licenseNumber().trim());
  if(r.licenseExpiryDate()!=null) d.setLicenseExpiryDate(r.licenseExpiryDate());
  if(r.serviceArea()!=null) d.setServiceArea(r.serviceArea().trim());
  d.setUpdatedAt(Instant.now()); return response(save(d));
 }
 public DriverResponse updateStatus(String id, UpdateDriverStatusRequest request) {
  Driver driver = find(id);
  DriverStatus target = request.status();
  if (target == null) throw new InvalidDriverProfileException("Status must not be null");
  boolean forceUnavailable = target != DriverStatus.ACTIVE
      && driver.getAvailability() != DriverAvailability.UNAVAILABLE;
  if (target == driver.getStatus() && !forceUnavailable) return response(driver);
  if (target == DriverStatus.ACTIVE && !vehicles.existsByDriverId(driver.getId())) {
   throw new DriverActivationConflictException("Driver cannot be activated without a registered vehicle");
  }
  driver.setStatus(target);
  if (target != DriverStatus.ACTIVE) driver.setAvailability(DriverAvailability.UNAVAILABLE);
  driver.setUpdatedAt(Instant.now());
  return response(save(driver));
 }
 public DriverResponse updateAvailability(String id, UpdateDriverAvailabilityRequest request) {
  Driver driver = find(id);
  DriverAvailability target = request.availability();
  if (target == null) throw new InvalidDriverProfileException("Availability must not be null");
  if (target == DriverAvailability.AVAILABLE && driver.getStatus() != DriverStatus.ACTIVE) {
   throw new DriverAvailabilityConflictException("Only ACTIVE drivers can become available");
  }
  if (target == driver.getAvailability()) return response(driver);
  driver.setAvailability(target);
  driver.setUpdatedAt(Instant.now());
  return response(save(driver));
 }
 public DriverLocationResponse updateLocation(String id, UpdateDriverLocationRequest request) {
  Driver driver = find(id);
  if (driver.getStatus() != DriverStatus.ACTIVE || driver.getAvailability() != DriverAvailability.AVAILABLE) {
   throw new DriverLocationConflictException("Only ACTIVE and AVAILABLE drivers can update location");
  }
  driver.setLatitude(request.latitude());
  driver.setLongitude(request.longitude());
  Instant now = Instant.now();
  driver.setLocationUpdatedAt(now);
  driver.setUpdatedAt(now);
  return locationResponse(save(driver));
 }
 public DriverLocationResponse getLocation(String id) {
  Driver driver = find(id);
  if (driver.getLatitude() == null || driver.getLongitude() == null || driver.getLocationUpdatedAt() == null) {
   throw new DriverLocationNotAvailableException("Driver location not available");
  }
  return locationResponse(driver);
 }
 private DriverLocationResponse locationResponse(Driver driver) {
  return new DriverLocationResponse(driver.getId(), driver.getLatitude(), driver.getLongitude(), driver.getLocationUpdatedAt());
 }
 private Driver find(String id) { return repository.findById(id).orElseThrow(() -> new DriverNotFoundException("Driver not found")); }
 private void validateExpiry(LocalDate date) {
  if(date==null || date.isBefore(LocalDate.now(ZoneId.of("Asia/Colombo")))) throw new InvalidDriverProfileException("License expiry date must be today or later");
 }
 private Driver save(Driver d) {
  try { return repository.save(d); }
  catch(DuplicateKeyException e) { throw new DuplicateDriverException("Account ID or license number is already in use"); }
 }
 private DriverResponse response(Driver d) {
  return new DriverResponse(d.getId(),d.getAccountId(),d.getFullName(),d.getPhoneNumber(),d.getLicenseNumber(),d.getLicenseExpiryDate(),d.getServiceArea(),d.getStatus(),d.getAvailability(),d.getLatitude(),d.getLongitude(),d.getLocationUpdatedAt(),d.getCreatedAt(),d.getUpdatedAt());
 }
}