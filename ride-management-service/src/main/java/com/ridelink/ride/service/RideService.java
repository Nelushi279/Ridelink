package com.ridelink.ride.service;

import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.LocationDto;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.model.Ride;
import com.ridelink.ride.model.RideLocation;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.repository.RideRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RideService {
    private final RideRepository rideRepository;

    public RideService(RideRepository rideRepository) {
        this.rideRepository = rideRepository;
    }

    public RideResponse createRide(CreateRideRequest request) {
        Instant now = Instant.now();
        Ride ride = new Ride();
        ride.setPassengerAccountId(request.passengerAccountId());
        ride.setPickupLocation(toModel(request.pickupLocation()));
        ride.setDropoffLocation(toModel(request.dropoffLocation()));
        ride.setStatus(RideStatus.REQUESTED);
        ride.setRequestedAt(now);
        ride.setCreatedAt(now);
        ride.setUpdatedAt(now);
        return toResponse(rideRepository.save(ride));
    }

    public RideResponse getRideById(String rideId) {
        return rideRepository.findById(rideId)
                .map(this::toResponse)
                .orElseThrow(() -> new RideNotFoundException(rideId));
    }

    public List<RideResponse> getRidesByPassenger(String passengerAccountId) {
        return rideRepository.findByPassengerAccountId(passengerAccountId).stream()
                .map(this::toResponse)
                .toList();
    }

    private RideLocation toModel(LocationDto location) {
        return new RideLocation(location.address(), location.latitude(), location.longitude());
    }

    private LocationDto toDto(RideLocation location) {
        return new LocationDto(location.getAddress(), location.getLatitude(), location.getLongitude());
    }

    private RideResponse toResponse(Ride ride) {
        return new RideResponse(
                ride.getId(), ride.getPassengerAccountId(), ride.getDriverId(),
                toDto(ride.getPickupLocation()), toDto(ride.getDropoffLocation()), ride.getStatus(),
                ride.getRequestedAt(), ride.getAssignedAt(), ride.getAcceptedAt(), ride.getStartedAt(),
                ride.getCompletedAt(), ride.getCancelledAt(), ride.getCreatedAt(), ride.getUpdatedAt());
    }
}
