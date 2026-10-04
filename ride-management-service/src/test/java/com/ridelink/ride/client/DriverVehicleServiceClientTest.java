package com.ridelink.ride.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ridelink.ride.exception.DriverNotFoundException;
import com.ridelink.ride.exception.ExternalServiceUnavailableException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DriverVehicleServiceClientTest {
    private static final String BASE_URL = "http://driver-service.test";

    private MockRestServiceServer server;
    private DriverVehicleServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new DriverVehicleServiceClient(builder.build());
    }

    @Test
    void consumesExactDriverEligibilityContract() {
        server.expect(once(), requestTo(BASE_URL + "/api/drivers/driver-123/eligibility"))
                .andRespond(withSuccess("""
                        {"driverId":"driver-123","status":"ACTIVE","availability":"AVAILABLE",
                         "hasRegisteredVehicle":true,"eligible":true}
                        """, MediaType.APPLICATION_JSON));

        var response = client.getDriverEligibility("driver-123");

        assertEquals("driver-123", response.driverId());
        assertEquals("ACTIVE", response.status());
        assertEquals("AVAILABLE", response.availability());
        assertTrue(response.hasRegisteredVehicle());
        assertTrue(response.eligible());
        server.verify();
    }

    @Test
    void mapsMissingDriverToNotFound() {
        server.expect(requestTo(BASE_URL + "/api/drivers/missing/eligibility"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(DriverNotFoundException.class,
                () -> client.getDriverEligibility("missing"));
        server.verify();
    }

    @Test
    void mapsDriverServiceServerErrorToUnavailable() {
        server.expect(requestTo(BASE_URL + "/api/drivers/driver-123/eligibility"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        ExternalServiceUnavailableException exception = assertThrows(
                ExternalServiceUnavailableException.class,
                () -> client.getDriverEligibility("driver-123"));

        assertEquals("Driver & Vehicle Service is unavailable", exception.getMessage());
        server.verify();
    }

    @Test
    void mapsDriverServiceConnectionFailureToUnavailable() {
        server.expect(requestTo(BASE_URL + "/api/drivers/driver-123/eligibility"))
                .andRespond(withException(new IOException("connection refused")));

        assertThrows(ExternalServiceUnavailableException.class,
                () -> client.getDriverEligibility("driver-123"));
        server.verify();
    }

    @Test
    void mapsIncompleteDriverResponseToUnavailable() {
        server.expect(requestTo(BASE_URL + "/api/drivers/driver-123/eligibility"))
                .andRespond(withSuccess("""
                        {"driverId":"driver-123","status":"ACTIVE","availability":"AVAILABLE",
                         "hasRegisteredVehicle":true}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(ExternalServiceUnavailableException.class,
                () -> client.getDriverEligibility("driver-123"));
        server.verify();
    }
}
