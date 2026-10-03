package com.ridelink.drivervehicle.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "vehicles")
public class Vehicle {
    @Id
    private String id;
    @Indexed
    private String driverId;
    @Indexed(unique = true)
    private String registrationNumber;
    private String make;
    private String model;
    private Integer manufactureYear;
    private String color;
    private VehicleType vehicleType;
    private Integer seatCapacity;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String value) { this.id = value; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String value) { this.driverId = value; }

    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String value) { this.registrationNumber = value; }

    public String getMake() { return make; }
    public void setMake(String value) { this.make = value; }

    public String getModel() { return model; }
    public void setModel(String value) { this.model = value; }

    public Integer getManufactureYear() { return manufactureYear; }
    public void setManufactureYear(Integer value) { this.manufactureYear = value; }

    public String getColor() { return color; }
    public void setColor(String value) { this.color = value; }

    public VehicleType getVehicleType() { return vehicleType; }
    public void setVehicleType(VehicleType value) { this.vehicleType = value; }

    public Integer getSeatCapacity() { return seatCapacity; }
    public void setSeatCapacity(Integer value) { this.seatCapacity = value; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { this.updatedAt = value; }
}
