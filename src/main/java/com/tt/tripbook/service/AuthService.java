package com.tt.tripbook.service;

import com.tt.tripbook.dto.AuthResponse;
import com.tt.tripbook.dto.LoginRequest;
import com.tt.tripbook.dto.RegisterRequest;
import com.tt.tripbook.dto.UserResponse;
import com.tt.tripbook.exception.ConflictException;
import com.tt.tripbook.exception.InvalidCredentialsException;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.model.AppUser;
import com.tt.tripbook.model.Role;
import com.tt.tripbook.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (repository.existsByEmail(email)) {
            throw new ConflictException("Email already registered: " + email);
        }
        AppUser user = new AppUser();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        return tokenFor(repository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = repository.findByEmail(normalize(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        return repository.findByEmail(email)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Transactional
    public UserResponse changeRole(Long userId, Role role) {
        AppUser user = repository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setRole(role);
        return UserResponse.from(repository.save(user));
    }

    @Transactional
    public UserResponse promoteFirstAdmin(String email) {
        if (repository.existsByRole(Role.ADMIN)) {
            throw new ConflictException("An admin already exists. Ask them to grant you the ADMIN role.");
        }
        AppUser user = repository.findByEmail(normalize(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        user.setRole(Role.ADMIN);
        return UserResponse.from(repository.save(user));
    }

    private AuthResponse tokenFor(AppUser user) {
        return new AuthResponse(
                jwtService.generateToken(user.getEmail(), user.getRole().name()),
                "Bearer",
                jwtService.expiresInSeconds(),
                UserResponse.from(user));
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
