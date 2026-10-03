package com.ridelink.drivervehicle.model;
import java.time.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
@Document(collection = "drivers")
public class Driver {
@Id
private String id;
public String getId() { return id; }
public void setId(String value) { this.id = value; }
@Indexed(unique = true)
private String accountId;
public String getAccountId() { return accountId; }
public void setAccountId(String value) { this.accountId = value; }
private String fullName;
public String getFullName() { return fullName; }
public void setFullName(String value) { this.fullName = value; }
private String phoneNumber;
public String getPhoneNumber() { return phoneNumber; }
public void setPhoneNumber(String value) { this.phoneNumber = value; }
@Indexed(unique = true)
private String licenseNumber;
public String getLicenseNumber() { return licenseNumber; }
public void setLicenseNumber(String value) { this.licenseNumber = value; }
private LocalDate licenseExpiryDate;
public LocalDate getLicenseExpiryDate() { return licenseExpiryDate; }
public void setLicenseExpiryDate(LocalDate value) { this.licenseExpiryDate = value; }
private String serviceArea;
public String getServiceArea() { return serviceArea; }
public void setServiceArea(String value) { this.serviceArea = value; }
private DriverStatus status;
public DriverStatus getStatus() { return status; }
public void setStatus(DriverStatus value) { this.status = value; }
private DriverAvailability availability = DriverAvailability.UNAVAILABLE;
public DriverAvailability getAvailability() {
    return availability == null ? DriverAvailability.UNAVAILABLE : availability;
}
public void setAvailability(DriverAvailability value) { this.availability = value; }
private Double latitude;
public Double getLatitude() { return latitude; }
public void setLatitude(Double value) { this.latitude = value; }
private Double longitude;
public Double getLongitude() { return longitude; }
public void setLongitude(Double value) { this.longitude = value; }
private Instant locationUpdatedAt;
public Instant getLocationUpdatedAt() { return locationUpdatedAt; }
public void setLocationUpdatedAt(Instant value) { this.locationUpdatedAt = value; }
private Instant createdAt;
public Instant getCreatedAt() { return createdAt; }
public void setCreatedAt(Instant value) { this.createdAt = value; }
private Instant updatedAt;
public Instant getUpdatedAt() { return updatedAt; }
public void setUpdatedAt(Instant value) { this.updatedAt = value; }
}