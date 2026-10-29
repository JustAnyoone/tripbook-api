package com.tt.tripbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tt.tripbook.dto.AuthResponse;
import com.tt.tripbook.dto.LoginRequest;
import com.tt.tripbook.dto.RegisterRequest;
import com.tt.tripbook.dto.UserResponse;
import com.tt.tripbook.exception.ConflictException;
import com.tt.tripbook.exception.InvalidCredentialsException;
import com.tt.tripbook.exception.ResourceNotFoundException;
import com.tt.tripbook.model.AppUser;
import com.tt.tripbook.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

// Pure unit test: the repository, the encoder and the token service are Mockito mocks.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService service;

    @Test
    void register_storesAHashedPasswordAndANormalizedEmail_andReturnsAToken() {
        when(repository.existsByEmail("ana@email.com")).thenReturn(false);
        when(passwordEncoder.encode("a-long-passphrase")).thenReturn("HASH");
        when(repository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });
        when(jwtService.generateToken("ana@email.com")).thenReturn("the-token");
        when(jwtService.expiresInSeconds()).thenReturn(3600L);

        AuthResponse response = service.register(
                new RegisterRequest("  Ana Souza ", "  Ana@Email.COM ", "a-long-passphrase"));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Ana Souza");
        assertThat(captor.getValue().getEmail()).isEqualTo("ana@email.com");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("HASH");
        assertThat(response.token()).isEqualTo("the-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.user()).isEqualTo(new UserResponse(7L, "Ana Souza", "ana@email.com", null));
    }

    @Test
    void register_throwsConflict_whenTheEmailIsAlreadyRegistered() {
        when(repository.existsByEmail("ana@email.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("Ana", "ana@email.com", "a-long-passphrase")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ana@email.com");

        verify(repository, never()).save(any(AppUser.class));
    }

    @Test
    void login_returnsAToken_whenThePasswordMatches() {
        AppUser user = user(3L, "ana@email.com", "HASH");
        when(repository.findByEmail("ana@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("a-long-passphrase", "HASH")).thenReturn(true);
        when(jwtService.generateToken("ana@email.com")).thenReturn("the-token");
        when(jwtService.expiresInSeconds()).thenReturn(3600L);

        AuthResponse response = service.login(new LoginRequest(" ANA@email.com", "a-long-passphrase"));

        assertThat(response.token()).isEqualTo("the-token");
        assertThat(response.user().id()).isEqualTo(3L);
    }

    @Test
    void login_throwsInvalidCredentials_whenThePasswordIsWrong() {
        when(repository.findByEmail("ana@email.com")).thenReturn(Optional.of(user(3L, "ana@email.com", "HASH")));
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("ana@email.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_throwsTheSameError_whenTheEmailIsUnknown() {
        when(repository.findByEmail("nobody@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("nobody@email.com", "whatever")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void currentUser_returnsTheAccount_withoutThePasswordHash() {
        when(repository.findByEmail("ana@email.com")).thenReturn(Optional.of(user(3L, "ana@email.com", "HASH")));

        UserResponse response = service.currentUser("ana@email.com");

        assertThat(response).isEqualTo(new UserResponse(3L, "Ana Souza", "ana@email.com", null));
    }

    @Test
    void currentUser_throwsNotFound_whenTheAccountWasRemoved() {
        when(repository.findByEmail("gone@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.currentUser("gone@email.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private AppUser user(Long id, String email, String hash) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setName("Ana Souza");
        user.setEmail(email);
        user.setPasswordHash(hash);
        return user;
    }
}
