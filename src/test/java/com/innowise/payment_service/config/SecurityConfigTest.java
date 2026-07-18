package com.innowise.payment_service.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    void jwtAuthenticationConverter_shouldMapClaimsToAuthorities() {
        SecurityConfig config = new SecurityConfig();
        Converter<Jwt, AbstractAuthenticationToken> converter = config.jwtAuthenticationConverter();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("role", "ADMIN")
                .build();

        AbstractAuthenticationToken authenticationToken = converter.convert(jwt);

        assertThat(authenticationToken).isNotNull();
        boolean hasAdminRole = authenticationToken.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        assertThat(hasAdminRole).isTrue();
    }

    @Test
    void jwtAuthenticationConverter_shouldReturnEmptyAuthorities_whenRoleMissing() {
        SecurityConfig config = new SecurityConfig();
        Converter<Jwt, AbstractAuthenticationToken> converter = config.jwtAuthenticationConverter();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("user_id", 1L)
                .build();

        AbstractAuthenticationToken authenticationToken = converter.convert(jwt);

        assertThat(authenticationToken).isNotNull();
        boolean hasAnyCustomRole = authenticationToken.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> {
                    Assertions.assertNotNull(authority);
                    return authority.startsWith("ROLE_");
                });
        assertThat(hasAnyCustomRole).isFalse();
    }
}