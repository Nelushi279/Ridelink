package com.ridelink.farepayment.repository;

import com.ridelink.farepayment.model.Payment;
import com.ridelink.farepayment.model.PaymentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String> {
    boolean existsByFareIdAndPaymentStatus(String fareId, PaymentStatus paymentStatus);
}
