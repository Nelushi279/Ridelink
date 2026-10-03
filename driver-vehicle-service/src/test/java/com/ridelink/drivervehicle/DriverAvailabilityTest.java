package com.ridelink.drivervehicle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ridelink.drivervehicle.model.*;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
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
class DriverAvailabilityTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean DriverRepository repository;
    @MockitoBean VehicleRepository vehicleRepository;
    private Driver driver;
    private Instant originalUpdatedAt;

    @BeforeEach
    void setup() {
        driver = new Driver();
        driver.setId("driver-123"); driver.setAccountId("account-123");
        driver.setFullName("Test Driver"); driver.setPhoneNumber("0771234567");
        driver.setLicenseNumber("B1234567"); driver.setLicenseExpiryDate(LocalDate.of(2028,12,31));
        driver.setServiceArea("Colombo"); driver.setStatus(DriverStatus.ACTIVE);
        driver.setCreatedAt(Instant.parse("2026-10-03T10:00:00Z"));
        originalUpdatedAt = driver.getCreatedAt(); driver.setUpdatedAt(originalUpdatedAt);
        when(repository.findById("driver-123")).thenReturn(Optional.of(driver));
        when(repository.findByAccountId("account-123")).thenReturn(Optional.of(driver));
        when(repository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ResultActions request(String body) throws Exception {
        return mvc.perform(patch("/api/drivers/driver-123/availability").contentType("application/json").content(body));
    }
    private ResultActions availability(DriverAvailability value) throws Exception {
        return request(json.writeValueAsString(Map.of("availability",value.name())));
    }
    private ResultActions statusChange(DriverStatus value) throws Exception {
        return mvc.perform(patch("/api/drivers/driver-123/status").contentType("application/json")
            .content(json.writeValueAsString(Map.of("status",value.name()))));
    }
    private ObjectNode unrelatedFields(Driver value) {
        ObjectNode fields = json.valueToTree(value);
        fields.remove(List.of("status","availability","updatedAt"));
        return fields;
    }

    @Test
    void activeDriverCanBecomeAvailableAndOnlyAvailabilityAndTimestampChange() throws Exception {
        ObjectNode before = unrelatedFields(driver);
        availability(DriverAvailability.AVAILABLE).andExpect(status().isOk())
            .andExpect(jsonPath("$.availability").value("AVAILABLE"));
        assertEquals(DriverAvailability.AVAILABLE,driver.getAvailability());
        assertTrue(driver.getUpdatedAt().isAfter(originalUpdatedAt));
        assertEquals(before,unrelatedFields(driver));
        assertEquals(DriverStatus.ACTIVE,driver.getStatus());
        verify(repository).save(driver); verifyNoInteractions(vehicleRepository);
    }

    @ParameterizedTest @EnumSource(DriverStatus.class)
    void anyStatusCanBecomeUnavailable(DriverStatus current) throws Exception {
        driver.setStatus(current); driver.setAvailability(DriverAvailability.AVAILABLE);
        ObjectNode before = unrelatedFields(driver);
        availability(DriverAvailability.UNAVAILABLE).andExpect(status().isOk())
            .andExpect(jsonPath("$.availability").value("UNAVAILABLE"));
        assertEquals(current,driver.getStatus()); assertEquals(before,unrelatedFields(driver));
        assertTrue(driver.getUpdatedAt().isAfter(originalUpdatedAt));
        verify(repository).save(driver); verifyNoInteractions(vehicleRepository);
    }

    @ParameterizedTest @EnumSource(value=DriverStatus.class,names={"PENDING","INACTIVE","SUSPENDED"})
    void nonActiveDriverCannotBecomeAvailable(DriverStatus current) throws Exception {
        driver.setStatus(current); ObjectNode before=json.valueToTree(driver);
        availability(DriverAvailability.AVAILABLE).andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Only ACTIVE drivers can become available"));
        assertEquals(before,json.valueToTree(driver)); verify(repository,never()).save(any());
        verifyNoInteractions(vehicleRepository);
    }

    @Test
    void inconsistentNonActiveAvailableDriverCannotRepeatAvailable() throws Exception {
        driver.setStatus(DriverStatus.SUSPENDED); driver.setAvailability(DriverAvailability.AVAILABLE);
        availability(DriverAvailability.AVAILABLE).andExpect(status().isConflict());
        verify(repository,never()).save(any());
    }

    @Test
    void activeAvailableRepeatIsAnUnchangedNoOp() throws Exception {
        driver.setAvailability(DriverAvailability.AVAILABLE); ObjectNode before=json.valueToTree(driver);
        String body=availability(DriverAvailability.AVAILABLE).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(before,json.readTree(body)); verify(repository,never()).save(any());
        verifyNoInteractions(vehicleRepository);
    }

    @ParameterizedTest @EnumSource(DriverStatus.class)
    void unavailableRepeatIsAnUnchangedNoOpForAnyStatus(DriverStatus current) throws Exception {
        driver.setStatus(current); ObjectNode before=json.valueToTree(driver);
        String body=availability(DriverAvailability.UNAVAILABLE).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(before,json.readTree(body)); verify(repository,never()).save(any());
        verifyNoInteractions(vehicleRepository);
    }

    @ParameterizedTest @EnumSource(value=DriverStatus.class,names={"PENDING","INACTIVE","SUSPENDED"})
    void leavingActiveAutomaticallyForcesUnavailable(DriverStatus target) throws Exception {
        driver.setAvailability(DriverAvailability.AVAILABLE); ObjectNode before=unrelatedFields(driver);
        statusChange(target).andExpect(status().isOk()).andExpect(jsonPath("$.status").value(target.name()))
            .andExpect(jsonPath("$.availability").value("UNAVAILABLE"));
        assertEquals(before,unrelatedFields(driver)); assertTrue(driver.getUpdatedAt().isAfter(originalUpdatedAt));
        verify(repository).save(driver); verifyNoInteractions(vehicleRepository);
    }

    @Test
    void activatingPendingDriverLeavesItUnavailable() throws Exception {
        driver.setStatus(DriverStatus.PENDING); when(vehicleRepository.existsByDriverId("driver-123")).thenReturn(true);
        statusChange(DriverStatus.ACTIVE).andExpect(status().isOk()).andExpect(jsonPath("$.availability").value("UNAVAILABLE"));
        verify(vehicleRepository).existsByDriverId("driver-123");
    }

    @Test
    void repeatedInactiveStatusRepairsInconsistentAvailability() throws Exception {
        driver.setStatus(DriverStatus.INACTIVE); driver.setAvailability(DriverAvailability.AVAILABLE);
        statusChange(DriverStatus.INACTIVE).andExpect(status().isOk()).andExpect(jsonPath("$.availability").value("UNAVAILABLE"));
        assertTrue(driver.getUpdatedAt().isAfter(originalUpdatedAt)); verify(repository).save(driver);
    }

    @Test
    void newProfileDefaultsToUnavailable() throws Exception {
        when(repository.save(any())).thenAnswer(invocation -> { Driver saved=invocation.getArgument(0); saved.setId("new-driver"); return saved; });
        mvc.perform(post("/api/drivers").contentType("application/json").content("{\"accountId\":\"new-account\",\"fullName\":\"New Driver\",\"phoneNumber\":\"0771234567\",\"licenseNumber\":\"B1234567\",\"licenseExpiryDate\":\"2099-12-31\",\"serviceArea\":\"Colombo\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.availability").value("UNAVAILABLE"));
    }

    @Test
    void lookupsByIdAndAccountExposeCurrentAvailability() throws Exception {
        driver.setAvailability(DriverAvailability.AVAILABLE);
        mvc.perform(get("/api/drivers/driver-123")).andExpect(status().isOk()).andExpect(jsonPath("$.availability").value("AVAILABLE"));
        mvc.perform(get("/api/drivers/account/account-123")).andExpect(status().isOk()).andExpect(jsonPath("$.availability").value("AVAILABLE"));
    }

    @Test
    void legacyNullAvailabilityIsReportedAsUnavailable() throws Exception {
        driver.setAvailability(null);
        mvc.perform(get("/api/drivers/driver-123")).andExpect(status().isOk()).andExpect(jsonPath("$.availability").value("UNAVAILABLE"));
        availability(DriverAvailability.UNAVAILABLE).andExpect(status().isOk());
        assertEquals(originalUpdatedAt,driver.getUpdatedAt()); verify(repository,never()).save(any());
    }

    @Test
    void unknownDriverReturnsNotFound() throws Exception {
        mvc.perform(patch("/api/drivers/missing/availability").contentType("application/json").content("{\"availability\":\"AVAILABLE\"}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Driver not found"));
    }

    @ParameterizedTest @ValueSource(strings={"{}","{\"availability\":null}"})
    void missingOrNullAvailabilityReturnsValidationError(String body) throws Exception {
        request(body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.availability").value("must not be null"));
        verifyNoInteractions(repository,vehicleRepository);
    }

    @ParameterizedTest @ValueSource(strings={"{","{\"availability\":\"ONLINE\"}","{\"availability\":\"available\"}","{\"availability\":\"\"}","{\"availability\":0}","{\"availability\":\"0\"}","{\"availability\":true}","{\"availability\":[]}","{\"availability\":{}}"})
    void malformedAvailabilityReturnsBadRequest(String body) throws Exception {
        request(body).andExpect(status().isBadRequest()); verifyNoInteractions(repository,vehicleRepository);
    }

    @ParameterizedTest @ValueSource(strings={"status","availability","accountId","id","createdAt"})
    void profilePatchRejectsProtectedFields(String field) throws Exception {
        mvc.perform(patch("/api/drivers/driver-123").contentType("application/json")
            .content(json.writeValueAsString(Map.of(field,"AVAILABLE","fullName","Changed"))))
            .andExpect(status().isBadRequest()); verifyNoInteractions(repository,vehicleRepository);
    }

    @Test
    void creationCannotChooseAvailability() throws Exception {
        mvc.perform(post("/api/drivers").contentType("application/json").content("{\"availability\":\"AVAILABLE\"}"))
            .andExpect(status().isBadRequest()); verifyNoInteractions(repository,vehicleRepository);
    }

    @Test
    void availabilityEndpointRejectsUnrelatedEdits() throws Exception {
        request("{\"availability\":\"UNAVAILABLE\",\"status\":\"ACTIVE\"}").andExpect(status().isBadRequest());
        verifyNoInteractions(repository,vehicleRepository);
    }

    @Test
    void statusEndpointCannotDirectlyChooseAvailability() throws Exception {
        mvc.perform(patch("/api/drivers/driver-123/status").contentType("application/json")
            .content("{\"status\":\"ACTIVE\",\"availability\":\"AVAILABLE\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void unexpectedFailureHasSafeResponse() throws Exception {
        when(repository.save(any())).thenThrow(new IllegalStateException("private database details"));
        availability(DriverAvailability.AVAILABLE).andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @Test
    void healthSwaggerAndOpenApiRemainWorking() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/availability'].patch.parameters[0].name").value("driverId"))
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/availability'].patch.requestBody").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/availability'].patch.responses['200']").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/availability'].patch.responses['400']").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/availability'].patch.responses['404']").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/availability'].patch.responses['409'].description").value("Only ACTIVE drivers can become available"))
            .andExpect(jsonPath("$.components.schemas.DriverResponse.properties.availability.enum.length()").value(2))
            .andExpect(jsonPath("$.components.schemas.UpdateDriverAvailabilityRequest.required[0]").value("availability"))
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/status'].patch").exists())
            .andExpect(jsonPath("$.paths['/api/drivers'].post").exists())
            .andExpect(jsonPath("$.paths['/api/vehicles'].post").exists());
    }
}