package com.ridelink.drivervehicle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ridelink.drivervehicle.model.Driver;
import com.ridelink.drivervehicle.model.DriverStatus;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"})
@AutoConfigureMockMvc
class DriverStatusTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean DriverRepository repository;
    @MockitoBean VehicleRepository vehicleRepository;
    private Driver driver;
    private Instant originalUpdatedAt;
    private ObjectNode originalProfile;

    @BeforeEach
    void setup() {
        driver = new Driver();
        driver.setId("driver-123");
        driver.setAccountId("account-123");
        driver.setFullName("Test Driver");
        driver.setPhoneNumber("0771234567");
        driver.setLicenseNumber("B1234567");
        driver.setLicenseExpiryDate(LocalDate.of(2028, 12, 31));
        driver.setServiceArea("Colombo");
        driver.setStatus(DriverStatus.PENDING);
        driver.setCreatedAt(Instant.parse("2026-10-03T10:00:00Z"));
        originalUpdatedAt = driver.getCreatedAt();
        driver.setUpdatedAt(originalUpdatedAt);
        originalProfile = json.valueToTree(driver);
        when(repository.findById("driver-123")).thenReturn(Optional.of(driver));
        when(repository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ResultActions request(String body) throws Exception {
        return mvc.perform(patch("/api/drivers/driver-123/status").contentType("application/json").content(body));
    }

    private ResultActions setStatus(DriverStatus target) throws Exception {
        return request(json.writeValueAsString(Map.of("status", target.name())));
    }

    private void assertOnlyStatusAndUpdatedAtChanged(String response) throws Exception {
        ObjectNode actual = (ObjectNode) json.readTree(response);
        ObjectNode expected = originalProfile.deepCopy();
        actual.remove("status"); actual.remove("updatedAt");
        expected.remove("status"); expected.remove("updatedAt");
        assertEquals(expected, actual, "Identity, profile fields and createdAt must remain unchanged");
    }

    @ParameterizedTest
    @EnumSource(value = DriverStatus.class, names = {"PENDING", "INACTIVE", "SUSPENDED"})
    void driverWithVehicleCanBecomeActive(DriverStatus initial) throws Exception {
        driver.setStatus(initial);
        when(vehicleRepository.existsByDriverId("driver-123")).thenReturn(true);
        String response = setStatus(DriverStatus.ACTIVE).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE")).andReturn().getResponse().getContentAsString();
        assertEquals(DriverStatus.ACTIVE, driver.getStatus());
        assertTrue(driver.getUpdatedAt().isAfter(originalUpdatedAt));
        assertOnlyStatusAndUpdatedAtChanged(response);
        verify(vehicleRepository).existsByDriverId("driver-123");
        verify(repository).save(driver);
        verifyNoMoreInteractions(vehicleRepository);
    }

    @ParameterizedTest
    @EnumSource(value = DriverStatus.class, names = {"PENDING", "INACTIVE", "SUSPENDED"})
    void driverWithoutVehicleCannotBecomeActive(DriverStatus initial) throws Exception {
        driver.setStatus(initial);
        setStatus(DriverStatus.ACTIVE).andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Driver cannot be activated without a registered vehicle"))
            .andExpect(jsonPath("$.fieldErrors").isMap());
        assertEquals(initial, driver.getStatus());
        assertEquals(originalUpdatedAt, driver.getUpdatedAt());
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = DriverStatus.class, names = {"PENDING", "INACTIVE", "SUSPENDED"})
    void otherStatusesDoNotRequireVehicles(DriverStatus target) throws Exception {
        driver.setStatus(DriverStatus.ACTIVE);
        String response = setStatus(target).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(target.name())).andReturn().getResponse().getContentAsString();
        assertTrue(driver.getUpdatedAt().isAfter(originalUpdatedAt));
        assertOnlyStatusAndUpdatedAtChanged(response);
        verify(repository).save(driver);
        verifyNoInteractions(vehicleRepository);
    }

    @ParameterizedTest
    @EnumSource(DriverStatus.class)
    void repeatingCurrentStatusIsAnUnchangedNoOp(DriverStatus current) throws Exception {
        driver.setStatus(current);
        ObjectNode before = json.valueToTree(driver);
        String response = setStatus(current).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(before, json.readTree(response));
        assertEquals(originalUpdatedAt, driver.getUpdatedAt());
        verify(repository, never()).save(any());
        verifyNoInteractions(vehicleRepository);
    }

    @Test
    void unknownDriverReturnsNotFoundBeforeCheckingVehicles() throws Exception {
        mvc.perform(patch("/api/drivers/missing/status").contentType("application/json").content("{\"status\":\"ACTIVE\"}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Driver not found"));
        verifyNoInteractions(vehicleRepository);
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status\":null}"})
    void missingOrNullStatusReturnsValidationError(String body) throws Exception {
        request(body).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid request data"))
            .andExpect(jsonPath("$.fieldErrors.status").value("must not be null"));
        verifyNoInteractions(repository, vehicleRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "{\"status\":\"ONLINE\"}", "{\"status\":\"active\"}", "{\"status\":\"\"}",
        "{\"status\":0}", "{\"status\":\"0\"}", "{\"status\":true}", "{\"status\":[]}", "{\"status\":{}}"})
    void invalidStatusOrMalformedJsonReturnsBadRequest(String body) throws Exception {
        request(body).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Malformed JSON or unsupported request field"));
        verifyNoInteractions(repository, vehicleRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "accountId", "createdAt", "updatedAt", "fullName"})
    void statusEndpointRejectsUnrelatedFields(String field) throws Exception {
        request(json.writeValueAsString(Map.of("status", "INACTIVE", field, "changed")))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(repository, vehicleRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "accountId", "status", "createdAt"})
    void normalProfilePatchStillRejectsProtectedFields(String field) throws Exception {
        String value = field.equals("status") ? "ACTIVE" : "changed";
        mvc.perform(patch("/api/drivers/driver-123").contentType("application/json")
            .content(json.writeValueAsString(Map.of(field, value, "fullName", "Changed Name"))))
            .andExpect(status().isBadRequest());
        assertEquals(originalProfile, json.valueToTree(driver));
        verifyNoInteractions(repository, vehicleRepository);
    }

    @Test
    void unexpectedSaveFailureHasSafeError() throws Exception {
        when(repository.save(any())).thenThrow(new IllegalStateException("private database details"));
        setStatus(DriverStatus.INACTIVE).andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @Test
    void unexpectedVehicleLookupFailureDoesNotChangeDriver() throws Exception {
        when(vehicleRepository.existsByDriverId("driver-123")).thenThrow(new IllegalStateException("private database details"));
        setStatus(DriverStatus.ACTIVE).andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Internal server error"));
        assertEquals(originalProfile, json.valueToTree(driver));
        verify(repository, never()).save(any());
    }

    @Test
    void healthAndSwaggerStillLoad() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void openApiDocumentsStatusOperationAndPreservesExistingRoutes() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/status'].patch.requestBody").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/status'].patch.parameters[0].name").value("driverId"))
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/status'].patch.responses['200']").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/status'].patch.responses['400']").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/status'].patch.responses['404']").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/status'].patch.responses['409'].description").value("Activation requires a registered vehicle"))
            .andExpect(jsonPath("$.components.schemas.UpdateDriverStatusRequest.required[0]").value("status"))
            .andExpect(jsonPath("$.components.schemas.UpdateDriverStatusRequest.properties.status.enum.length()").value(4))
            .andExpect(jsonPath("$.paths['/api/drivers'].post").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}'].patch").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/account/{accountId}'].get").exists())
            .andExpect(jsonPath("$.paths['/api/vehicles'].post").exists())
            .andExpect(jsonPath("$.paths['/api/vehicles/{vehicleId}'].get").exists())
            .andExpect(jsonPath("$.paths['/api/vehicles/{vehicleId}'].patch").exists())
            .andExpect(jsonPath("$.paths['/api/vehicles/driver/{driverId}'].get").exists())
            .andExpect(jsonPath("$.paths['/api/health'].get").exists());
    }
}