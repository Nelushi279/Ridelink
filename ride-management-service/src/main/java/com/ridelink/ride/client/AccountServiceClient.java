package com.ridelink.ride.client;

import com.ridelink.ride.exception.ExternalServiceUnavailableException;
import com.ridelink.ride.exception.PassengerAccountNotFoundException;
import com.ridelink.ride.integration.dto.AccountValidationResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AccountServiceClient {
    private final RestClient restClient;

    public AccountServiceClient(
            @Qualifier("accountServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public AccountValidationResponse getAccountValidation(String accountId) {
        try {
            AccountValidationResponse response = restClient.get()
                    .uri("/api/accounts/{accountId}/validation", accountId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, downstreamResponse) -> {
                        throw new PassengerAccountNotFoundException(accountId);
                    })
                    .onStatus(HttpStatusCode::isError, (request, downstreamResponse) -> {
                        throw new ExternalServiceUnavailableException("Account Service");
                    })
                    .body(AccountValidationResponse.class);

            if (response == null
                    || !accountId.equals(response.accountId())
                    || response.role() == null
                    || response.status() == null) {
                throw new ExternalServiceUnavailableException("Account Service");
            }
            return response;
        } catch (PassengerAccountNotFoundException | ExternalServiceUnavailableException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new ExternalServiceUnavailableException("Account Service", exception);
        }
    }
}
