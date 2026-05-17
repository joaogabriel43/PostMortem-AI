package com.postmortemai.unit.usecase;

import com.postmortemai.application.dto.ExportFormat;
import com.postmortemai.application.dto.ExportedDocument;
import com.postmortemai.application.exception.ResourceNotFoundException;
import com.postmortemai.application.port.PostMortemRepositoryPort;
import com.postmortemai.application.port.out.PostMortemExporter;
import com.postmortemai.application.usecase.ExportPostMortemUseCase;
import com.postmortemai.domain.model.PostMortem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportPostMortemUseCaseTest {

    @Mock
    private PostMortemRepositoryPort postMortemRepositoryPort;

    @Mock
    private PostMortemExporter markdownExporter;

    @Mock
    private PostMortemExporter pdfExporter;

    private ExportPostMortemUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ExportPostMortemUseCase(postMortemRepositoryPort, List.of(markdownExporter, pdfExporter));
    }

    @Test
    @DisplayName("Deve selecionar dynamicamente o exportador Markdown e persistir o log alterado no banco")
    void shouldExportMarkdownAndPersist() {
        UUID incidentId = UUID.randomUUID();
        PostMortem postMortem = new PostMortem(
                UUID.randomUUID(),
                incidentId,
                "Title", "Summary", "Timeline", "Root Cause", "Impact", "Detection",
                null, null, null, null,
                LocalDateTime.now()
        );

        when(postMortemRepositoryPort.findByIncidentId(incidentId)).thenReturn(Optional.of(postMortem));
        
        when(markdownExporter.supports(ExportFormat.MARKDOWN)).thenReturn(true);

        String generatedMarkdown = "# Generated Markdown Report";
        byte[] contentBytes = generatedMarkdown.getBytes(StandardCharsets.UTF_8);
        when(markdownExporter.export(postMortem)).thenReturn(contentBytes);

        ExportedDocument result = useCase.execute(incidentId, ExportFormat.MARKDOWN);

        assertThat(result).isNotNull();
        assertThat(result.contentType()).isEqualTo("text/markdown");
        assertThat(new String(result.content(), StandardCharsets.UTF_8)).isEqualTo(generatedMarkdown);

        // Verification of save calls and record mutation
        ArgumentCaptor<PostMortem> postMortemCaptor = ArgumentCaptor.forClass(PostMortem.class);
        verify(postMortemRepositoryPort, times(1)).save(postMortemCaptor.capture());
        
        PostMortem savedPostMortem = postMortemCaptor.getValue();
        assertThat(savedPostMortem.exportedMarkdown()).isEqualTo(generatedMarkdown);
    }

    @Test
    @DisplayName("Deve selecionar dynamicamente o exportador PDF e NAO salvar nada no banco de dados")
    void shouldExportPdfAndAvoidPersisting() {
        UUID incidentId = UUID.randomUUID();
        PostMortem postMortem = new PostMortem(
                UUID.randomUUID(),
                incidentId,
                "Title", "Summary", "Timeline", "Root Cause", "Impact", "Detection",
                null, null, null, null,
                LocalDateTime.now()
        );

        when(postMortemRepositoryPort.findByIncidentId(incidentId)).thenReturn(Optional.of(postMortem));
        
        when(markdownExporter.supports(ExportFormat.PDF)).thenReturn(false);
        when(pdfExporter.supports(ExportFormat.PDF)).thenReturn(true);

        byte[] pdfBytes = "%PDF-1.4 simulated bytes".getBytes(StandardCharsets.UTF_8);
        when(pdfExporter.export(postMortem)).thenReturn(pdfBytes);

        ExportedDocument result = useCase.execute(incidentId, ExportFormat.PDF);

        assertThat(result).isNotNull();
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.content()).isEqualTo(pdfBytes);

        // Asserts database save is NEVER called
        verify(postMortemRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Deve estourar ResourceNotFoundException se o PostMortem nao existir")
    void shouldThrowExceptionWhenPostMortemNotFound() {
        UUID incidentId = UUID.randomUUID();
        when(postMortemRepositoryPort.findByIncidentId(incidentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(incidentId, ExportFormat.MARKDOWN))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Post-Mortem not found");
    }
}
