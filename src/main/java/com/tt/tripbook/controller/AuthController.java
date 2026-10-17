package com.tt.tripbook.controller;

import com.tt.tripbook.dto.AuthResponse;
import com.tt.tripbook.dto.LoginRequest;
import com.tt.tripbook.dto.RegisterRequest;
import com.tt.tripbook.dto.UserResponse;
import com.tt.tripbook.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "auth", description = "Create an account, sign in and read the current user")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @Operation(summary = "Create an account", description = "Returns a token, so the user is signed in right away.")
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(responseCode = "400", description = "Invalid data")
    @ApiResponse(responseCode = "409", description = "Email already registered")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.register(request));
    }

    @Operation(summary = "Sign in", description = "Use the returned token as 'Authorization: Bearer <token>'.")
    @ApiResponse(responseCode = "200", description = "Signed in")
    @ApiResponse(responseCode = "401", description = "Invalid email or password")
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @Operation(summary = "Current user", description = "Needs a valid token.")
    @ApiResponse(responseCode = "200", description = "The signed-in user")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.currentUser(jwt.getSubject());
    }
}
