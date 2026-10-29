package com.tt.tripbook.controller;

import com.tt.tripbook.dto.AuthResponse;
import com.tt.tripbook.dto.LoginRequest;
import com.tt.tripbook.dto.RegisterRequest;
import com.tt.tripbook.dto.UserResponse;
import com.tt.tripbook.model.Role;
import com.tt.tripbook.service.AuthService;
import com.tt.tripbook.service.PhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "auth", description = "Create an account, sign in and read the current user")
public class AuthController {

    private final AuthService service;
    private final PhotoService photoService;

    public AuthController(AuthService service, PhotoService photoService) {
        this.service = service;
        this.photoService = photoService;
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

    @Operation(summary = "Change role of a user (admin only)",
            description = "Promote a user to ADMIN or demote back to USER.")
    @ApiResponse(responseCode = "200", description = "Role updated, new token returned")
    @ApiResponse(responseCode = "403", description = "Only admins can change roles")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{userId}/role")
    public UserResponse changeRole(@PathVariable Long userId, @RequestParam Role role) {
        return service.changeRole(userId, role);
    }

    @Operation(summary = "Promote yourself to ADMIN",
            description = "Only works if there are no admins yet. Use once to bootstrap the first admin account.")
    @ApiResponse(responseCode = "200", description = "You are now admin")
    @ApiResponse(responseCode = "409", description = "An admin already exists")
    @PostMapping("/promote-first-admin")
    public UserResponse promoteFirstAdmin(@AuthenticationPrincipal Jwt jwt) {
        return service.promoteFirstAdmin(jwt.getSubject());
    }

    @Operation(summary = "Upload or replace your profile picture",
            description = "Accepts JPEG, PNG or WebP, max 5 MB. Returns the Cloudinary URL.")
    @ApiResponse(responseCode = "200", description = "Picture uploaded")
    @ApiResponse(responseCode = "400", description = "Invalid file (not an image, too large or empty)")
    @PostMapping(value = "/me/photo", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> uploadMyPhoto(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(Map.of("photoUrl", photoService.uploadUserPhoto(file)));
    }

    @Operation(summary = "Remove your profile picture")
    @ApiResponse(responseCode = "204", description = "Picture removed")
    @ApiResponse(responseCode = "400", description = "Account has no picture")
    @DeleteMapping("/me/photo")
    public ResponseEntity<Void> deleteMyPhoto() {
        photoService.deleteUserPhoto();
        return ResponseEntity.noContent().build();
    }
}
