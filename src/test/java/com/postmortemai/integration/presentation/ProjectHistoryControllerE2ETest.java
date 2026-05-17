package com.postmortemai.integration.presentation;

import com.postmortemai.domain.enums.IncidentSeverity;
import com.postmortemai.domain.enums.IncidentStatus;
import com.postmortemai.infrastructure.persistence.entity.IncidentEntity;
import com.postmortemai.infrastructure.persistence.entity.PostMortemEntity;
import com.postmortemai.infrastructure.persistence.repository.IncidentJpaRepository;
import com.postmortemai.infrastructure.persistence.repository.PostMortemJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ProjectHistoryControllerE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl().replace("jdbc:postgresql", "jdbc:p6spy:postgresql"));
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IncidentJpaRepository incidentRepository;

    @Autowired
    private PostMortemJpaRepository postMortemRepository;

    @BeforeEach
    void setUp() {
        postMortemRepository.deleteAll();
        incidentRepository.deleteAll();

        // Seed 5 incidents for "PaymentGateway" with strictly decreasing dates
        LocalDateTime now = LocalDateTime.now();
        for (int i = 1; i <= 5; i++) {
            UUID incidentId = UUID.randomUUID();
            IncidentEntity incident = new IncidentEntity(
                    incidentId,
                    "PaymentGateway",
                    "gateway-service-" + i,
                    "hash-" + i,
                    IncidentSeverity.P1,
                    IncidentStatus.RESOLVED,
                    now.minusDays(i) // i=1 is the most recent (minus 1 day), i=5 is oldest (minus 5 days)
            );
            incidentRepository.save(incident);

            // Link a PostMortem so we have a title in the query
            PostMortemEntity postMortem = new PostMortemEntity(
                    UUID.randomUUID(),
                    incident,
                    "Outage Title " + i,
                    "Summary " + i,
                    "Timeline " + i,
                    "Root Cause " + i,
                    "Impact " + i,
                    "Detection " + i,
                    null, null, null,
                    now.minusDays(i)
            );
            postMortemRepository.save(postMortem);
        }
    }

    @Test
    @DisplayName("Deve retornar o historico paginado ordenado do mais recente para o mais antigo")
    void shouldReturnPaginatedHistoryOrdered() throws Exception {
        mockMvc.perform(get("/api/v1/projects/PaymentGateway/postmortems")
                        .param("page", "0")
                        .param("size", "3")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(5)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.currentPage", is(0)))
                .andExpect(jsonPath("$.data", hasSize(3)))
                // PaymentGateway Outage 1 (minus 1 day) is most recent, Outage 3 is third
                .andExpect(jsonPath("$.data[0].title", is("Outage Title 1")))
                .andExpect(jsonPath("$.data[1].title", is("Outage Title 2")))
                .andExpect(jsonPath("$.data[2].title", is("Outage Title 3")));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se o tamanho da pagina solicitado for excessivo")
    void shouldReturn400WhenSizeTooLarge() throws Exception {
        mockMvc.perform(get("/api/v1/projects/PaymentGateway/postmortems")
                        .param("page", "0")
                        .param("size", "500")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", notNullValue()))
                .andExpect(jsonPath("$.status", is(400)));
    }
}
