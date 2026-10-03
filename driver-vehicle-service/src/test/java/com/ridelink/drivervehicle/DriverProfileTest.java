package com.ridelink.drivervehicle;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.drivervehicle.model.Driver;
import com.ridelink.drivervehicle.repository.DriverRepository;
import com.ridelink.drivervehicle.repository.VehicleRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.dao.DuplicateKeyException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"})
@AutoConfigureMockMvc
class DriverProfileTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @MockitoBean DriverRepository repository;
 @MockitoBean VehicleRepository vehicleRepository;
 Map<String,Object> request;
 Driver saved;
 @BeforeEach void setup() {
  request=new LinkedHashMap<>(Map.of("accountId","account-123","fullName","Test Driver","phoneNumber","0771234567","licenseNumber","B1234567","licenseExpiryDate","2099-12-31","serviceArea","Colombo"));
  when(repository.save(any(Driver.class))).thenAnswer(invocation -> {
   saved=invocation.getArgument(0); saved.setId("driver-123"); return saved;
  });
 }
 private void createDriver() throws Exception {
  mvc.perform(post("/api/drivers").contentType("application/json").content(json.writeValueAsString(request)))
   .andExpect(status().isCreated()).andExpect(header().string("Location","/api/drivers/driver-123"))
   .andExpect(jsonPath("$.status").value("PENDING"))
   .andExpect(jsonPath("$.createdAt").isNotEmpty()).andExpect(jsonPath("$.updatedAt").isNotEmpty());
 }
 private void existing() throws Exception {
  createDriver(); saved.setUpdatedAt(Instant.parse("2020-01-01T00:00:00Z"));
  when(repository.findById("driver-123")).thenReturn(Optional.of(saved));
  when(repository.findByAccountId("account-123")).thenReturn(Optional.of(saved));
 }
 @Test void creationSetsPendingAndBothTimestamps() throws Exception { createDriver(); assertEquals(saved.getCreatedAt(),saved.getUpdatedAt()); }
 @Test void duplicateAccountReturnsConflict() throws Exception {
  when(repository.existsByAccountId("account-123")).thenReturn(true);
  mvc.perform(post("/api/drivers").contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isConflict());
  verify(repository,never()).save(any());
 }
 @Test void duplicateLicenseReturnsConflict() throws Exception {
  when(repository.existsByLicenseNumber("B1234567")).thenReturn(true);
  mvc.perform(post("/api/drivers").contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isConflict());
 }
 @ParameterizedTest @ValueSource(strings={"accountId","fullName","phoneNumber","licenseNumber","serviceArea"})
 void blankRequiredFieldsReturnBadRequest(String field) throws Exception {
  request.put(field," ");
  mvc.perform(post("/api/drivers").contentType("application/json").content(json.writeValueAsString(request)))
   .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors."+field).exists());
 }
 @Test void missingExpiryReturnsBadRequest() throws Exception {
  request.remove("licenseExpiryDate");
  mvc.perform(post("/api/drivers").contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
 }
 @Test void expiredLicenseReturnsBadRequest() throws Exception {
  request.put("licenseExpiryDate","2000-01-01");
  mvc.perform(post("/api/drivers").contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
 }
 @Test void licenseExpiringTodayIsAccepted() throws Exception {
  request.put("licenseExpiryDate",LocalDate.now(ZoneId.of("Asia/Colombo")).toString()); createDriver();
 }
 @Test void getById() throws Exception { existing(); mvc.perform(get("/api/drivers/driver-123")).andExpect(status().isOk()).andExpect(jsonPath("$.accountId").value("account-123")); }
 @Test void unknownId() throws Exception { mvc.perform(get("/api/drivers/missing")).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Driver not found")); }
 @Test void getByAccount() throws Exception { existing(); mvc.perform(get("/api/drivers/account/account-123")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value("driver-123")); }
 @Test void unknownAccount() throws Exception { mvc.perform(get("/api/drivers/account/missing")).andExpect(status().isNotFound()); }
 @ParameterizedTest @ValueSource(strings={"fullName","phoneNumber","licenseNumber","licenseExpiryDate","serviceArea"})
 void updatesEachAllowedFieldAndPreservesIdentity(String field) throws Exception {
  existing(); Instant created=saved.getCreatedAt(); String value=field.equals("licenseExpiryDate")?"2100-01-01":"Updated";
  mvc.perform(patch("/api/drivers/driver-123").contentType("application/json").content(json.writeValueAsString(Map.of(field,value))))
   .andExpect(status().isOk()).andExpect(jsonPath("$."+field).value(value))
   .andExpect(jsonPath("$.accountId").value("account-123")).andExpect(jsonPath("$.status").value("PENDING"));
  assertEquals(created,saved.getCreatedAt()); assertTrue(saved.getUpdatedAt().isAfter(Instant.parse("2020-01-01T00:00:00Z")));
 }
 @Test void duplicateUpdatedLicense() throws Exception {
  existing(); when(repository.existsByLicenseNumber("TAKEN")).thenReturn(true);
  mvc.perform(patch("/api/drivers/driver-123").contentType("application/json").content("{\"licenseNumber\":\"TAKEN\"}")).andExpect(status().isConflict());
  assertEquals("B1234567",saved.getLicenseNumber());
 }
 @Test void unchangedLicenseAllowed() throws Exception {
  existing(); when(repository.existsByLicenseNumber("B1234567")).thenReturn(true);
  mvc.perform(patch("/api/drivers/driver-123").contentType("application/json").content("{\"licenseNumber\":\"B1234567\"}")).andExpect(status().isOk());
 }
 @Test void expiredUpdatedLicense() throws Exception {
  existing(); mvc.perform(patch("/api/drivers/driver-123").contentType("application/json").content("{\"licenseExpiryDate\":\"2000-01-01\"}")).andExpect(status().isBadRequest());
 }
 @Test void emptyPatch() throws Exception { mvc.perform(patch("/api/drivers/driver-123").contentType("application/json").content("{}")).andExpect(status().isBadRequest()); }
 @ParameterizedTest @ValueSource(strings={"id","accountId","status","createdAt","updatedAt"})
 void protectedPatchFieldsRejected(String field) throws Exception {
  existing(); mvc.perform(patch("/api/drivers/driver-123").contentType("application/json").content(json.writeValueAsString(Map.of(field,"changed","fullName","Changed"))))
   .andExpect(status().isBadRequest()); assertEquals("Test Driver",saved.getFullName());
 }
 @ParameterizedTest @ValueSource(strings={"id","status","createdAt","updatedAt"})
 void protectedCreateFieldsRejected(String field) throws Exception {
  request.put(field,"changed");
  mvc.perform(post("/api/drivers").contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
 }
 @ParameterizedTest @ValueSource(strings={"fullName","phoneNumber","licenseNumber","serviceArea"})
 void blankPatchFieldsRejected(String field) throws Exception {
  mvc.perform(patch("/api/drivers/driver-123").contentType("application/json").content(json.writeValueAsString(Map.of(field," ")))).andExpect(status().isBadRequest());
 }
 @Test void malformedJson() throws Exception { mvc.perform(post("/api/drivers").contentType("application/json").content("{")).andExpect(status().isBadRequest()); }
 @Test void invalidDate() throws Exception {
  request.put("licenseExpiryDate","not-a-date"); mvc.perform(post("/api/drivers").contentType("application/json").content(json.writeValueAsString(request))).andExpect(status().isBadRequest());
 }
 @Test void uniqueIndexRaceReturnsConflictWithoutDatabaseDetails() throws Exception {
  when(repository.save(any())).thenThrow(new DuplicateKeyException("secret database details"));
  mvc.perform(post("/api/drivers").contentType("application/json").content(json.writeValueAsString(request)))
   .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Account ID or license number is already in use"));
 }
 @Test void unexpectedFailureHasSafeResponse() throws Exception {
  when(repository.findById("broken")).thenThrow(new IllegalStateException("secret database details"));
  mvc.perform(get("/api/drivers/broken")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.message").value("Internal server error"));
 }
 @Test void unknownUpdateDriver() throws Exception { mvc.perform(patch("/api/drivers/missing").contentType("application/json").content("{\"fullName\":\"Updated\"}")).andExpect(status().isNotFound()); }
 @Test void healthStillWorks() throws Exception { mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP")); }
 @Test void openApiDocumentsAllEndpoints() throws Exception {
  mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
   .andExpect(jsonPath("$.paths['/api/drivers'].post.responses['201']").exists())
   .andExpect(jsonPath("$.paths['/api/drivers/{driverId}'].get").exists())
   .andExpect(jsonPath("$.paths['/api/drivers/{driverId}'].patch").exists())
   .andExpect(jsonPath("$.paths['/api/drivers/account/{accountId}'].get").exists());
 }
 @Test void swaggerUiLoads() throws Exception { mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk()); }
}