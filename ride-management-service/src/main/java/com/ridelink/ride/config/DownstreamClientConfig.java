package com.ridelink.ride.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class DownstreamClientConfig {
    @Bean
    @Qualifier("accountServiceRestClient")
    public RestClient accountServiceRestClient(
            RestClient.Builder builder,
            @Value("${app.services.account.base-url}") String baseUrl,
            @Value("${app.services.connect-timeout}") Duration connectTimeout,
            @Value("${app.services.read-timeout}") Duration readTimeout) {
        return buildClient(builder, baseUrl, connectTimeout, readTimeout);
    }

    @Bean
    @Qualifier("driverVehicleServiceRestClient")
    public RestClient driverVehicleServiceRestClient(
            RestClient.Builder builder,
            @Value("${app.services.driver-vehicle.base-url}") String baseUrl,
            @Value("${app.services.connect-timeout}") Duration connectTimeout,
            @Value("${app.services.read-timeout}") Duration readTimeout) {
        return buildClient(builder, baseUrl, connectTimeout, readTimeout);
    }

    private RestClient buildClient(
            RestClient.Builder builder,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return builder.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
