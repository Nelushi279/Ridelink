package com.ridelink.ride.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ridelink.ride.dto.LocationDto;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.*;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.service.RideService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RideController.class)
@Import(GlobalExceptionHandler.class)
class DriverAssignmentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideService rideService;

    @Test
    void requestedRideCanBeAssigned() throws Exception {
        when(rideService.assignDriver(eq("ride-123"), any())).thenReturn(assignedResponse());

        mockMvc.perform(patch("/api/rides/ride-123/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"driver-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.driverId").value("driver-123"))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.assignedAt").value("2026-10-02T08:05:00Z"));
    }

    @Test
    void blankDriverIdReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/rides/ride-123/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request data"))
                .andExpect(jsonPath("$.fieldErrors.driverId").value("must not be blank"));
        verifyNoInteractions(rideService);
    }

    @Test
    void unknownRideReturnsNotFound() throws Exception {
        when(rideService.assignDriver(eq("missing"), any()))
                .thenThrow(new RideNotFoundException("missing"));

        mockMvc.perform(patch("/api/rides/missing/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"driver-123\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Ride not found: missing"));
    }

    @Test
    void alreadyAssignedRideReturnsConflict() throws Exception {
        when(rideService.assignDriver(eq("ride-123"), any()))
                .thenThrow(new InvalidRideStateException(RideStatus.ASSIGNED));

        mockMvc.perform(patch("/api/rides/ride-123/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"driver-456\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Ride cannot be assigned from status ASSIGNED"));
    }

    @Test
    void missingDriverReturnsNotFound() throws Exception {
        when(rideService.assignDriver(eq("ride-123"), any()))
                .thenThrow(new DriverNotFoundException("driver-123"));

        mockMvc.perform(patch("/api/rides/ride-123/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"driver-123\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Driver not found: driver-123"));
    }

    @Test
    void ineligibleDriverReturnsConflict() throws Exception {
        when(rideService.assignDriver(eq("ride-123"), any()))
                .thenThrow(new InvalidDriverException());

        mockMvc.perform(patch("/api/rides/ride-123/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"driver-123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Driver is not eligible for assignment"));
    }

    @Test
    void unavailableDriverServiceReturnsServiceUnavailable() throws Exception {
        when(rideService.assignDriver(eq("ride-123"), any()))
                .thenThrow(new ExternalServiceUnavailableException("Driver & Vehicle Service"));

        mockMvc.perform(patch("/api/rides/ride-123/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"driverId\":\"driver-123\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message")
                        .value("Driver & Vehicle Service is unavailable"));
    }

    private RideResponse assignedResponse() {
        Instant requested = Instant.parse("2026-10-02T08:00:00Z");
        Instant assigned = Instant.parse("2026-10-02T08:05:00Z");
        return new RideResponse(
                "ride-123", "account-123", "driver-123",
                new LocationDto("University of Moratuwa", 6.7969, 79.9018),
                new LocationDto("Colombo Fort", 6.9344, 79.8428),
                RideStatus.ASSIGNED, requested, assigned, null, null, null, null,
                requested, assigned);
    }
}
