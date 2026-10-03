package com.ridelink.drivervehicle.repository;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.ridelink.drivervehicle.model.Driver;
public interface DriverRepository extends MongoRepository<Driver,String> {
 boolean existsByAccountId(String accountId);
 boolean existsByLicenseNumber(String licenseNumber);
 Optional<Driver> findByAccountId(String accountId);
}