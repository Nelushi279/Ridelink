package com.ridelink.drivervehicle.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.ridelink.drivervehicle.model.Vehicle;

public interface VehicleRepository extends MongoRepository<Vehicle, String> {
    boolean existsByRegistrationNumber(String registrationNumber);
    List<Vehicle> findByDriverId(String driverId);
}
