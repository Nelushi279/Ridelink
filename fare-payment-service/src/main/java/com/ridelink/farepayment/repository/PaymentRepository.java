package com.ridelink.farepayment.repository;

import com.ridelink.farepayment.model.Payment;
import com.ridelink.farepayment.model.PaymentStatus;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    boolean existsByFareIdAndPaymentStatus(String fareId, PaymentStatus paymentStatus);

    List<Payment> findByRideIdOrderByCreatedAtDesc(String rideId);

    List<Payment> findByPassengerAccountIdOrderByCreatedAtDesc(String passengerAccountId);
}
