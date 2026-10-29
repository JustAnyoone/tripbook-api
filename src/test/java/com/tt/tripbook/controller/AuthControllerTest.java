package com.tt.tripbook.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tt.tripbook.dto.AuthResponse;
import com.tt.tripbook.dto.LoginRequest;
import com.tt.tripbook.dto.RegisterRequest;
import com.tt.tripbook.dto.UserResponse;
import com.tt.tripbook.model.Role;
import com.tt.tripbook.service.PhotoService;
import com.tt.tripbook.exception.ConflictException;
import com.tt.tripbook.exception.GlobalExceptionHandler;
import com.tt.tripbook.exception.InvalidCredentialsException;
import com.tt.tripbook.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// Web-layer test without Spring Boot or Spring Security: it checks the request and response
// contract and the error mapping. Token enforcement is covered by SecurityIntegrationTest.
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService service;

    @Mock
    private PhotoService photoService;

    @InjectMocks
    private AuthController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static AuthResponse authResponse() {
        return new AuthResponse("the-token", "Bearer", 3600, new UserResponse(1L, "Ana Souza", "ana@email.com", Role.USER, null));
    }

    @Test
    void register_returns201_withTheTokenAndTheUser() throws Exception {
        when(service.register(any(RegisterRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana Souza", "email": "ana@email.com", "password": "a-long-passphrase"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("the-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.email").value("ana@email.com"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void register_returns400_whenThePasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana", "email": "ana@email.com", "password": "short"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password: password must have 8 to 72 characters"));

        verifyNoInteractions(service);
    }

    @Test
    void register_returns400_whenTheEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana", "email": "not-an-email", "password": "a-long-passphrase"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("email: invalid email"));

        verifyNoInteractions(service);
    }

    @Test
    void register_returns409_whenTheEmailIsTaken() throws Exception {
        when(service.register(any(RegisterRequest.class)))
                .thenThrow(new ConflictException("Email already registered: ana@email.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Ana", "email": "ana@email.com", "password": "a-long-passphrase"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void login_returns200_withTheToken() throws Exception {
        when(service.login(any(LoginRequest.class))).thenReturn(authResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ana@email.com", "password": "a-long-passphrase"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("the-token"));
    }

    @Test
    void login_returns401_withTheErrorBody_whenTheCredentialsAreWrong() throws Exception {
        when(service.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ana@email.com", "password": "wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_returns400_whenThePasswordIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ana@email.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("password: password is required"));

        verifyNoInteractions(service);
    }
}
