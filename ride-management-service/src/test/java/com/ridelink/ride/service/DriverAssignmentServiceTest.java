package com.ridelink.ride.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ridelink.ride.client.AccountServiceClient;
import com.ridelink.ride.client.DriverVehicleServiceClient;
import com.ridelink.ride.dto.AssignDriverRequest;
import com.ridelink.ride.exception.DriverNotFoundException;
import com.ridelink.ride.exception.ExternalServiceUnavailableException;
import com.ridelink.ride.exception.InvalidDriverException;
import com.ridelink.ride.exception.InvalidRideStateException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideLocation;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import com.ridelink.ride.integration.dto.DriverEligibilityResponse;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DriverAssignmentServiceTest {
    private RideRepository repository;
    private AccountServiceClient accountServiceClient;
    private DriverVehicleServiceClient driverVehicleServiceClient;
    private RideService service;

    @BeforeEach
    void setUp() {
        repository = mock(RideRepository.class);
        accountServiceClient = mock(AccountServiceClient.class);
        driverVehicleServiceClient = mock(DriverVehicleServiceClient.class);
        when(driverVehicleServiceClient.getDriverEligibility("driver-123"))
                .thenReturn(new DriverEligibilityResponse(
                        "driver-123", "ACTIVE", "AVAILABLE", true, true));
        service = new RideService(repository, accountServiceClient, driverVehicleServiceClient);
    }

    @Test
    void requestedRideCanBeAssignedWithoutChangingExistingRideData() {
        Ride ride = rideWithStatus(RideStatus.REQUESTED);
        Instant originalUpdatedAt = ride.getUpdatedAt();
        when(repository.findById("ride-123")).thenReturn(Optional.of(ride));
        when(repository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.assignDriver("ride-123", new AssignDriverRequest("driver-123"));

        assertEquals("driver-123", response.driverId());
        assertEquals(RideStatus.ASSIGNED, response.status());
        assertNotNull(response.assignedAt());
        assertEquals(response.assignedAt(), response.updatedAt());
        assertTrue(response.updatedAt().isAfter(originalUpdatedAt));
        assertNull(response.acceptedAt());
        assertNull(response.startedAt());
        assertNull(response.completedAt());
        assertNull(response.cancelledAt());
        assertEquals("account-123", response.passengerAccountId());
        assertEquals(Instant.parse("2026-10-02T08:00:00Z"), response.requestedAt());
        assertEquals(Instant.parse("2026-10-02T08:00:00Z"), response.createdAt());
        assertEquals("University of Moratuwa", response.pickupLocation().address());
        assertEquals("Colombo Fort", response.dropoffLocation().address());
        var order = inOrder(repository, driverVehicleServiceClient);
        order.verify(repository).findById("ride-123");
        order.verify(driverVehicleServiceClient).getDriverEligibility("driver-123");
        order.verify(repository).save(ride);
    }

    @Test
    void ineligibleDriverLeavesRideUnchanged() {
        when(driverVehicleServiceClient.getDriverEligibility("driver-123"))
                .thenReturn(new DriverEligibilityResponse(
                        "driver-123", "ACTIVE", "UNAVAILABLE", true, false));

        assertFailedDriverValidationDoesNotMutateRide(InvalidDriverException.class);
    }

    @Test
    void missingDriverLeavesRideUnchanged() {
        when(driverVehicleServiceClient.getDriverEligibility("driver-123"))
                .thenThrow(new DriverNotFoundException("driver-123"));

        assertFailedDriverValidationDoesNotMutateRide(DriverNotFoundException.class);
    }

    @Test
    void unavailableDriverServiceLeavesRideUnchanged() {
        when(driverVehicleServiceClient.getDriverEligibility("driver-123"))
                .thenThrow(new ExternalServiceUnavailableException("Driver & Vehicle Service"));

        assertFailedDriverValidationDoesNotMutateRide(ExternalServiceUnavailableException.class);
    }

    @Test
    void unknownRideCannotBeAssigned() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(RideNotFoundException.class,
                () -> service.assignDriver("missing", new AssignDriverRequest("driver-123")));
        verify(repository, never()).save(any());
        verifyNoInteractions(driverVehicleServiceClient);
    }

    @ParameterizedTest(name = "{0} ride cannot be assigned")
    @EnumSource(value = RideStatus.class, names = "REQUESTED", mode = EnumSource.Mode.EXCLUDE)
    void rideOutsideRequestedStatusCannotBeAssigned(RideStatus status) {
        Ride ride = rideWithStatus(status);
        when(repository.findById("ride-123")).thenReturn(Optional.of(ride));

        InvalidRideStateException exception = assertThrows(InvalidRideStateException.class,
                () -> service.assignDriver("ride-123", new AssignDriverRequest("driver-123")));

        assertEquals("Ride cannot be assigned from status " + status, exception.getMessage());
        verify(repository, never()).save(any());
        verifyNoInteractions(driverVehicleServiceClient);
    }

    private void assertFailedDriverValidationDoesNotMutateRide(
            Class<? extends RuntimeException> exceptionType) {
        Ride ride = rideWithStatus(RideStatus.REQUESTED);
        Instant originalUpdatedAt = ride.getUpdatedAt();
        when(repository.findById("ride-123")).thenReturn(Optional.of(ride));

        assertThrows(exceptionType,
                () -> service.assignDriver("ride-123", new AssignDriverRequest("driver-123")));

        assertNull(ride.getDriverId());
        assertEquals(RideStatus.REQUESTED, ride.getStatus());
        assertNull(ride.getAssignedAt());
        assertEquals(originalUpdatedAt, ride.getUpdatedAt());
        verify(repository, never()).save(any());
    }

    private Ride rideWithStatus(RideStatus status) {
        Instant originalTime = Instant.parse("2026-10-02T08:00:00Z");
        Ride ride = new Ride();
        ride.setId("ride-123");
        ride.setPassengerAccountId("account-123");
        ride.setPickupLocation(new RideLocation("University of Moratuwa", 6.7969, 79.9018));
        ride.setDropoffLocation(new RideLocation("Colombo Fort", 6.9344, 79.8428));
        ride.setStatus(status);
        ride.setRequestedAt(originalTime);
        ride.setCreatedAt(originalTime);
        ride.setUpdatedAt(originalTime);
        return ride;
    }
}
