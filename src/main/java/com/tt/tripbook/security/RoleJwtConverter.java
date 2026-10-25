package com.tt.tripbook.security;

import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

// Maps the custom "role" JWT claim (e.g. "ADMIN") to a Spring Security authority "ROLE_ADMIN".
// Falls back to the default scope-based converter for any token that has no role claim.
public class RoleJwtConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        String role = jwt.getClaimAsString("role");
        if (role != null && !role.isBlank()) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        }
        return defaultConverter.convert(jwt);
    }
}
