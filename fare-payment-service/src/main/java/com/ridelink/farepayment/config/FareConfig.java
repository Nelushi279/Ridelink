package com.ridelink.farepayment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FareProperties.class)
public class FareConfig {
}
