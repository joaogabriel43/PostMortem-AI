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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ExportControllerE2ETest {

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

    private UUID incidentId;
    private UUID postMortemId;

    @BeforeEach
    void setUp() {
        postMortemRepository.deleteAll();
        incidentRepository.deleteAll();

        incidentId = UUID.randomUUID();
        postMortemId = UUID.randomUUID();

        IncidentEntity incident = new IncidentEntity(
                incidentId,
                "PaymentGateway",
                "gateway-service",
                "hash-abc-123",
                IncidentSeverity.P1,
                IncidentStatus.RESOLVED,
                LocalDateTime.now()
        );
        incidentRepository.save(incident);

        PostMortemEntity postMortem = new PostMortemEntity(
                postMortemId,
                incident,
                "Outage Title",
                "Summary Text",
                "Timeline Details",
                "Root Cause Info",
                "Impact Description",
                "Detection Strategy",
                "factors", "actions", "lessons",
                LocalDateTime.now()
        );
        postMortemRepository.save(postMortem);
    }

    @Test
    @DisplayName("Deve exportar Markdown com headers corretos e persistir a alteracao no banco de dados")
    void shouldExportMarkdownCorrectly() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/incidents/" + incidentId + "/postmortem/export")
                        .param("format", "markdown"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/markdown"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=postmortem-" + incidentId + ".md"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("# Outage Title");
        assertThat(body).contains("## Summary\nSummary Text");
        assertThat(body).contains("## Contributing Factors\nfactors");

        // Verify the database now contains the exported Markdown
        PostMortemEntity persisted = postMortemRepository.findById(postMortemId).orElseThrow();
        assertThat(persisted.getExportedMarkdown()).isNotBlank();
        assertThat(persisted.getExportedMarkdown()).isEqualTo(body);
    }

    @Test
    @DisplayName("Deve exportar PDF com headers corretos e com magic bytes PDF correspondentes")
    void shouldExportPdfCorrectly() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/incidents/" + incidentId + "/postmortem/export")
                        .param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=postmortem-" + incidentId + ".pdf"))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(content).isNotNull();
        assertThat(content.length).isGreaterThan(4);

        // Verify PDF Magic Bytes (%PDF signature)
        assertThat(content[0]).isEqualTo((byte) 0x25); // %
        assertThat(content[1]).isEqualTo((byte) 0x50); // P
        assertThat(content[2]).isEqualTo((byte) 0x44); // D
        assertThat(content[3]).isEqualTo((byte) 0x46); // F

        // Verify the database remained unmodified for exportedMarkdown
        PostMortemEntity persisted = postMortemRepository.findById(postMortemId).orElseThrow();
        assertThat(persisted.getExportedMarkdown()).isNull();
    }
}
