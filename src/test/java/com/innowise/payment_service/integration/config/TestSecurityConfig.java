package com.innowise.payment_service.integration.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.payment_service.integration.BaseIntegrationTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import java.security.interfaces.RSAPublicKey;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    public RSAPublicKey rsaPublicKey() {
        return (RSAPublicKey) BaseIntegrationTest.keyPair.getPublic();
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAPublicKey publicKey) {
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}