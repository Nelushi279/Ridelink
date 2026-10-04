package com.ridelink.farepayment.service;

import com.ridelink.farepayment.dto.CreatePaymentRequest;
import com.ridelink.farepayment.dto.PaymentResponse;
import com.ridelink.farepayment.dto.ReceiptResponse;
import com.ridelink.farepayment.exception.FareAlreadyPaidException;
import com.ridelink.farepayment.exception.PaymentNotFoundException;
import com.ridelink.farepayment.exception.ReceiptNotAvailableException;
import com.ridelink.farepayment.model.Fare;
import com.ridelink.farepayment.model.Payment;
import com.ridelink.farepayment.model.PaymentStatus;
import com.ridelink.farepayment.repository.PaymentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Simulated payments. No real gateway is called and no card or bank details are stored.
 * The amount always comes from the stored fare, never from the client.
 */
@Service
public class PaymentService {

    static final String SIMULATED_FAILURE_REASON = "Simulated payment declined";

    private final PaymentRepository paymentRepository;
    private final FareService fareService;

    public PaymentService(PaymentRepository paymentRepository, FareService fareService) {
        this.paymentRepository = paymentRepository;
        this.fareService = fareService;
    }

    /**
     * Records a payment attempt for a fare. The attempt starts as PENDING and the simulated
     * processor immediately settles it as COMPLETED or FAILED. A fare can be paid only once,
     * but a failed attempt may be retried.
     */
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        Fare fare = fareService.findFare(request.fareId());
        if (paymentRepository.existsByFareIdAndPaymentStatus(fare.getId(), PaymentStatus.COMPLETED)) {
            throw new FareAlreadyPaidException(fare.getId());
        }

        Instant now = Instant.now();
        Payment payment = new Payment();
        payment.setRideId(fare.getRideId());
        payment.setFareId(fare.getId());
        payment.setPassengerAccountId(fare.getPassengerAccountId());
        payment.setAmount(fare.getTotalAmount());
        payment.setCurrency(fare.getCurrency());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setTransactionReference(generateReference("TXN"));
        payment.setCreatedAt(now);

        settle(payment, Boolean.TRUE.equals(request.simulateFailure()), now);
        return toResponse(paymentRepository.save(payment));
    }

    public PaymentResponse getById(String paymentId) {
        return toResponse(findPayment(paymentId));
    }

    public List<PaymentResponse> getByRideId(String rideId) {
        return paymentRepository.findByRideIdOrderByCreatedAtDesc(rideId).stream().map(this::toResponse).toList();
    }

    public List<PaymentResponse> getByPassenger(String passengerAccountId) {
        return paymentRepository.findByPassengerAccountIdOrderByCreatedAtDesc(passengerAccountId).stream()
                .map(this::toResponse).toList();
    }

    /** A receipt exists only for a COMPLETED payment. */
    public ReceiptResponse getReceipt(String paymentId) {
        Payment payment = findPayment(paymentId);
        if (payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
            throw new ReceiptNotAvailableException(payment.getPaymentStatus());
        }
        Fare fare = fareService.findFare(payment.getFareId());
        return new ReceiptResponse(
                payment.getReceiptNumber(),
                payment.getId(),
                payment.getRideId(),
                payment.getFareId(),
                payment.getPassengerAccountId(),
                fare.getDistanceKm(),
                fare.getDurationMinutes(),
                fare.getBaseFare(),
                fare.getDistanceFare(),
                fare.getTimeFare(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getTransactionReference(),
                payment.getPaidAt());
    }

    private void settle(Payment payment, boolean simulateFailure, Instant now) {
        if (simulateFailure) {
            payment.setPaymentStatus(PaymentStatus.FAILED);
            payment.setFailureReason(SIMULATED_FAILURE_REASON);
        } else {
            payment.setPaymentStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(now);
            payment.setReceiptNumber(generateReference("RCP"));
        }
        payment.setUpdatedAt(now);
    }

    private Payment findPayment(String paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private String generateReference(String prefix) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        return prefix + "-" + token;
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getRideId(),
                payment.getFareId(),
                payment.getPassengerAccountId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaymentMethod(),
                payment.getPaymentStatus(),
                payment.getTransactionReference(),
                payment.getReceiptNumber(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getPaidAt());
    }
}
