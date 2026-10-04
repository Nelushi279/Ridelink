package com.ridelink.farepayment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ridelink.farepayment.dto.PaymentResponse;
import com.ridelink.farepayment.dto.ReceiptResponse;
import com.ridelink.farepayment.exception.FareAlreadyPaidException;
import com.ridelink.farepayment.exception.FareNotFoundException;
import com.ridelink.farepayment.exception.GlobalExceptionHandler;
import com.ridelink.farepayment.exception.PaymentNotFoundException;
import com.ridelink.farepayment.exception.ReceiptNotAvailableException;
import com.ridelink.farepayment.model.PaymentMethod;
import com.ridelink.farepayment.model.PaymentStatus;
import com.ridelink.farepayment.service.PaymentService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {
    private static final String VALID = "{\"fareId\":\"fare-1\",\"paymentMethod\":\"CARD\"}";
    private static final Instant NOW = Instant.parse("2026-10-04T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void createsPaymentWithLocation() throws Exception {
        when(paymentService.createPayment(any())).thenReturn(response(PaymentStatus.COMPLETED));

        mockMvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/payments/pay-1"))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(1050.00));
    }

    @Test
    void failedSimulatedPaymentIsStillRecorded() throws Exception {
        when(paymentService.createPayment(any())).thenReturn(response(PaymentStatus.FAILED));

        mockMvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fareId\":\"fare-1\",\"paymentMethod\":\"CARD\",\"simulateFailure\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentStatus").value("FAILED"));
    }

    @Test
    void missingFieldsAreBadRequest() throws Exception {
        mockMvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fareId").exists())
                .andExpect(jsonPath("$.fieldErrors.paymentMethod").exists());
    }

    @Test
    void unsupportedPaymentMethodIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fareId\":\"fare-1\",\"paymentMethod\":\"BITCOIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unknownFareIsNotFound() throws Exception {
        when(paymentService.createPayment(any())).thenThrow(new FareNotFoundException("fare-1"));

        mockMvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Fare not found: fare-1"));
    }

    @Test
    void alreadyPaidFareIsConflict() throws Exception {
        when(paymentService.createPayment(any())).thenThrow(new FareAlreadyPaidException("fare-1"));

        mockMvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getsPaymentById() throws Exception {
        when(paymentService.getById("pay-1")).thenReturn(response(PaymentStatus.COMPLETED));

        mockMvc.perform(get("/api/payments/pay-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pay-1"));
    }

    @Test
    void unknownPaymentIsNotFound() throws Exception {
        when(paymentService.getById("missing")).thenThrow(new PaymentNotFoundException("missing"));

        mockMvc.perform(get("/api/payments/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment not found: missing"));
    }

    @Test
    void getsReceiptOfCompletedPayment() throws Exception {
        when(paymentService.getReceipt("pay-1")).thenReturn(new ReceiptResponse("RCP-ABC", "pay-1", "ride-1",
                "fare-1", "acc-1", 10, 20, new BigDecimal("150.00"), new BigDecimal("800.00"),
                new BigDecimal("100.00"), new BigDecimal("1050.00"), "LKR", PaymentMethod.CARD, "TXN-ABC", NOW));

        mockMvc.perform(get("/api/payments/pay-1/receipt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").value("RCP-ABC"))
                .andExpect(jsonPath("$.totalAmount").value(1050.00));
    }

    @Test
    void receiptOfFailedPaymentIsConflict() throws Exception {
        when(paymentService.getReceipt("pay-1")).thenThrow(new ReceiptNotAvailableException(PaymentStatus.FAILED));

        mockMvc.perform(get("/api/payments/pay-1/receipt"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getsRidePayments() throws Exception {
        when(paymentService.getByRideId("ride-1")).thenReturn(List.of(response(PaymentStatus.COMPLETED)));

        mockMvc.perform(get("/api/payments/ride/ride-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rideId").value("ride-1"));
    }

    @Test
    void passengerWithoutPaymentsGetsEmptyList() throws Exception {
        when(paymentService.getByPassenger("acc-9")).thenReturn(List.of());

        mockMvc.perform(get("/api/payments/passenger/acc-9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private PaymentResponse response(PaymentStatus status) {
        boolean completed = status == PaymentStatus.COMPLETED;
        return new PaymentResponse("pay-1", "ride-1", "fare-1", "acc-1", new BigDecimal("1050.00"), "LKR",
                PaymentMethod.CARD, status, "TXN-ABC", completed ? "RCP-ABC" : null,
                completed ? null : "Simulated payment declined", NOW, NOW, completed ? NOW : null);
    }
}
