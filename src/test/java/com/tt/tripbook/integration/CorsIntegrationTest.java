package com.tt.tripbook.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// Browser preflight requests (OPTIONS) against the real application context. CORS is answered by
// the Spring Security filter chain, so the chain is added to MockMvc (webAppContextSetup does not
// register servlet filters by itself).
@SpringBootTest
class CorsIntegrationTest {

    private static final String FRONT_END = "http://localhost:5173";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void preflight_isAllowed_forTheFrontEndOrigin() throws Exception {
        mockMvc.perform(options("/api/destinations")
                        .header("Origin", FRONT_END)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", FRONT_END));
    }

    @Test
    void preflight_isRejected_forAnUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/destinations")
                        .header("Origin", "http://unknown.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
