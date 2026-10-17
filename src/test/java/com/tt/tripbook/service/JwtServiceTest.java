package com.tt.tripbook.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

// Pure unit test: the real encoder and decoder, no Spring context.
class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-with-at-least-32-bytes!!";

    private final JwtService service = new JwtService(
            new NimbusJwtEncoder(new ImmutableSecret<>(key(SECRET))), 30);

    @Test
    void generateToken_putsTheSubjectTheIssuerAndTheExpiryInTheToken() {
        Instant before = Instant.now();

        Jwt jwt = decoder(SECRET).decode(service.generateToken("ana@email.com"));

        assertThat(jwt.getSubject()).isEqualTo("ana@email.com");
        assertThat(jwt.getIssuer().toString()).isEqualTo(JwtService.ISSUER);
        assertThat(jwt.getExpiresAt()).isBetween(
                before.plus(Duration.ofMinutes(30)).minusSeconds(5),
                Instant.now().plus(Duration.ofMinutes(30)).plusSeconds(5));
    }

    @Test
    void expiresInSeconds_matchesTheConfiguredLifetime() {
        assertThat(service.expiresInSeconds()).isEqualTo(30 * 60);
    }

    @Test
    void aTokenSignedWithAnotherKey_isRejected() {
        String token = service.generateToken("ana@email.com");

        assertThatThrownBy(() -> decoder("another-secret-with-at-least-32-bytes!!!!").decode(token))
                .isInstanceOf(JwtException.class);
    }

    private static SecretKey key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static JwtDecoder decoder(String secret) {
        return NimbusJwtDecoder.withSecretKey(key(secret)).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
