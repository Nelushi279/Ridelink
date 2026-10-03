package com.ridelink.drivervehicle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.drivervehicle.model.*;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"})
@AutoConfigureMockMvc
class DriverEligibilityTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean DriverRepository repository;
    @MockitoBean VehicleRepository vehicleRepository;
    private Driver driver;

    @BeforeEach void setup() {
        driver=new Driver(); driver.setId("driver-123"); driver.setAccountId("account-123");
        driver.setFullName("Test Driver"); driver.setPhoneNumber("0771234567"); driver.setLicenseNumber("B1234567");
        driver.setLicenseExpiryDate(LocalDate.of(2028,12,31)); driver.setServiceArea("Colombo");
        driver.setStatus(DriverStatus.ACTIVE); driver.setAvailability(DriverAvailability.AVAILABLE);
        driver.setLatitude(6.9271); driver.setLongitude(79.8612);
        Instant time=Instant.parse("2026-10-03T10:00:00Z");
        driver.setCreatedAt(time); driver.setUpdatedAt(time); driver.setLocationUpdatedAt(time);
        when(repository.findById("driver-123")).thenReturn(Optional.of(driver));
    }

    @ParameterizedTest
    @CsvSource({
        "ACTIVE,AVAILABLE,true,true", "ACTIVE,AVAILABLE,false,false",
        "ACTIVE,UNAVAILABLE,true,false", "ACTIVE,UNAVAILABLE,false,false",
        "PENDING,AVAILABLE,true,false", "PENDING,AVAILABLE,false,false",
        "PENDING,UNAVAILABLE,true,false", "PENDING,UNAVAILABLE,false,false",
        "INACTIVE,AVAILABLE,true,false", "INACTIVE,AVAILABLE,false,false",
        "INACTIVE,UNAVAILABLE,true,false", "INACTIVE,UNAVAILABLE,false,false",
        "SUSPENDED,AVAILABLE,true,false", "SUSPENDED,AVAILABLE,false,false",
        "SUSPENDED,UNAVAILABLE,true,false", "SUSPENDED,UNAVAILABLE,false,false"
    })
    void eligibilityRequiresEveryConditionAndNeverChangesState(DriverStatus status,DriverAvailability availability,
            boolean hasVehicle,boolean expectedEligible) throws Exception {
        driver.setStatus(status); driver.setAvailability(availability);
        when(vehicleRepository.existsByDriverId("driver-123")).thenReturn(hasVehicle);
        var before=json.valueToTree(driver);
        String response=mvc.perform(get("/api/drivers/driver-123/eligibility")).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        // Exact shape excludes personal profile, location, credentials and persistence details.
        assertEquals(json.valueToTree(Map.of("driverId","driver-123","status",status.name(),
            "availability",availability.name(),"hasRegisteredVehicle",hasVehicle,"eligible",expectedEligible)),json.readTree(response));
        assertEquals(before,json.valueToTree(driver),"Status, availability, location and all timestamps must remain unchanged");
        verify(repository).findById("driver-123"); verify(vehicleRepository).existsByDriverId("driver-123");
        verifyNoMoreInteractions(repository,vehicleRepository);
    }

    @Test void multipleRegisteredVehiclesStillGiveEligibleTrueWithoutFetchingThem() throws Exception {
        Vehicle first=new Vehicle(); first.setId("vehicle-1"); first.setDriverId("driver-123");
        Vehicle second=new Vehicle(); second.setId("vehicle-2"); second.setDriverId("driver-123");
        List<Vehicle> registeredVehicles=List.of(first,second);
        when(vehicleRepository.existsByDriverId(anyString())).thenAnswer(invocation ->
            registeredVehicles.stream().anyMatch(vehicle -> vehicle.getDriverId().equals(invocation.getArgument(0))));
        mvc.perform(get("/api/drivers/driver-123/eligibility")).andExpect(status().isOk())
            .andExpect(jsonPath("$.hasRegisteredVehicle").value(true)).andExpect(jsonPath("$.eligible").value(true));
        verify(repository).findById("driver-123"); verify(vehicleRepository).existsByDriverId("driver-123");
        verifyNoMoreInteractions(repository,vehicleRepository);
    }

    @Test void unknownDriverReturns404AndDoesNotQueryVehicles() throws Exception {
        mvc.perform(get("/api/drivers/missing/eligibility")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Driver not found")).andExpect(jsonPath("$.status").value(404));
        verify(repository).findById("missing"); verifyNoMoreInteractions(repository); verifyNoInteractions(vehicleRepository);
    }

    @Test void legacyNullAvailabilityIsUnavailableAndIneligibleWithoutWrites() throws Exception {
        driver.setAvailability(null); when(vehicleRepository.existsByDriverId("driver-123")).thenReturn(true);
        mvc.perform(get("/api/drivers/driver-123/eligibility")).andExpect(status().isOk())
            .andExpect(jsonPath("$.availability").value("UNAVAILABLE")).andExpect(jsonPath("$.eligible").value(false));
        verify(repository).findById("driver-123"); verify(vehicleRepository).existsByDriverId("driver-123");
        verifyNoMoreInteractions(repository,vehicleRepository);
    }

    @Test void databaseFailureReturnsSafeErrorWithoutModifyingDriver() throws Exception {
        var before=json.valueToTree(driver);
        when(vehicleRepository.existsByDriverId("driver-123")).thenThrow(new IllegalStateException("private database details"));
        mvc.perform(get("/api/drivers/driver-123/eligibility")).andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Internal server error"));
        assertEquals(before,json.valueToTree(driver));
        verify(repository).findById("driver-123"); verify(vehicleRepository).existsByDriverId("driver-123");
        verifyNoMoreInteractions(repository,vehicleRepository);
    }

    @Test void healthSwaggerAndEligibilityContractAreDocumented() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/eligibility'].get.parameters[0].name").value("driverId"))
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/eligibility'].get.responses['200']").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/eligibility'].get.responses['404']").exists())
            .andExpect(jsonPath("$.components.schemas.DriverEligibilityResponse.properties.driverId").exists())
            .andExpect(jsonPath("$.components.schemas.DriverEligibilityResponse.properties.status").exists())
            .andExpect(jsonPath("$.components.schemas.DriverEligibilityResponse.properties.availability").exists())
            .andExpect(jsonPath("$.components.schemas.DriverEligibilityResponse.properties.hasRegisteredVehicle.type").value("boolean"))
            .andExpect(jsonPath("$.components.schemas.DriverEligibilityResponse.properties.eligible.type").value("boolean"))
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/location'].get").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/location'].patch").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/availability'].patch").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/status'].patch").exists())
            .andExpect(jsonPath("$.paths['/api/drivers'].post").exists())
            .andExpect(jsonPath("$.paths['/api/vehicles'].post").exists());
    }
}