package com.ridelink.ride;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ridelink.ride.repository.RideRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RideRepository rideRepository;

    @Test
    void openApiDocumentLoadsAndContainsRideEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/rides']").exists())
                .andExpect(jsonPath("$.paths['/api/rides/{rideId}']").exists())
                .andExpect(jsonPath("$.paths['/api/rides/passenger/{passengerAccountId}']").exists())
                .andExpect(jsonPath("$.paths['/api/rides/{rideId}/assign']").exists());
    }
}
