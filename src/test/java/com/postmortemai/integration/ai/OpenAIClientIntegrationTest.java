package com.postmortemai.integration.ai;

import com.postmortemai.application.exception.OpenAiResponseException;
import com.postmortemai.infrastructure.ai.OpenAIClient;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.postmortemai.infrastructure.persistence.repository.IncidentJpaRepository;
import com.postmortemai.infrastructure.persistence.repository.PostMortemJpaRepository;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "spring.flyway.enabled=false",
                "spring.datasource.url="
        }
)
@ActiveProfiles("dev")
class OpenAIClientIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @MockBean
    private IncidentJpaRepository incidentJpaRepository;

    @MockBean
    private PostMortemJpaRepository postMortemJpaRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("openai.base-url", wireMock::baseUrl);
        registry.add("openai.api-key", () -> "test-api-key");
    }

    @Autowired
    private OpenAIClient openAIClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        // Reset CircuitBreaker state before each test to prevent interference
        circuitBreakerRegistry.circuitBreaker("openai").transitionToClosedState();
    }

    @Test
    @DisplayName("Cenário 1: Sucesso ponta a ponta")
    void success_scenario() {
        String mockResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "Mocked AI Response"
                      }
                    }
                  ]
                }
                """;

        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        String result = openAIClient.callChatCompletion("Test prompt");

        assertThat(result).isEqualTo("Mocked AI Response");
        wireMock.verify(1, postRequestedFor(urlEqualTo("/chat/completions")));
    }

    @Test
    @DisplayName("Cenário 2: Falha Transiente - Retry recupera o dado")
    void transient_failure_with_retry() {
        String mockResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "Mocked AI Response after retry"
                      }
                    }
                  ]
                }
                """;

        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("Second Call"));

        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Second Call")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        String result = openAIClient.callChatCompletion("Test prompt");

        assertThat(result).isEqualTo("Mocked AI Response after retry");
        wireMock.verify(2, postRequestedFor(urlEqualTo("/chat/completions")));
    }

    @Test
    @DisplayName("Cenário 3: Falha Catastrófica - Circuit Breaker abre")
    void catastrophic_failure_circuit_breaker() {
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse().withStatus(500)));

        // Sliding window is 10. Max retries is 3.
        // We do 4 calls, each resulting in 3 attempts = 12 attempts total.
        // This will exceed the sliding window size of 10 and trip the breaker.
        for (int i = 0; i < 4; i++) {
            try {
                openAIClient.callChatCompletion("Failing prompt");
            } catch (Exception ignored) {
            }
        }

        // The 5th call should be blocked immediately by CallNotPermittedException
        assertThatThrownBy(() -> openAIClient.callChatCompletion("Fast fail prompt"))
                .isInstanceOf(CallNotPermittedException.class);
    }

    @Test
    @DisplayName("Cenário 4: Payload Corrompido lança erro de negócio controlado")
    void corrupted_payload_handled_gracefully() {
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("invalid json string")));

        assertThatThrownBy(() -> openAIClient.callChatCompletion("Test prompt"))
                .isInstanceOf(OpenAiResponseException.class)
                .hasMessageContaining("Unexpected error when communicating with OpenAI");
    }
}
