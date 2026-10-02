package com.ridelink.ride.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ridelink.ride.dto.*;
import com.ridelink.ride.exception.*;
import com.ridelink.ride.model.RideStatus;
import com.ridelink.ride.service.RideService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RideController.class)
@Import(GlobalExceptionHandler.class)
class RideControllerTest {
 @Autowired MockMvc mvc;
 @MockitoBean RideService service;
 private static final String VALID="""
 {"passengerAccountId":"account-123",
 "pickupLocation":{"address":"University of Moratuwa","latitude":6.7969,"longitude":79.9018},
 "dropoffLocation":{"address":"Colombo Fort","latitude":6.9344,"longitude":79.8428}}
 """;

 @Test void createsRideAndIgnoresServerOwnedInput() throws Exception {
  when(service.createRide(any())).thenReturn(response("ride-123"));
  String body=VALID.trim().replaceFirst("\\}$", ",\"status\":\"COMPLETED\",\"driverId\":\"driver-9\",\"id\":\"chosen\"}");

  mvc.perform(post("/api/rides").contentType(MediaType.APPLICATION_JSON).content(body))
   .andExpect(status().isCreated()).andExpect(header().string("Location","/api/rides/ride-123"))
   .andExpect(jsonPath("$.status").value("REQUESTED")).andExpect(jsonPath("$.driverId").isEmpty());
  verify(service).createRide(argThat(r->r.passengerAccountId().equals("account-123")));
 }

 @Test void blankPassengerIsBadRequest() throws Exception {
  invalid(VALID.replace("account-123"," "));
 }
 @Test void missingPickupIsBadRequest() throws Exception {
  invalid("{\"passengerAccountId\":\"account-123\",\"dropoffLocation\":{\"address\":\"Fort\",\"latitude\":6.9,\"longitude\":79.8}}");
 }
 @Test void missingDropoffIsBadRequest() throws Exception {
  invalid("{\"passengerAccountId\":\"account-123\",\"pickupLocation\":{\"address\":\"Moratuwa\",\"latitude\":6.7,\"longitude\":79.9}}");
 }
 @Test void invalidPickupLatitudeIsBadRequest() throws Exception { invalid(VALID.replace("6.7969","91")); }
 @Test void invalidPickupLongitudeIsBadRequest() throws Exception { invalid(VALID.replace("79.9018","181")); }
 @Test void invalidDropoffCoordinatesAreBadRequest() throws Exception { invalid(VALID.replace("6.9344","-91")); }
 @Test void malformedJsonIsBadRequest() throws Exception {
  mvc.perform(post("/api/rides").contentType(MediaType.APPLICATION_JSON).content("{"))
   .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Malformed JSON request"));
 }

 @Test void getsRideById() throws Exception {
  when(service.getRideById("ride-123")).thenReturn(response("ride-123"));
  mvc.perform(get("/api/rides/ride-123")).andExpect(status().isOk())
   .andExpect(jsonPath("$.id").value("ride-123"));
 }
 @Test void unknownRideIsNotFound() throws Exception {
  when(service.getRideById("missing")).thenThrow(new RideNotFoundException("missing"));
  mvc.perform(get("/api/rides/missing")).andExpect(status().isNotFound())
   .andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.message").value("Ride not found: missing"));
 }
 @Test void getsPassengerRides() throws Exception {
  when(service.getRidesByPassenger("account-123")).thenReturn(List.of(response("ride-1")));
  mvc.perform(get("/api/rides/passenger/account-123")).andExpect(status().isOk())
   .andExpect(jsonPath("$[0].id").value("ride-1"));
 }
 @Test void passengerWithoutRidesGetsEmptyArray() throws Exception {
  when(service.getRidesByPassenger("none")).thenReturn(List.of());
  mvc.perform(get("/api/rides/passenger/none")).andExpect(status().isOk())
   .andExpect(content().json("[]"));
 }
 private void invalid(String body) throws Exception {
  mvc.perform(post("/api/rides").contentType(MediaType.APPLICATION_JSON).content(body))
   .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Invalid request data"));
  verifyNoInteractions(service);
 }
 private RideResponse response(String id) {
  LocationDto p=new LocationDto("University of Moratuwa",6.7969,79.9018);
  LocationDto d=new LocationDto("Colombo Fort",6.9344,79.8428);
  Instant now=Instant.parse("2026-10-02T08:00:00Z");
  return new RideResponse(id,"account-123",null,p,d,RideStatus.REQUESTED,now,null,null,null,null,null,now,now);
 }
}
