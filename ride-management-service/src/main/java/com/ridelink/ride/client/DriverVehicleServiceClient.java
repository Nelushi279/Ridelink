package com.ridelink.ride.client;

import com.ridelink.ride.exception.DriverNotFoundException;
import com.ridelink.ride.exception.ExternalServiceUnavailableException;
import com.ridelink.ride.integration.dto.DriverEligibilityResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DriverVehicleServiceClient {
    private final RestClient restClient;

    public DriverVehicleServiceClient(
            @Qualifier("driverVehicleServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public DriverEligibilityResponse getDriverEligibility(String driverId) {
        try {
            DriverEligibilityResponse response = restClient.get()
                    .uri("/api/drivers/{driverId}/eligibility", driverId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, downstreamResponse) -> {
                        throw new DriverNotFoundException(driverId);
                    })
                    .onStatus(HttpStatusCode::isError, (request, downstreamResponse) -> {
                        throw new ExternalServiceUnavailableException("Driver & Vehicle Service");
                    })
                    .body(DriverEligibilityResponse.class);

            if (response == null
                    || !driverId.equals(response.driverId())
                    || response.status() == null
                    || response.availability() == null
                    || response.hasRegisteredVehicle() == null
                    || response.eligible() == null) {
                throw new ExternalServiceUnavailableException("Driver & Vehicle Service");
            }
            return response;
        } catch (DriverNotFoundException | ExternalServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ExternalServiceUnavailableException("Driver & Vehicle Service", exception);
        }
    }
}
