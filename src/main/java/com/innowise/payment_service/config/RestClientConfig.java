package com.innowise.payment_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
public class RestClientConfig {

    @Bean
    @Primary
    public RestClient restClient() {
        return RestClient.builder()
                .requestInterceptor(bearerTokenInterceptor())
                .build();
    }

    @Bean(name = "cleanRestClient")
    public RestClient cleanRestClient() {
        return RestClient.builder()
                .build();
    }

    private ClientHttpRequestInterceptor bearerTokenInterceptor() {
        return (request, body, execution) -> {
            ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (servletRequestAttributes != null) {
                HttpServletRequest httpServletRequest = servletRequestAttributes.getRequest();
                String authorizationHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);

                if (authorizationHeader != null && !authorizationHeader.isEmpty()) {
                    request.getHeaders().add(HttpHeaders.AUTHORIZATION, authorizationHeader);
                }
            }

            return execution.execute(request, body);
        };
    }
}