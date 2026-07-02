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
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"payment-events"})
@Import({TestSecurityConfig.class})
@TestPropertySource(properties = {
        "mongock.enabled=false",
        "spring.task.execution.pool.core-size=4",
        "spring.main.allow-bean-definition-overriding=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest extends PaymentTestData {

    @LocalServerPort
    protected int port;

    protected RestTemplate restTemplate;
    protected static final WireMockServer wireMockServer;
    protected static final MongoDBContainer mongoDBContainer;
    public static final KeyPair keyPair;

    @Autowired
    protected PaymentDAO paymentDAO;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired(required = false)
    protected EmbeddedKafkaBroker embeddedKafkaBroker;

    static {
        mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.4"));
        mongoDBContainer.start();

        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        WireMock.configureFor("127.0.0.1", wireMockServer.port());

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
        registry.add("external.api.url", () -> "http://127.0.0.1:" + wireMockServer.port() + "/api/numbers");
    }

    @BeforeEach
    void setUpBase() {
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(HttpStatusCode statusCode) {
                return false;
            }
        });
        wireMockServer.resetRequests();
        wireMockServer.resetToDefaultMappings();
        mockExternalApi();
    }

    @AfterEach
    void tearDownBase() {
        paymentDAO.deleteAll();
    }

    protected String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    protected String generateTestToken(Long userId, String role) {
        try {
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .claim("user_id", userId)
                    .claim("role", role)
                    .expirationTime(new Date(System.currentTimeMillis() + 300000))
                    .build();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID("1")
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(new RSASSASigner(keyPair.getPrivate()));
            return signedJWT.serialize();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private void mockExternalApi() {
        wireMockServer.stubFor(WireMock.get(WireMock.urlEqualTo("/api/numbers"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("{\"number\":" + EVEN_NUMBER + "}")));
    }

    protected String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}