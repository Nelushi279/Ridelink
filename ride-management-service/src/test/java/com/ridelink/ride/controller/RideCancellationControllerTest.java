package com.ridelink.ride.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ridelink.ride.dto.LocationDto;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.GlobalExceptionHandler;
import com.ridelink.ride.exception.InvalidRideStateException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.service.RideService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RideController.class)
@Import(GlobalExceptionHandler.class)
class RideCancellationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideService rideService;

    @Test
    void eligibleRideCanBeCancelled() throws Exception {
        when(rideService.cancelRide("ride-123")).thenReturn(cancelledResponse());

        mockMvc.perform(patch("/api/rides/ride-123/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelledAt").value("2026-10-03T08:07:00Z"));
    }

    @Test
    void unknownRideReturnsNotFound() throws Exception {
        when(rideService.cancelRide("missing")).thenThrow(new RideNotFoundException("missing"));

        mockMvc.perform(patch("/api/rides/missing/cancel"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ride not found: missing"));
    }

    @Test
    void ineligibleRideReturnsConflict() throws Exception {
        when(rideService.cancelRide("ride-123"))
                .thenThrow(new InvalidRideStateException("cancelled", RideStatus.IN_PROGRESS));

        mockMvc.perform(patch("/api/rides/ride-123/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Ride cannot be cancelled from status IN_PROGRESS"));
    }

    private RideResponse cancelledResponse() {
        Instant requested = Instant.parse("2026-10-03T08:00:00Z");
        Instant assigned = Instant.parse("2026-10-03T08:05:00Z");
        Instant accepted = Instant.parse("2026-10-03T08:06:00Z");
        Instant cancelled = Instant.parse("2026-10-03T08:07:00Z");
        return new RideResponse(
                "ride-123", "account-123", "driver-123",
                new LocationDto("University of Moratuwa", 6.7969, 79.9018),
                new LocationDto("Colombo Fort", 6.9344, 79.8428),
                RideStatus.CANCELLED, requested, assigned, accepted, null, null, cancelled,
                requested, cancelled);
    }
}
