package com.ridelink.drivervehicle.dto;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.ridelink.drivervehicle.model.DriverAvailability;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateDriverAvailabilityRequest(
    @NotNull
    @Schema(description = "Target driver availability", example = "AVAILABLE")
    @JsonDeserialize(using = AvailabilityDeserializer.class)
    DriverAvailability availability
) {
    // Keep enum input strict for this endpoint, without changing existing APIs.
    public static class AvailabilityDeserializer extends StdDeserializer<DriverAvailability> {
        public AvailabilityDeserializer() { super(DriverAvailability.class); }

        @Override
        public DriverAvailability deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return context.reportInputMismatch(DriverAvailability.class, "Availability must be an enum name string");
            }
            String value = parser.getText();
            try {
                return DriverAvailability.valueOf(value);
            } catch (IllegalArgumentException exception) {
                throw context.weirdStringException(value, DriverAvailability.class, "Unsupported driver availability");
            }
        }
    }
}