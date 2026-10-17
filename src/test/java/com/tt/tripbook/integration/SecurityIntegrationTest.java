package com.tt.tripbook.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tt.tripbook.repository.UserRepository;
import com.tt.tripbook.service.JwtService;
import jakarta.servlet.Filter;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// Authentication through the real stack. The Spring Security filter chain is added to MockMvc by
// hand: webAppContextSetup does not register servlet filters, and without the chain no request
// would ever be rejected. The other integration tests skip the chain on purpose, because they are
// about pagination and business rules, not about who is calling.
@SpringBootTest
class SecurityIntegrationTest {

    private static final String REGISTER = """
            {"name": "Ana Souza", "email": "ana@email.com", "password": "a-long-passphrase"}
            """;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtEncoder jwtEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void protectedEndpoint_returns401_withTheErrorBody_whenThereIsNoToken() throws Exception {
        mockMvc.perform(get("/api/destinations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required: missing, invalid or expired token"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void protectedEndpoint_returns401_whenTheTokenIsGarbage() throws Exception {
        mockMvc.perform(get("/api/destinations").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedEndpoint_returns401_whenTheSignatureWasTamperedWith() throws Exception {
        String token = jwtService.generateToken("ana@email.com");

        mockMvc.perform(get("/api/destinations").header(HttpHeaders.AUTHORIZATION, "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_returns401_whenTheTokenIsExpired() throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtService.ISSUER)
                .subject("ana@email.com")
                .issuedAt(now.minusSeconds(7200))
                .expiresAt(now.minusSeconds(3600))
                .build();
        String expired = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mockMvc.perform(get("/api/destinations").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_returns200_withAValidToken() throws Exception {
        String token = jwtService.generateToken("ana@email.com");

        mockMvc.perform(get("/api/destinations").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void writeEndpoints_areProtectedToo() throws Exception {
        mockMvc.perform(post("/api/destinations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"city": "Lisbon", "country": "Portugal"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerDocumentation_isPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    void register_returnsAToken_thatOpensTheProtectedEndpoints() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("ana@email.com"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.token");

        mockMvc.perform(get("/api/destinations").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void register_storesOnlyAHashOfThePassword() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated());

        String stored = userRepository.findByEmail("ana@email.com").orElseThrow().getPasswordHash();
        assertThat(stored).isNotEqualTo("a-long-passphrase").startsWith("$2");
    }

    @Test
    void register_returns409_whenTheEmailIsTaken_evenWithDifferentCase() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Other", "email": "ANA@email.com", "password": "another-passphrase"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void login_returnsAToken_forTheRightPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ana@email.com", "password": "a-long-passphrase"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_returns401_forAWrongPassword_andForAnUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ana@email.com", "password": "wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nobody@email.com", "password": "wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void me_returnsTheSignedInUser_andNeedsAToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.token");

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value("ana@email.com"));

        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void preflight_doesNotNeedAToken() throws Exception {
        mockMvc.perform(options("/api/destinations")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
