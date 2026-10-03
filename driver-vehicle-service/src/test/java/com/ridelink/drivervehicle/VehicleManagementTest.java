package com.ridelink.drivervehicle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.drivervehicle.model.Vehicle;
import com.ridelink.drivervehicle.model.VehicleType;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"})
@AutoConfigureMockMvc
class VehicleManagementTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean DriverRepository repository;
    @MockitoBean VehicleRepository vehicleRepository;

    private Map<String, Object> request;
    private Vehicle saved;

    @BeforeEach
    void setup() {
        request = new LinkedHashMap<>();
        request.put("driverId", "driver-123");
        request.put("registrationNumber", "CAB-1234");
        request.put("make", "Toyota");
        request.put("model", "Aqua");
        request.put("manufactureYear", 2022);
        request.put("color", "White");
        request.put("vehicleType", "CAR");
        request.put("seatCapacity", 4);
        when(repository.existsById("driver-123")).thenReturn(true);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId("vehicle-123");
            return saved;
        });
    }

    private ResultActions create() throws Exception {
        return mvc.perform(post("/api/vehicles").contentType("application/json")
            .content(json.writeValueAsString(request)));
    }

    private ResultActions update(Map<String, Object> fields) throws Exception {
        return mvc.perform(patch("/api/vehicles/vehicle-123").contentType("application/json")
            .content(json.writeValueAsString(fields)));
    }

    private Vehicle existingVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setId("vehicle-123");
        vehicle.setDriverId("driver-123");
        vehicle.setRegistrationNumber("CAB-1234");
        vehicle.setMake("Toyota");
        vehicle.setModel("Aqua");
        vehicle.setManufactureYear(2022);
        vehicle.setColor("White");
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setSeatCapacity(4);
        vehicle.setCreatedAt(Instant.parse("2020-01-01T00:00:00Z"));
        vehicle.setUpdatedAt(vehicle.getCreatedAt());
        when(vehicleRepository.findById("vehicle-123")).thenReturn(Optional.of(vehicle));
        return vehicle;
    }

    @Test
    void createsVehicleWithDriverAndBothTimestamps() throws Exception {
        create().andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/vehicles/vehicle-123"))
            .andExpect(jsonPath("$.id").value("vehicle-123"))
            .andExpect(jsonPath("$.driverId").value("driver-123"))
            .andExpect(jsonPath("$.registrationNumber").value("CAB-1234"))
            .andExpect(jsonPath("$.vehicleType").value("CAR"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());
        verify(repository).existsById("driver-123");
    }

    @Test
    void rejectsUnknownDriverWithoutSaving() throws Exception {
        request.put("driverId", "missing");
        create().andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Driver not found"));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void normalizesRegistrationBeforeDuplicateCheckAndPersistence() throws Exception {
        request.put("registrationNumber", "  cab-1234  ");
        request.put("driverId", " driver-123 ");
        create().andExpect(status().isCreated()).andExpect(jsonPath("$.registrationNumber").value("CAB-1234"));
        assertEquals("CAB-1234", saved.getRegistrationNumber());
        assertEquals("driver-123", saved.getDriverId());
        verify(vehicleRepository).existsByRegistrationNumber("CAB-1234");
    }

    @Test
    void duplicateNormalizedRegistrationReturnsConflict() throws Exception {
        request.put("registrationNumber", " cab-1234 ");
        when(vehicleRepository.existsByRegistrationNumber("CAB-1234")).thenReturn(true);
        create().andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Vehicle registration number is already in use"));
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void sameDriverCanCreateMultipleVehicles() throws Exception {
        create().andExpect(status().isCreated());
        request.put("registrationNumber", "CBB-5678");
        create().andExpect(status().isCreated()).andExpect(jsonPath("$.driverId").value("driver-123"));
        verify(vehicleRepository, times(2)).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"driverId", "registrationNumber", "make", "model", "color"})
    void rejectsBlankCreateFields(String field) throws Exception {
        request.put(field, " \t ");
        create().andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors." + field).exists());
        verify(vehicleRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"driverId", "registrationNumber", "make", "model", "manufactureYear", "color", "vehicleType", "seatCapacity"})
    void rejectsMissingCreateFields(String field) throws Exception {
        request.remove(field);
        create().andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors." + field).exists());
    }

    @ParameterizedTest
    @CsvSource({"driverId,129", "registrationNumber,31", "make,81", "model,81", "color,51"})
    void rejectsOverlongCreateFields(String field, int length) throws Exception {
        request.put(field, "X".repeat(length));
        create().andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors." + field).exists());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1900, 1979})
    void rejectsOldManufactureYears(int year) throws Exception {
        request.put("manufactureYear", year);
        create().andExpect(status().isBadRequest());
    }

    @Test
    void rejectsYearBeyondNextYear() throws Exception {
        request.put("manufactureYear", Year.now(ZoneId.of("Asia/Colombo")).getValue() + 2);
        create().andExpect(status().isBadRequest());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void acceptsManufactureYearBoundaries() throws Exception {
        request.put("manufactureYear", 1980);
        create().andExpect(status().isCreated());
        request.put("manufactureYear", Year.now(ZoneId.of("Asia/Colombo")).getValue() + 1);
        create().andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 17})
    void rejectsInvalidSeatCapacity(int seats) throws Exception {
        request.put("seatCapacity", seats);
        create().andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.seatCapacity").exists());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 16})
    void acceptsSeatCapacityBoundaries(int seats) throws Exception {
        request.put("seatCapacity", seats);
        create().andExpect(status().isCreated());
    }

    @Test
    void getsVehicleById() throws Exception {
        existingVehicle();
        mvc.perform(get("/api/vehicles/vehicle-123")).andExpect(status().isOk())
            .andExpect(jsonPath("$.driverId").value("driver-123"))
            .andExpect(jsonPath("$.registrationNumber").value("CAB-1234"));
    }

    @Test
    void unknownVehicleReturnsNotFound() throws Exception {
        mvc.perform(get("/api/vehicles/missing")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Vehicle not found"));
    }

    @Test
    void listsMultipleVehiclesForDriver() throws Exception {
        Vehicle first = existingVehicle();
        Vehicle second = new Vehicle();
        second.setId("vehicle-456"); second.setDriverId("driver-123"); second.setRegistrationNumber("CBB-5678");
        when(vehicleRepository.findByDriverId("driver-123")).thenReturn(List.of(first, second));
        mvc.perform(get("/api/vehicles/driver/driver-123")).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value("vehicle-123"))
            .andExpect(jsonPath("$[1].id").value("vehicle-456"));
        verify(vehicleRepository).findByDriverId("driver-123");
    }

    @Test
    void driverWithNoVehiclesGetsEmptyArray() throws Exception {
        mvc.perform(get("/api/vehicles/driver/driver-123")).andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void unknownDriverListReturnsNotFound() throws Exception {
        mvc.perform(get("/api/vehicles/driver/missing")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Driver not found"));
        verify(vehicleRepository, never()).findByDriverId(anyString());
    }

    @ParameterizedTest
    @CsvSource({"registrationNumber,CBB-5678", "make,Honda", "model,Fit", "manufactureYear,2023", "color,Blue", "vehicleType,VAN", "seatCapacity,8"})
    void updatesEachEditableFieldAndPreservesIdentityAndCreationTime(String field, String value) throws Exception {
        Vehicle original = existingVehicle();
        Instant createdAt = original.getCreatedAt();
        Instant oldUpdatedAt = original.getUpdatedAt();
        Object typedValue = Set.of("manufactureYear", "seatCapacity").contains(field) ? Integer.valueOf(value) : value;
        update(Map.of(field, typedValue)).andExpect(status().isOk())
            .andExpect(jsonPath("$." + field).value(typedValue))
            .andExpect(jsonPath("$.id").value("vehicle-123"))
            .andExpect(jsonPath("$.driverId").value("driver-123"))
            .andExpect(jsonPath("$.createdAt").value(createdAt.toString()));
        assertEquals(createdAt, saved.getCreatedAt());
        assertTrue(saved.getUpdatedAt().isAfter(oldUpdatedAt));
    }

    @Test
    void normalizesChangedRegistration() throws Exception {
        existingVehicle();
        update(Map.of("registrationNumber", " cbb-5678 ")).andExpect(status().isOk())
            .andExpect(jsonPath("$.registrationNumber").value("CBB-5678"));
        verify(vehicleRepository).existsByRegistrationNumber("CBB-5678");
    }

    @Test
    void rejectsDuplicateChangedRegistrationBeforeModifyingOtherFields() throws Exception {
        Vehicle original = existingVehicle();
        when(vehicleRepository.existsByRegistrationNumber("TAKEN")).thenReturn(true);
        update(Map.of("registrationNumber", " taken ", "make", "Changed")).andExpect(status().isConflict());
        assertEquals("CAB-1234", original.getRegistrationNumber());
        assertEquals("Toyota", original.getMake());
        verify(vehicleRepository, never()).save(any());
    }

    @Test
    void unchangedNormalizedRegistrationDoesNotConflictWithItself() throws Exception {
        existingVehicle();
        when(vehicleRepository.existsByRegistrationNumber("CAB-1234")).thenReturn(true);
        update(Map.of("registrationNumber", " cab-1234 ")).andExpect(status().isOk());
        verify(vehicleRepository, never()).existsByRegistrationNumber(anyString());
    }

    @Test
    void rejectsFutureUpdatedYearBeforeChangingFields() throws Exception {
        Vehicle original = existingVehicle();
        update(Map.of("manufactureYear", Year.now(ZoneId.of("Asia/Colombo")).getValue() + 2, "color", "Blue"))
            .andExpect(status().isBadRequest());
        assertEquals("White", original.getColor());
        verify(vehicleRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({"manufactureYear,1979", "seatCapacity,0", "seatCapacity,17"})
    void rejectsInvalidUpdatedNumbers(String field, int value) throws Exception {
        update(Map.of(field, value)).andExpect(status().isBadRequest());
        verify(vehicleRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"registrationNumber", "make", "model", "color"})
    void rejectsBlankPatchStrings(String field) throws Exception {
        update(Map.of(field, " ")).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @CsvSource({"registrationNumber,31", "make,81", "model,81", "color,51"})
    void rejectsOverlongPatchStrings(String field, int length) throws Exception {
        update(Map.of(field, "X".repeat(length))).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsEmptyPatch() throws Exception {
        update(Map.of()).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("At least one vehicle field is required"));
    }

    @Test
    void rejectsNullOnlyPatch() throws Exception {
        Map<String, Object> fields = new HashMap<>(); fields.put("color", null);
        update(fields).andExpect(status().isBadRequest());
    }

    @Test
    void nullFieldsAreOmittedDuringValidPatch() throws Exception {
        existingVehicle();
        Map<String, Object> fields = new HashMap<>(); fields.put("color", null); fields.put("make", "Honda");
        update(fields).andExpect(status().isOk()).andExpect(jsonPath("$.color").value("White"))
            .andExpect(jsonPath("$.make").value("Honda"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "driverId", "createdAt", "updatedAt"})
    void rejectsProtectedPatchFieldsAlongsideEditableFields(String field) throws Exception {
        Vehicle original = existingVehicle();
        update(Map.of(field, "changed", "color", "Blue")).andExpect(status().isBadRequest());
        assertEquals("driver-123", original.getDriverId());
        assertEquals("White", original.getColor());
        verify(vehicleRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "createdAt", "updatedAt"})
    void rejectsServerOwnedCreateFields(String field) throws Exception {
        request.put(field, "changed");
        create().andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnknownVehicleOnUpdate() throws Exception {
        update(Map.of("color", "Blue")).andExpect(status().isNotFound());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/vehicles").contentType("application/json").content("{"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidEnumOnCreateAndPatch() throws Exception {
        request.put("vehicleType", "PLANE");
        create().andExpect(status().isBadRequest());
        update(Map.of("vehicleType", "PLANE")).andExpect(status().isBadRequest());
    }

    @Test
    void uniqueIndexRaceOnCreateReturnsSafeConflict() throws Exception {
        when(vehicleRepository.save(any())).thenThrow(new DuplicateKeyException("private MongoDB details"));
        create().andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Vehicle registration number is already in use"))
            .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void uniqueIndexRaceOnUpdateReturnsSafeConflict() throws Exception {
        existingVehicle();
        when(vehicleRepository.save(any())).thenThrow(new DuplicateKeyException("private MongoDB details"));
        update(Map.of("registrationNumber", "CBB-5678")).andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Vehicle registration number is already in use"));
    }

    @Test
    void unexpectedErrorHasSafeResponse() throws Exception {
        when(vehicleRepository.findById("broken")).thenThrow(new IllegalStateException("private MongoDB details"));
        mvc.perform(get("/api/vehicles/broken")).andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.message").value("Internal server error"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty())
            .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void healthAndSwaggerStillLoad() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void openApiDocumentsVehicleOperationsAndPreservesDriverOperations() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/vehicles'].post.requestBody").exists())
            .andExpect(jsonPath("$.paths['/api/vehicles'].post.responses['201']").exists())
            .andExpect(jsonPath("$.paths['/api/vehicles'].post.responses['400']").exists())
            .andExpect(jsonPath("$.paths['/api/vehicles'].post.responses['404']").exists())
            .andExpect(jsonPath("$.paths['/api/vehicles'].post.responses['409']").exists())
            .andExpect(jsonPath("$.paths['/api/vehicles/{vehicleId}'].get").exists())
            .andExpect(jsonPath("$.paths['/api/vehicles/{vehicleId}'].patch.requestBody").exists())
            .andExpect(jsonPath("$.paths['/api/vehicles/driver/{driverId}'].get.responses['200'].content['*/*'].schema.type").value("array"))
            .andExpect(jsonPath("$.paths['/api/drivers'].post").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}'].get").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/{driverId}'].patch").exists())
            .andExpect(jsonPath("$.paths['/api/drivers/account/{accountId}'].get").exists())
            .andExpect(jsonPath("$.paths['/api/health'].get").exists());
    }
}