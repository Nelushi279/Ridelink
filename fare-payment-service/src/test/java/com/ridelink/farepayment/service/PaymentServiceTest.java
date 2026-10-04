package com.ridelink.farepayment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ridelink.farepayment.dto.CreatePaymentRequest;
import com.ridelink.farepayment.dto.PaymentResponse;
import com.ridelink.farepayment.dto.ReceiptResponse;
import com.ridelink.farepayment.exception.FareAlreadyPaidException;
import com.ridelink.farepayment.exception.FareNotFoundException;
import com.ridelink.farepayment.exception.PaymentNotFoundException;
import com.ridelink.farepayment.exception.ReceiptNotAvailableException;
import com.ridelink.farepayment.model.Fare;
import com.ridelink.farepayment.model.Payment;
import com.ridelink.farepayment.model.PaymentMethod;
import com.ridelink.farepayment.model.PaymentStatus;
import com.ridelink.farepayment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private FareService fareService;
    @InjectMocks
    private PaymentService paymentService;

    private Fare fare;

    @BeforeEach
    void setUp() {
        fare = new Fare();
        fare.setId("fare-1");
        fare.setRideId("ride-1");
        fare.setPassengerAccountId("acc-1");
        fare.setDistanceKm(10);
        fare.setDurationMinutes(20);
        fare.setBaseFare(new BigDecimal("150.00"));
        fare.setDistanceFare(new BigDecimal("800.00"));
        fare.setTimeFare(new BigDecimal("100.00"));
        fare.setTotalAmount(new BigDecimal("1050.00"));
        fare.setCurrency("LKR");
    }

    private void stubSave() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            saved.setId("pay-1");
            return saved;
        });
    }

    @Test
    void successfulPaymentIsCompletedWithAmountTakenFromFare() {
        when(fareService.findFare("fare-1")).thenReturn(fare);
        when(paymentRepository.existsByFareIdAndPaymentStatus("fare-1", PaymentStatus.COMPLETED)).thenReturn(false);
        stubSave();

        PaymentResponse response =
                paymentService.createPayment(new CreatePaymentRequest("fare-1", PaymentMethod.CARD, null));

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(response.amount()).isEqualByComparingTo("1050.00");
        assertThat(response.rideId()).isEqualTo("ride-1");
        assertThat(response.passengerAccountId()).isEqualTo("acc-1");
        assertThat(response.transactionReference()).startsWith("TXN-");
        assertThat(response.receiptNumber()).startsWith("RCP-");
        assertThat(response.paidAt()).isNotNull();
        assertThat(response.failureReason()).isNull();
    }

    @Test
    void simulatedFailureIsRecordedAsFailedWithoutReceipt() {
        when(fareService.findFare("fare-1")).thenReturn(fare);
        when(paymentRepository.existsByFareIdAndPaymentStatus("fare-1", PaymentStatus.COMPLETED)).thenReturn(false);
        stubSave();

        PaymentResponse response =
                paymentService.createPayment(new CreatePaymentRequest("fare-1", PaymentMethod.CARD, true));

        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(response.failureReason()).isEqualTo(PaymentService.SIMULATED_FAILURE_REASON);
        assertThat(response.receiptNumber()).isNull();
        assertThat(response.paidAt()).isNull();
    }

    @Test
    void alreadyPaidFareIsRejected() {
        when(fareService.findFare("fare-1")).thenReturn(fare);
        when(paymentRepository.existsByFareIdAndPaymentStatus("fare-1", PaymentStatus.COMPLETED)).thenReturn(true);

        assertThatThrownBy(() ->
                paymentService.createPayment(new CreatePaymentRequest("fare-1", PaymentMethod.CASH, false)))
                .isInstanceOf(FareAlreadyPaidException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void paymentForUnknownFareIsRejected() {
        when(fareService.findFare("nope")).thenThrow(new FareNotFoundException("nope"));

        assertThatThrownBy(() ->
                paymentService.createPayment(new CreatePaymentRequest("nope", PaymentMethod.CASH, false)))
                .isInstanceOf(FareNotFoundException.class);
    }

    @Test
    void receiptIsBuiltFromCompletedPaymentAndFare() {
        Payment payment = payment(PaymentStatus.COMPLETED);
        payment.setReceiptNumber("RCP-ABC");
        payment.setPaidAt(Instant.now());
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(payment));
        when(fareService.findFare("fare-1")).thenReturn(fare);

        ReceiptResponse receipt = paymentService.getReceipt("pay-1");

        assertThat(receipt.receiptNumber()).isEqualTo("RCP-ABC");
        assertThat(receipt.totalAmount()).isEqualByComparingTo("1050.00");
        assertThat(receipt.distanceFare()).isEqualByComparingTo("800.00");
        assertThat(receipt.paymentMethod()).isEqualTo(PaymentMethod.CARD);
    }

    @Test
    void receiptIsNotAvailableForFailedPayment() {
        when(paymentRepository.findById("pay-1")).thenReturn(Optional.of(payment(PaymentStatus.FAILED)));

        assertThatThrownBy(() -> paymentService.getReceipt("pay-1")).isInstanceOf(ReceiptNotAvailableException.class);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(paymentRepository.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getById("nope")).isInstanceOf(PaymentNotFoundException.class);
    }

    private Payment payment(PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId("pay-1");
        payment.setFareId("fare-1");
        payment.setRideId("ride-1");
        payment.setPassengerAccountId("acc-1");
        payment.setAmount(new BigDecimal("1050.00"));
        payment.setCurrency("LKR");
        payment.setPaymentMethod(PaymentMethod.CARD);
        payment.setPaymentStatus(status);
        return payment;
    }
}
