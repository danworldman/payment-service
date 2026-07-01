package com.innowise.payment_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class RestClientConfigTest {

    @Test
    void restClientBeans_shouldBeInitialized() {
        RestClientConfig config = new RestClientConfig();
        RestClient primaryClient = config.restClient();
        RestClient cleanClient = config.cleanRestClient();

        assertThat(primaryClient).isNotNull();
        assertThat(cleanClient).isNotNull();
        assertThat(primaryClient).isNotSameAs(cleanClient);
    }
}
