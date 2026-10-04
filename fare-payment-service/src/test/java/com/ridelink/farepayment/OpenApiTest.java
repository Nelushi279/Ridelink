package com.ridelink.farepayment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ridelink.farepayment.repository.FareRepository;
import com.ridelink.farepayment.repository.PaymentRepository;
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

    @MockitoBean
    private PaymentRepository paymentRepository;

    @Test
    void openApiDocumentLoadsAndContainsFareEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/fares/estimate']").exists())
                .andExpect(jsonPath("$.paths['/api/fares/calculate']").exists())
                .andExpect(jsonPath("$.paths['/api/fares/{fareId}']").exists())
                .andExpect(jsonPath("$.paths['/api/fares/ride/{rideId}']").exists());
    }

    @Test
    void openApiDocumentContainsPaymentEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/payments']").exists())
                .andExpect(jsonPath("$.paths['/api/payments/{paymentId}']").exists());
    }
}
