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
class RideLifecycleControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideService rideService;

    @Test
    void acceptsAssignedRide() throws Exception {
        when(rideService.acceptRide("ride-123")).thenReturn(response(RideStatus.ACCEPTED));
        mockMvc.perform(patch("/api/rides/ride-123/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.acceptedAt").exists());
    }

    @Test
    void startsAcceptedRide() throws Exception {
        when(rideService.startRide("ride-123")).thenReturn(response(RideStatus.IN_PROGRESS));
        mockMvc.perform(patch("/api/rides/ride-123/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedAt").exists());
    }

    @Test
    void completesInProgressRide() throws Exception {
        when(rideService.completeRide("ride-123")).thenReturn(response(RideStatus.COMPLETED));
        mockMvc.perform(patch("/api/rides/ride-123/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").exists());
    }

    @Test
    void unknownRideReturnsNotFoundForAccept() throws Exception {
        when(rideService.acceptRide("missing")).thenThrow(new RideNotFoundException("missing"));
        assertNotFound("/api/rides/missing/accept");
    }

    @Test
    void unknownRideReturnsNotFoundForStart() throws Exception {
        when(rideService.startRide("missing")).thenThrow(new RideNotFoundException("missing"));
        assertNotFound("/api/rides/missing/start");
    }

    @Test
    void unknownRideReturnsNotFoundForComplete() throws Exception {
        when(rideService.completeRide("missing")).thenThrow(new RideNotFoundException("missing"));
        assertNotFound("/api/rides/missing/complete");
    }

    @Test
    void invalidStatusReturnsConflictForAccept() throws Exception {
        when(rideService.acceptRide("ride-123"))
                .thenThrow(new InvalidRideStateException("accepted", RideStatus.REQUESTED));
        assertConflict("/api/rides/ride-123/accept", "Ride cannot be accepted from status REQUESTED");
    }

    @Test
    void invalidStatusReturnsConflictForStart() throws Exception {
        when(rideService.startRide("ride-123"))
                .thenThrow(new InvalidRideStateException("started", RideStatus.ASSIGNED));
        assertConflict("/api/rides/ride-123/start", "Ride cannot be started from status ASSIGNED");
    }

    @Test
    void invalidStatusReturnsConflictForComplete() throws Exception {
        when(rideService.completeRide("ride-123"))
                .thenThrow(new InvalidRideStateException("completed", RideStatus.ACCEPTED));
        assertConflict("/api/rides/ride-123/complete", "Ride cannot be completed from status ACCEPTED");
    }

    private void assertNotFound(String path) throws Exception {
        mockMvc.perform(patch(path))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ride not found: missing"));
    }

    private void assertConflict(String path, String message) throws Exception {
        mockMvc.perform(patch(path))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(message));
    }

    private RideResponse response(RideStatus status) {
        Instant requested = Instant.parse("2026-10-02T08:00:00Z");
        Instant assigned = Instant.parse("2026-10-02T08:05:00Z");
        Instant accepted = status == RideStatus.ACCEPTED || status == RideStatus.IN_PROGRESS
                || status == RideStatus.COMPLETED ? Instant.parse("2026-10-02T08:06:00Z") : null;
        Instant started = status == RideStatus.IN_PROGRESS || status == RideStatus.COMPLETED
                ? Instant.parse("2026-10-02T08:10:00Z") : null;
        Instant completed = status == RideStatus.COMPLETED
                ? Instant.parse("2026-10-02T08:30:00Z") : null;
        Instant updated = completed != null ? completed : started != null ? started : accepted;
        return new RideResponse(
                "ride-123", "account-123", "driver-123",
                new LocationDto("University of Moratuwa", 6.7969, 79.9018),
                new LocationDto("Colombo Fort", 6.9344, 79.8428),
                status, requested, assigned, accepted, started, completed, null, requested, updated);
    }
}
