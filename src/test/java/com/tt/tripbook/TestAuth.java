package com.tt.tripbook;

import com.tt.tripbook.model.AppUser;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

// Signs an account in for tests that call the API without the security filter chain: the services
// read the current account from the SecurityContext, exactly as the real JWT filter would fill it.
public final class TestAuth {

    private TestAuth() {
    }

    public static void signIn(AppUser user) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(user.getEmail())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    public static void signOut() {
        SecurityContextHolder.clearContext();
    }
}
