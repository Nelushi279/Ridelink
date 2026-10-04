package com.ridelink.ride.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ridelink.ride.exception.ExternalServiceUnavailableException;
import com.ridelink.ride.exception.PassengerAccountNotFoundException;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AccountServiceClientTest {
    private static final String BASE_URL = "http://account-service.test";

    private MockRestServiceServer server;
    private AccountServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AccountServiceClient(builder.build());
    }

    @Test
    void consumesExactAccountValidationContract() {
        server.expect(once(), requestTo(BASE_URL + "/api/accounts/account-123/validation"))
                .andRespond(withSuccess("""
                        {"accountId":"account-123","role":"PASSENGER","status":"ACTIVE"}
                        """, MediaType.APPLICATION_JSON));

        var response = client.getAccountValidation("account-123");

        assertEquals("account-123", response.accountId());
        assertEquals("PASSENGER", response.role());
        assertEquals("ACTIVE", response.status());
        server.verify();
    }

    @Test
    void mapsMissingAccountToNotFound() {
        server.expect(requestTo(BASE_URL + "/api/accounts/missing/validation"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(PassengerAccountNotFoundException.class,
                () -> client.getAccountValidation("missing"));
        server.verify();
    }

    @Test
    void mapsAccountServiceServerErrorToUnavailable() {
        server.expect(requestTo(BASE_URL + "/api/accounts/account-123/validation"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        ExternalServiceUnavailableException exception = assertThrows(
                ExternalServiceUnavailableException.class,
                () -> client.getAccountValidation("account-123"));

        assertEquals("Account Service is unavailable", exception.getMessage());
        server.verify();
    }

    @Test
    void mapsAccountServiceConnectionFailureToUnavailable() {
        server.expect(requestTo(BASE_URL + "/api/accounts/account-123/validation"))
                .andRespond(withException(new IOException("connection refused")));

        assertThrows(ExternalServiceUnavailableException.class,
                () -> client.getAccountValidation("account-123"));
        server.verify();
    }

    @Test
    void mapsIncompleteAccountResponseToUnavailable() {
        server.expect(requestTo(BASE_URL + "/api/accounts/account-123/validation"))
                .andRespond(withSuccess("""
                        {"accountId":"account-123","status":"ACTIVE"}
                        """, MediaType.APPLICATION_JSON));

        assertThrows(ExternalServiceUnavailableException.class,
                () -> client.getAccountValidation("account-123"));
        server.verify();
    }
}
