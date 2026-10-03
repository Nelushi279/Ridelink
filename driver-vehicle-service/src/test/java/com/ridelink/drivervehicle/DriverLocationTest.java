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
import org.junit.jupiter.params.provider.*;
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
class DriverLocationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean DriverRepository repository;
    @MockitoBean VehicleRepository vehicleRepository;
    private Driver driver;
    private Instant originalUpdatedAt;

    @BeforeEach void setup() {
        driver=new Driver(); driver.setId("driver-123"); driver.setAccountId("account-123");
        driver.setFullName("Test Driver"); driver.setPhoneNumber("0771234567"); driver.setLicenseNumber("B1234567");
        driver.setLicenseExpiryDate(LocalDate.of(2028,12,31)); driver.setServiceArea("Colombo");
        driver.setStatus(DriverStatus.ACTIVE); driver.setAvailability(DriverAvailability.AVAILABLE);
        originalUpdatedAt=Instant.parse("2026-10-03T10:00:00Z");
        driver.setCreatedAt(originalUpdatedAt); driver.setUpdatedAt(originalUpdatedAt);
        when(repository.findById("driver-123")).thenReturn(Optional.of(driver));
        when(repository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
    private ResultActions location(String body) throws Exception {
        return mvc.perform(patch("/api/drivers/driver-123/location").contentType("application/json").content(body));
    }
    private ResultActions report(double latitude,double longitude) throws Exception {
        return location(json.writeValueAsString(Map.of("latitude",latitude,"longitude",longitude)));
    }
    private void knownLocation() {
        driver.setLatitude(6.9271); driver.setLongitude(79.8612); driver.setLocationUpdatedAt(originalUpdatedAt);
    }
    private ObjectNode unrelated() {
        ObjectNode fields=json.valueToTree(driver);
        fields.remove(List.of("latitude","longitude","locationUpdatedAt","updatedAt")); return fields;
    }

    @Test void activeAvailableDriverReportsCoordinatesWithEqualTimestampsAndPreservedProfile() throws Exception {
        ObjectNode before=unrelated();
        report(6.9271,79.8612).andExpect(status().isOk()).andExpect(jsonPath("$.driverId").value("driver-123"))
            .andExpect(jsonPath("$.latitude").value(6.9271)).andExpect(jsonPath("$.longitude").value(79.8612))
            .andExpect(jsonPath("$.locationUpdatedAt").isNotEmpty());
        assertEquals(6.9271,driver.getLatitude()); assertEquals(79.8612,driver.getLongitude());
        assertEquals(driver.getUpdatedAt(),driver.getLocationUpdatedAt()); assertTrue(driver.getUpdatedAt().isAfter(originalUpdatedAt));
        assertEquals(before,unrelated()); verify(repository).save(driver); verifyNoInteractions(vehicleRepository);
    }
    @Test void reportingAgainReplacesOnlyCurrentLocation() throws Exception {
        knownLocation(); report(7.1,80.2).andExpect(status().isOk());
        mvc.perform(get("/api/drivers/driver-123/location")).andExpect(status().isOk())
            .andExpect(jsonPath("$.latitude").value(7.1)).andExpect(jsonPath("$.longitude").value(80.2));
    }
    @Test void repeatedCoordinatesStillRefreshReportTime() throws Exception {
        knownLocation(); report(6.9271,79.8612).andExpect(status().isOk());
        assertTrue(driver.getLocationUpdatedAt().isAfter(originalUpdatedAt));
    }
    @ParameterizedTest @EnumSource(value=DriverStatus.class,names={"PENDING","INACTIVE","SUSPENDED"})
    void nonActiveDriversCannotReportEvenIfAvailable(DriverStatus current) throws Exception {
        knownLocation(); driver.setStatus(current); ObjectNode before=json.valueToTree(driver);
        report(7,80).andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Only ACTIVE and AVAILABLE drivers can update location"));
        assertEquals(before,json.valueToTree(driver)); verify(repository,never()).save(any());
    }
    @Test void activeUnavailableDriverCannotReport() throws Exception {
        driver.setAvailability(DriverAvailability.UNAVAILABLE);
        report(7,80).andExpect(status().isConflict()); verify(repository,never()).save(any());
    }
    @ParameterizedTest @CsvSource({"-90.0001,0","90.0001,0","0,-180.0001","0,180.0001"})
    void invalidCoordinatesReturnBadRequest(double latitude,double longitude) throws Exception {
        report(latitude,longitude).andExpect(status().isBadRequest()); verifyNoInteractions(repository,vehicleRepository);
    }
    @ParameterizedTest @CsvSource({"-90,-180","90,180","0,0"})
    void inclusiveBoundariesAndZeroAreValid(double latitude,double longitude) throws Exception {
        report(latitude,longitude).andExpect(status().isOk()).andExpect(jsonPath("$.latitude").value(latitude))
            .andExpect(jsonPath("$.longitude").value(longitude));
    }
    @ParameterizedTest @ValueSource(strings={"{}","{\"longitude\":79.8612}","{\"latitude\":6.9271}","{\"latitude\":null,\"longitude\":79.8612}","{\"latitude\":6.9271,\"longitude\":null}"})
    void missingOrNullCoordinatesReturnBadRequest(String body) throws Exception {
        location(body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Invalid request data"));
        verifyNoInteractions(repository,vehicleRepository);
    }
    @ParameterizedTest @ValueSource(strings={"{","{\"latitude\":true,\"longitude\":80}","{\"latitude\":{},\"longitude\":80}","{\"latitude\":\"invalid\",\"longitude\":80}","{\"latitude\":\"NaN\",\"longitude\":80}","{\"latitude\":7,\"longitude\":\"Infinity\"}"})
    void malformedOrNonFiniteCoordinatesReturnBadRequest(String body) throws Exception {
        location(body).andExpect(status().isBadRequest()); verifyNoInteractions(repository,vehicleRepository);
    }
    @Test void unknownDriverUpdateReturnsNotFound() throws Exception {
        mvc.perform(patch("/api/drivers/missing/location").contentType("application/json").content("{\"latitude\":7,\"longitude\":80}"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Driver not found"));
    }
    @Test void getsKnownLocationWithoutRequiringActiveAvailability() throws Exception {
        knownLocation(); driver.setStatus(DriverStatus.SUSPENDED); driver.setAvailability(DriverAvailability.UNAVAILABLE);
        mvc.perform(get("/api/drivers/driver-123/location")).andExpect(status().isOk())
            .andExpect(jsonPath("$.driverId").value("driver-123")).andExpect(jsonPath("$.latitude").value(6.9271))
            .andExpect(jsonPath("$.longitude").value(79.8612)).andExpect(jsonPath("$.locationUpdatedAt").value(originalUpdatedAt.toString()));
        verify(repository,never()).save(any());
    }
    @Test void unknownDriverRetrievalReturnsNotFound() throws Exception {
        mvc.perform(get("/api/drivers/missing/location")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Driver not found"));
    }
    @Test void neverReportedLocationReturnsClearNotFound() throws Exception {
        mvc.perform(get("/api/drivers/driver-123/location")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Driver location not available"));
    }
    @Test void newDriverHasNoFakeCoordinates() throws Exception {
        when(repository.save(any())).thenAnswer(invocation -> {
            Driver created=invocation.getArgument(0); assertNull(created.getLatitude()); assertNull(created.getLongitude());
            assertNull(created.getLocationUpdatedAt()); created.setId("new-driver"); return created;
        });
        mvc.perform(post("/api/drivers").contentType("application/json").content("{\"accountId\":\"new-account\",\"fullName\":\"Test Driver\",\"phoneNumber\":\"0771234567\",\"licenseNumber\":\"B1234567\",\"licenseExpiryDate\":\"2099-12-31\",\"serviceArea\":\"Colombo\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.latitude").isEmpty())
            .andExpect(jsonPath("$.longitude").isEmpty()).andExpect(jsonPath("$.locationUpdatedAt").isEmpty());
    }
    @ParameterizedTest @ValueSource(strings={"latitude","longitude","locationUpdatedAt","availability","status","accountId","id","createdAt"})
    void profilePatchRejectsProtectedFields(String field) throws Exception {
        mvc.perform(patch("/api/drivers/driver-123").contentType("application/json")
            .content(json.writeValueAsString(Map.of(field,"changed","fullName","Changed Name"))))
            .andExpect(status().isBadRequest()); verifyNoInteractions(repository,vehicleRepository);
    }
    @ParameterizedTest @ValueSource(strings={"latitude","longitude","locationUpdatedAt"})
    void createCannotSupplyLocation(String field) throws Exception {
        mvc.perform(post("/api/drivers").contentType("application/json").content(json.writeValueAsString(Map.of(field,1))))
            .andExpect(status().isBadRequest()); verifyNoInteractions(repository,vehicleRepository);
    }
    @Test void locationPatchRejectsServerOwnedTimestamp() throws Exception {
        location("{\"latitude\":7,\"longitude\":80,\"locationUpdatedAt\":\"2026-10-04T08:00:00Z\"}")
            .andExpect(status().isBadRequest()); verifyNoInteractions(repository,vehicleRepository);
    }
    @ParameterizedTest @EnumSource(value=DriverStatus.class,names={"PENDING","INACTIVE","SUSPENDED"})
    void leavingActiveKeepsLastLocationButBlocksFurtherReports(DriverStatus target) throws Exception {
        knownLocation();
        mvc.perform(patch("/api/drivers/driver-123/status").contentType("application/json")
            .content(json.writeValueAsString(Map.of("status",target.name())))).andExpect(status().isOk());
        assertEquals(6.9271,driver.getLatitude()); assertEquals(79.8612,driver.getLongitude());
        assertEquals(originalUpdatedAt,driver.getLocationUpdatedAt()); assertEquals(DriverAvailability.UNAVAILABLE,driver.getAvailability());
        report(7,80).andExpect(status().isConflict());
        mvc.perform(get("/api/drivers/driver-123/location")).andExpect(status().isOk());
    }
    @Test void becomingUnavailableKeepsLastLocationButBlocksReports() throws Exception {
        knownLocation();
        mvc.perform(patch("/api/drivers/driver-123/availability").contentType("application/json").content("{\"availability\":\"UNAVAILABLE\"}"))
            .andExpect(status().isOk());
        assertEquals(originalUpdatedAt,driver.getLocationUpdatedAt()); report(7,80).andExpect(status().isConflict());
        mvc.perform(get("/api/drivers/driver-123/location")).andExpect(status().isOk());
    }
    @Test void unexpectedFailureHasSafeError() throws Exception {
        when(repository.save(any())).thenThrow(new IllegalStateException("private database details"));
        report(7,80).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.message").value("Internal server error"));
    }
    @Test void healthSwaggerAndAllExistingRoutesRemainDocumented() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/location'].patch.requestBody").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/location'].patch.parameters[0].name").value("driverId"))
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/location'].patch.responses['200']").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/location'].patch.responses['400']").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/location'].patch.responses['404']").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/location'].patch.responses['409']").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/location'].get.responses['200']").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/location'].get.responses['404']").exists())
            .andExpect(jsonPath("$.components.schemas.UpdateDriverLocationRequest.properties.latitude.minimum").value(-90))
            .andExpect(jsonPath("$.components.schemas.UpdateDriverLocationRequest.properties.latitude.maximum").value(90))
            .andExpect(jsonPath("$.components.schemas.UpdateDriverLocationRequest.properties.longitude.minimum").value(-180))
            .andExpect(jsonPath("$.components.schemas.UpdateDriverLocationRequest.properties.longitude.maximum").value(180))
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/availability'].patch").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}/status'].patch").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}'].patch").exists())
            .andExpect(jsonPath("$.paths['/api/drivers'].post").exists())
            .andExpect(jsonPath("$.paths['/api/vehicles'].post").exists());
    }
}