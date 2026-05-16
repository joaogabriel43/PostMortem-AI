package com.postmortemai.integration.presentation;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.postmortemai.infrastructure.persistence.repository.IncidentJpaRepository;
import com.postmortemai.infrastructure.persistence.repository.PostMortemJpaRepository;
import com.postmortemai.presentation.dto.IncidentRequest;
import com.postmortemai.presentation.dto.PostMortemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class IncidentControllerE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl().replace("jdbc:postgresql", "jdbc:p6spy:postgresql"));
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("openai.base-url", wireMock::baseUrl);
        registry.add("openai.api-key", () -> "test-api-key");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private IncidentJpaRepository incidentRepository;

    @Autowired
    private PostMortemJpaRepository postMortemRepository;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        postMortemRepository.deleteAll();
        incidentRepository.deleteAll();
    }

    @Test
    @DisplayName("Deve executar o pipeline completo e salvar Post-Mortem no banco com sucesso")
    void shouldExecuteFullPipelineAndSave() {
        // Setup WireMock for Extraction (Prompt 1)
        String extractionResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "{\\"title\\": \\"Database Outage\\", \\"severity\\": \\"P1\\", \\"status\\": \\"RESOLVED\\", \\"summary\\": \\"DB went down\\", \\"timeline\\": \\"10:00 DB went down\\", \\"rootCause\\": \\"OOM\\", \\"impact\\": \\"High\\", \\"detection\\": \\"Alert\\"}"
                      }
                    }
                  ]
                }
                """;
                
        // Setup WireMock for Redaction (Prompt 2)
        String redactionResponse = """
                {
                  "choices": [
                    {
                      "message": {
                        "content": "# Database Outage\\n**Severity:** P1"
                      }
                    }
                  ]
                }
                """;

        // Since both calls go to the same endpoint, we use a scenario to return different responses
        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario("AI Pipeline")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(extractionResponse))
                .willSetStateTo("Redaction Phase"));

        wireMock.stubFor(post(urlEqualTo("/chat/completions"))
                .inScenario("AI Pipeline")
                .whenScenarioStateIs("Redaction Phase")
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(redactionResponse)));

        IncidentRequest request = new IncidentRequest(
                "PaymentSystem",
                "payment-api",
                "2026-05-16 10:00:00 ERROR Connection refused"
        );

        ResponseEntity<PostMortemResponse> response = restTemplate.postForEntity(
                "/api/v1/incidents", request, PostMortemResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PostMortemResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.title()).isEqualTo("Database Outage");
        assertThat(body.exportedMarkdown()).contains("# Database Outage");

        // Verify Database state
        assertThat(incidentRepository.findAll()).hasSize(1);
        assertThat(postMortemRepository.findAll()).hasSize(1);
        assertThat(postMortemRepository.findAll().get(0).getIncident().getId()).isEqualTo(body.incidentId());
        
        wireMock.verify(2, postRequestedFor(urlEqualTo("/chat/completions")));
    }

    @Test
    @DisplayName("Deve retornar 404 quando buscar um Post-Mortem inexistente")
    void shouldReturn404WhenNotFound() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/incidents/" + UUID.randomUUID() + "/postmortem", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("Resource Not Found");
    }
}
