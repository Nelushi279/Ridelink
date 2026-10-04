package com.ridelink.farepayment.repository;

import com.ridelink.farepayment.model.Fare;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FareRepository extends MongoRepository<Fare, String> {

    Optional<Fare> findByRideId(String rideId);

    boolean existsByRideId(String rideId);
}
