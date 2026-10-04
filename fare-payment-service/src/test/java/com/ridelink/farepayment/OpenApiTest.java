package com.ridelink.farepayment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ridelink.farepayment.repository.FareRepository;
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
    private FareRepository fareRepository;

    @Test
    void openApiDocumentLoadsAndContainsFareEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/fares/estimate']").exists())
                .andExpect(jsonPath("$.paths['/api/fares/calculate']").exists())
                .andExpect(jsonPath("$.paths['/api/fares/{fareId}']").exists())
                .andExpect(jsonPath("$.paths['/api/fares/ride/{rideId}']").exists());
    }
}
