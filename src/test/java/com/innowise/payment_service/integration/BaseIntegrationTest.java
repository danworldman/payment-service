package com.innowise.payment_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.innowise.payment_service.dao.PaymentDAO;
import com.innowise.payment_service.integration.config.TestSecurityConfig;
import com.innowise.payment_service.testdata.PaymentTestData;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.Date;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EnableAsync
@Import(TestSecurityConfig.class)
public abstract class BaseIntegrationTest extends PaymentTestData {

    @LocalServerPort
    protected int port;

    protected RestTemplate restTemplate;
    protected static final WireMockServer wireMockServer;
    protected static final MongoDBContainer mongoDBContainer;
    protected static final KafkaContainer kafkaContainer;
    public static final KeyPair keyPair;

    @Autowired
    protected PaymentDAO paymentDAO;

    @Autowired
    protected ObjectMapper objectMapper;

    static {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.4"))
                .withStartupTimeout(Duration.ofMinutes(2));
        mongoDBContainer.start();

        kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
                .withStartupTimeout(Duration.ofMinutes(2));
        kafkaContainer.start();

        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            keyPair = kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("external.api.url", () -> "http://localhost:" + wireMockServer.port() + "/api/numbers");
    }

    @BeforeEach
    void setUpBase() {
        restTemplate = new RestTemplate();
        wireMockServer.resetRequests();
        wireMockServer.resetToDefaultMappings();
        mockExternalApi();
    }

    @AfterEach
    void tearDownBase() {
        paymentDAO.deleteAll();
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected String generateTestToken(Long userId, String role) {
        try {
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .claim("user_id", userId.toString())
                    .claim("role", role)
                    .expirationTime(new Date(System.currentTimeMillis() + 300000))
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID("1")
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new RSASSASigner(keyPair.getPrivate()));
            return signedJWT.serialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void mockExternalApi() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/api/numbers"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"number\":42}")));
    }

    protected String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}