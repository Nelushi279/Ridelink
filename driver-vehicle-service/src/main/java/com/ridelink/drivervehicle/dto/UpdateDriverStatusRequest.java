package com.ridelink.drivervehicle.dto;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.ridelink.drivervehicle.model.DriverStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateDriverStatusRequest(
    @NotNull
    @Schema(description = "Target driver status", example = "ACTIVE")
    @JsonDeserialize(using = StatusDeserializer.class)
    DriverStatus status
) {
    // Keep enum input strict for this endpoint, without changing existing APIs.
    public static class StatusDeserializer extends StdDeserializer<DriverStatus> {
        public StatusDeserializer() { super(DriverStatus.class); }

        @Override
        public DriverStatus deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                return context.reportInputMismatch(DriverStatus.class, "Status must be an enum name string");
            }
            String value = parser.getText();
            try {
                return DriverStatus.valueOf(value);
            } catch (IllegalArgumentException exception) {
                throw context.weirdStringException(value, DriverStatus.class, "Unsupported driver status");
            }
        }
    }
}