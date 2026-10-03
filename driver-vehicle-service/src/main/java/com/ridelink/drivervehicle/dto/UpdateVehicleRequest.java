package com.ridelink.drivervehicle.dto;

import com.ridelink.drivervehicle.model.VehicleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Editable vehicle fields only. Omitted or null fields are unchanged; at least one non-null field is required.")
public record UpdateVehicleRequest(
    @Size(max = 30) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String registrationNumber,
    @Size(max = 80) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String make,
    @Size(max = 80) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String model,
    @Min(1980) @Schema(description = "1980 through the current year plus one") Integer manufactureYear,
    @Size(max = 50) @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank") String color,
    VehicleType vehicleType,
    @Min(1) @Max(16) Integer seatCapacity
) {
    public boolean isEmpty() {
        return registrationNumber == null && make == null && model == null
            && manufactureYear == null && color == null && vehicleType == null && seatCapacity == null;
    }
}
