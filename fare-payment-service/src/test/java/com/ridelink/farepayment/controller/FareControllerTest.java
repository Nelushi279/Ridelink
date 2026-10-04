package com.ridelink.farepayment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ridelink.farepayment.dto.FareEstimateResponse;
import com.ridelink.farepayment.dto.FareResponse;
import com.ridelink.farepayment.exception.DuplicateFareException;
import com.ridelink.farepayment.exception.FareNotFoundException;
import com.ridelink.farepayment.exception.GlobalExceptionHandler;
import com.ridelink.farepayment.service.FareService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FareController.class)
@Import(GlobalExceptionHandler.class)
class FareControllerTest {
    private static final String VALID =
            "{\"rideId\":\"ride-1\",\"passengerAccountId\":\"acc-1\",\"distanceKm\":10,\"durationMinutes\":20}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FareService fareService;

    @Test
    void estimateReturnsBreakdown() throws Exception {
        when(fareService.estimate(any())).thenReturn(new FareEstimateResponse("Malabe", "Colombo Fort", 10, 20,
                new BigDecimal("150.00"), new BigDecimal("800.00"), new BigDecimal("100.00"),
                new BigDecimal("1050.00"), "LKR"));

        mockMvc.perform(post("/api/fares/estimate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupLocation\":\"Malabe\",\"dropoffLocation\":\"Colombo Fort\","
                                + "\"distanceKm\":10,\"durationMinutes\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedTotal").value(1050.00))
                .andExpect(jsonPath("$.currency").value("LKR"));
    }

    @Test
    void estimateWithoutPickupIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/fares/estimate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dropoffLocation\":\"Colombo Fort\",\"distanceKm\":10,\"durationMinutes\":20}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.pickupLocation").exists());
    }

    @Test
    void calculateReturnsCreatedWithLocation() throws Exception {
        when(fareService.calculateFinalFare(any())).thenReturn(response());

        mockMvc.perform(post("/api/fares/calculate").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/fares/fare-1"))
                .andExpect(jsonPath("$.id").value("fare-1"))
                .andExpect(jsonPath("$.totalAmount").value(1050.00));
    }

    @Test
    void invalidInputIsBadRequestWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/fares/calculate").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID.replace("ride-1", " ").replace(":10,", ":-3,")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid request data"))
                .andExpect(jsonPath("$.fieldErrors.rideId").exists())
                .andExpect(jsonPath("$.fieldErrors.distanceKm").exists());
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/fares/calculate").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }

    @Test
    void duplicateFareIsConflict() throws Exception {
        when(fareService.calculateFinalFare(any())).thenThrow(new DuplicateFareException("ride-1"));

        mockMvc.perform(post("/api/fares/calculate").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getsFareById() throws Exception {
        when(fareService.getById("fare-1")).thenReturn(response());

        mockMvc.perform(get("/api/fares/fare-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rideId").value("ride-1"));
    }

    @Test
    void unknownFareIsNotFound() throws Exception {
        when(fareService.getById("missing")).thenThrow(new FareNotFoundException("missing"));

        mockMvc.perform(get("/api/fares/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Fare not found: missing"));
    }

    @Test
    void rideWithoutFareIsNotFound() throws Exception {
        when(fareService.getByRideId("ride-9")).thenThrow(FareNotFoundException.forRide("ride-9"));

        mockMvc.perform(get("/api/fares/ride/ride-9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private FareResponse response() {
        Instant now = Instant.parse("2026-10-04T08:00:00Z");
        return new FareResponse("fare-1", "ride-1", "acc-1", 10, 20, new BigDecimal("150.00"),
                new BigDecimal("800.00"), new BigDecimal("100.00"), new BigDecimal("1050.00"), "LKR", now, now);
    }
}
