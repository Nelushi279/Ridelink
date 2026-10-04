package com.ridelink.ride.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ridelink.ride.client.AccountServiceClient;
import com.ridelink.ride.client.DriverVehicleServiceClient;
import com.ridelink.ride.exception.InvalidRideStateException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideLocation;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RideCancellationServiceTest {
    private static final Instant REQUESTED_AT = Instant.parse("2026-10-03T08:00:00Z");
    private static final Instant ASSIGNED_AT = Instant.parse("2026-10-03T08:05:00Z");
    private static final Instant ACCEPTED_AT = Instant.parse("2026-10-03T08:06:00Z");

    private RideRepository repository;
    private RideService service;

    @BeforeEach
    void setUp() {
        repository = mock(RideRepository.class);
        service = new RideService(repository, mock(AccountServiceClient.class),
                mock(DriverVehicleServiceClient.class));
    }

    @ParameterizedTest(name = "{0} ride can be cancelled")
    @EnumSource(value = RideStatus.class, names = {"REQUESTED", "ASSIGNED", "ACCEPTED"})
    void eligibleRideCanBeCancelledAndPreservesEarlierData(RideStatus status) {
        Ride ride = rideWithStatus(status);
        Instant previousUpdatedAt = ride.getUpdatedAt();
        Instant previousAssignedAt = ride.getAssignedAt();
        Instant previousAcceptedAt = ride.getAcceptedAt();
        when(repository.findById("ride-123")).thenReturn(Optional.of(ride));
        when(repository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.cancelRide("ride-123");

        assertEquals(RideStatus.CANCELLED, response.status());
        assertNotNull(response.cancelledAt());
        assertEquals(response.cancelledAt(), response.updatedAt());
        assertTrue(response.updatedAt().isAfter(previousUpdatedAt));
        assertEquals(REQUESTED_AT, response.requestedAt());
        assertEquals(previousAssignedAt, response.assignedAt());
        assertEquals(previousAcceptedAt, response.acceptedAt());
        assertNull(response.startedAt());
        assertNull(response.completedAt());
        assertEquals(REQUESTED_AT, response.createdAt());
        assertEquals("account-123", response.passengerAccountId());
        assertEquals("University of Moratuwa", response.pickupLocation().address());
        verify(repository).save(ride);
    }

    @ParameterizedTest(name = "{0} ride cannot be cancelled")
    @EnumSource(value = RideStatus.class, names = {"IN_PROGRESS", "COMPLETED", "CANCELLED"})
    void ineligibleRideCannotBeCancelled(RideStatus status) {
        when(repository.findById("ride-123")).thenReturn(Optional.of(rideWithStatus(status)));

        InvalidRideStateException exception = assertThrows(
                InvalidRideStateException.class, () -> service.cancelRide("ride-123"));

        assertEquals("Ride cannot be cancelled from status " + status, exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void unknownRideCannotBeCancelled() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(RideNotFoundException.class, () -> service.cancelRide("missing"));
        verify(repository, never()).save(any());
    }

    private Ride rideWithStatus(RideStatus status) {
        Ride ride = new Ride();
        ride.setId("ride-123");
        ride.setPassengerAccountId("account-123");
        ride.setDriverId(status == RideStatus.REQUESTED ? null : "driver-123");
        ride.setPickupLocation(new RideLocation("University of Moratuwa", 6.7969, 79.9018));
        ride.setDropoffLocation(new RideLocation("Colombo Fort", 6.9344, 79.8428));
        ride.setStatus(status);
        ride.setRequestedAt(REQUESTED_AT);
        if (status != RideStatus.REQUESTED) {
            ride.setAssignedAt(ASSIGNED_AT);
        }
        if (status == RideStatus.ACCEPTED || status == RideStatus.IN_PROGRESS
                || status == RideStatus.COMPLETED) {
            ride.setAcceptedAt(ACCEPTED_AT);
        }
        ride.setCreatedAt(REQUESTED_AT);
        ride.setUpdatedAt(status == RideStatus.ACCEPTED ? ACCEPTED_AT
                : status == RideStatus.REQUESTED ? REQUESTED_AT : ASSIGNED_AT);
        return ride;
    }
}
