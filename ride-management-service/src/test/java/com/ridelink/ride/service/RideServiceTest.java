package com.ridelink.ride.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.LocationDto;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideLocation;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RideServiceTest {
    private RideRepository repository;
    private RideService service;

    @BeforeEach
    void setUp() {
        repository = mock(RideRepository.class);
        service = new RideService(repository);
    }

    @Test
    void createsRequestedRideWithoutDriverOrLifecycleTimestamps() {
        when(repository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            ride.setId("ride-123");
            return ride;
        });

        var response = service.createRide(request());
        ArgumentCaptor<Ride> captor = ArgumentCaptor.forClass(Ride.class);
        verify(repository).save(captor.capture());
        Ride saved = captor.getValue();

        assertEquals("ride-123", response.id());
        assertEquals(RideStatus.REQUESTED, saved.getStatus());
        assertNull(saved.getDriverId());
        assertNull(saved.getAssignedAt());
        assertNull(saved.getAcceptedAt());
        assertNull(saved.getStartedAt());
        assertNull(saved.getCompletedAt());
        assertNull(saved.getCancelledAt());
    }

    @Test
    void creationPopulatesConsistentTimestamps() {
        when(repository.save(any(Ride.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createRide(request());
        ArgumentCaptor<Ride> captor = ArgumentCaptor.forClass(Ride.class);
        verify(repository).save(captor.capture());
        Ride saved = captor.getValue();

        assertNotNull(saved.getRequestedAt());
        assertEquals(saved.getRequestedAt(), saved.getCreatedAt());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());
    }

    @Test
    void retrievesRideById() {
        when(repository.findById("ride-123")).thenReturn(Optional.of(ride("ride-123")));
        assertEquals("ride-123", service.getRideById("ride-123").id());
    }

    @Test
    void unknownRideThrowsNotFoundException() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThrows(RideNotFoundException.class, () -> service.getRideById("missing"));
    }

    @Test
    void retrievesRidesByPassenger() {
        when(repository.findByPassengerAccountId("account-123"))
                .thenReturn(List.of(ride("ride-1"), ride("ride-2")));
        assertEquals(2, service.getRidesByPassenger("account-123").size());
    }

    @Test
    void passengerWithoutRidesReturnsEmptyList() {
        when(repository.findByPassengerAccountId("account-none")).thenReturn(List.of());
        assertTrue(service.getRidesByPassenger("account-none").isEmpty());
    }

    private CreateRideRequest request() {
        return new CreateRideRequest("account-123",
                new LocationDto("University of Moratuwa", 6.7969, 79.9018),
                new LocationDto("Colombo Fort", 6.9344, 79.8428));
    }

    private Ride ride(String id) {
        Ride ride = new Ride();
        ride.setId(id);
        ride.setPassengerAccountId("account-123");
        ride.setPickupLocation(new RideLocation("University of Moratuwa", 6.7969, 79.9018));
        ride.setDropoffLocation(new RideLocation("Colombo Fort", 6.9344, 79.8428));
        ride.setStatus(RideStatus.REQUESTED);
        Instant now = Instant.parse("2026-10-02T08:00:00Z");
        ride.setRequestedAt(now);
        ride.setCreatedAt(now);
        ride.setUpdatedAt(now);
        return ride;
    }
}
