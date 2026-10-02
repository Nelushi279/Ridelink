package com.ridelink.ride.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "rides")
public class Ride {
    @Id
    private String id;
    private String passengerAccountId;
    private String driverId;
    private RideLocation pickupLocation;
    private RideLocation dropoffLocation;
    private RideStatus status;
    private Instant requestedAt;
    private Instant assignedAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String value) { id = value; }
    public String getPassengerAccountId() { return passengerAccountId; }
    public void setPassengerAccountId(String value) { passengerAccountId = value; }
    public String getDriverId() { return driverId; }
    public void setDriverId(String value) { driverId = value; }
    public RideLocation getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(RideLocation value) { pickupLocation = value; }
    public RideLocation getDropoffLocation() { return dropoffLocation; }
    public void setDropoffLocation(RideLocation value) { dropoffLocation = value; }
    public RideStatus getStatus() { return status; }
    public void setStatus(RideStatus value) { status = value; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant value) { requestedAt = value; }
    public Instant getAssignedAt() { return assignedAt; }
    public void setAssignedAt(Instant value) { assignedAt = value; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant value) { acceptedAt = value; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant value) { startedAt = value; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant value) { completedAt = value; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant value) { cancelledAt = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { updatedAt = value; }
}
