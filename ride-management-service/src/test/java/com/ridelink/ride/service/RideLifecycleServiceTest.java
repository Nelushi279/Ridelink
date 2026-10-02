package com.ridelink.ride.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

class RideLifecycleServiceTest {
    private static final Instant REQUESTED_AT = Instant.parse("2026-10-02T08:00:00Z");
    private static final Instant ASSIGNED_AT = Instant.parse("2026-10-02T08:05:00Z");
    private static final Instant ACCEPTED_AT = Instant.parse("2026-10-02T08:06:00Z");
    private static final Instant STARTED_AT = Instant.parse("2026-10-02T08:10:00Z");

    private RideRepository repository;
    private RideService service;

    @BeforeEach
    void setUp() {
        repository = mock(RideRepository.class);
        service = new RideService(repository);
    }

    @Test
    void assignedRideCanBeAccepted() {
        Ride ride = rideWithStatus(RideStatus.ASSIGNED);
        when(repository.findById("ride-123")).thenReturn(Optional.of(ride));
        saveReturnsArgument();

        var response = service.acceptRide("ride-123");

        assertEquals(RideStatus.ACCEPTED, response.status());
        assertNotNull(response.acceptedAt());
        assertEquals(response.acceptedAt(), response.updatedAt());
        assertTrue(response.updatedAt().isAfter(ASSIGNED_AT));
        assertEquals(ASSIGNED_AT, response.assignedAt());
        assertNull(response.startedAt());
        assertNull(response.completedAt());
        assertNull(response.cancelledAt());
        assertUnchangedOwnershipAndRequestData(response.passengerAccountId(), response.driverId(),
                response.requestedAt(), response.createdAt());
    }

    @Test
    void acceptedRideCanBeStartedAndPreservesEarlierTimestamps() {
        Ride ride = rideWithStatus(RideStatus.ACCEPTED);
        when(repository.findById("ride-123")).thenReturn(Optional.of(ride));
        saveReturnsArgument();

        var response = service.startRide("ride-123");

        assertEquals(RideStatus.IN_PROGRESS, response.status());
        assertNotNull(response.startedAt());
        assertEquals(response.startedAt(), response.updatedAt());
        assertEquals(ACCEPTED_AT, response.acceptedAt());
        assertEquals(ASSIGNED_AT, response.assignedAt());
        assertNull(response.completedAt());
        assertNull(response.cancelledAt());
    }

    @Test
    void inProgressRideCanBeCompletedAndPreservesEarlierTimestamps() {
        Ride ride = rideWithStatus(RideStatus.IN_PROGRESS);
        when(repository.findById("ride-123")).thenReturn(Optional.of(ride));
        saveReturnsArgument();

        var response = service.completeRide("ride-123");

        assertEquals(RideStatus.COMPLETED, response.status());
        assertNotNull(response.completedAt());
        assertEquals(response.completedAt(), response.updatedAt());
        assertEquals(STARTED_AT, response.startedAt());
        assertEquals(ACCEPTED_AT, response.acceptedAt());
        assertEquals(ASSIGNED_AT, response.assignedAt());
        assertNull(response.cancelledAt());
    }

    @ParameterizedTest(name = "{0} ride cannot be accepted")
    @EnumSource(value = RideStatus.class, names = "ASSIGNED", mode = EnumSource.Mode.EXCLUDE)
    void onlyAssignedRideCanBeAccepted(RideStatus status) {
        assertInvalidTransition(status, "accepted", () -> service.acceptRide("ride-123"));
    }

    @ParameterizedTest(name = "{0} ride cannot be started")
    @EnumSource(value = RideStatus.class, names = "ACCEPTED", mode = EnumSource.Mode.EXCLUDE)
    void onlyAcceptedRideCanBeStarted(RideStatus status) {
        assertInvalidTransition(status, "started", () -> service.startRide("ride-123"));
    }

    @ParameterizedTest(name = "{0} ride cannot be completed")
    @EnumSource(value = RideStatus.class, names = "IN_PROGRESS", mode = EnumSource.Mode.EXCLUDE)
    void onlyInProgressRideCanBeCompleted(RideStatus status) {
        assertInvalidTransition(status, "completed", () -> service.completeRide("ride-123"));
    }

    @Test
    void unknownRideCannotBeAccepted() {
        assertUnknownRide(() -> service.acceptRide("missing"));
    }

    @Test
    void unknownRideCannotBeStarted() {
        assertUnknownRide(() -> service.startRide("missing"));
    }

    @Test
    void unknownRideCannotBeCompleted() {
        assertUnknownRide(() -> service.completeRide("missing"));
    }

    private void assertInvalidTransition(RideStatus status, String action, Runnable transition) {
        when(repository.findById("ride-123")).thenReturn(Optional.of(rideWithStatus(status)));

        InvalidRideStateException exception = assertThrows(InvalidRideStateException.class, transition::run);

        assertEquals("Ride cannot be " + action + " from status " + status, exception.getMessage());
        verify(repository, never()).save(any());
    }

    private void assertUnknownRide(Runnable transition) {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RideNotFoundException.class, transition::run);
        verify(repository, never()).save(any());
    }

    private void saveReturnsArgument() {
        when(repository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Ride rideWithStatus(RideStatus status) {
        Ride ride = new Ride();
        ride.setId("ride-123");
        ride.setPassengerAccountId("account-123");
        ride.setDriverId("driver-123");
        ride.setPickupLocation(new RideLocation("University of Moratuwa", 6.7969, 79.9018));
        ride.setDropoffLocation(new RideLocation("Colombo Fort", 6.9344, 79.8428));
        ride.setStatus(status);
        ride.setRequestedAt(REQUESTED_AT);
        ride.setAssignedAt(ASSIGNED_AT);
        if (status == RideStatus.ACCEPTED || status == RideStatus.IN_PROGRESS
                || status == RideStatus.COMPLETED) {
            ride.setAcceptedAt(ACCEPTED_AT);
        }
        if (status == RideStatus.IN_PROGRESS || status == RideStatus.COMPLETED) {
            ride.setStartedAt(STARTED_AT);
        }
        ride.setCreatedAt(REQUESTED_AT);
        ride.setUpdatedAt(ASSIGNED_AT);
        return ride;
    }

    private void assertUnchangedOwnershipAndRequestData(
            String passengerId, String driverId, Instant requestedAt, Instant createdAt) {
        assertEquals("account-123", passengerId);
        assertEquals("driver-123", driverId);
        assertEquals(REQUESTED_AT, requestedAt);
        assertEquals(REQUESTED_AT, createdAt);
    }
}
