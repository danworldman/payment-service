package com.innowise.payment_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import java.util.Arrays;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        boolean isDockerProfileActive = Arrays.asList(environment.getActiveProfiles()).contains("docker");

        if (isDockerProfileActive) {
            httpSecurity
                    .authorizeHttpRequests(authorizationRegistry -> authorizationRegistry
                            .requestMatchers(HttpMethod.POST, "/api/payments").hasAnyRole("USER", "ADMIN")
                            .requestMatchers(HttpMethod.GET, "/api/payments/**").hasAnyRole("USER", "ADMIN")
                            .requestMatchers(HttpMethod.GET, "/api/payments/summary").hasRole("ADMIN")
                            .anyRequest().authenticated()
                    )
                    .oauth2ResourceServer(oauth2Configurer -> oauth2Configurer.jwt(jwtConfigurer -> {}));
        } else {
            httpSecurity.authorizeHttpRequests(authorizationRegistry -> authorizationRegistry
                    .anyRequest().permitAll()
            );
        }

        return httpSecurity.build();
    }
}