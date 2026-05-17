package com.postmortemai.infrastructure.export;

import com.lowagie.text.Document;
import com.lowagie.text.html.simpleparser.HTMLWorker;
import com.lowagie.text.pdf.PdfWriter;
import com.postmortemai.application.port.out.PostMortemExporter;
import com.postmortemai.domain.model.PostMortem;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

@Component
public class PdfExporter implements PostMortemExporter {

    private final MarkdownExporter markdownExporter;

    public PdfExporter(MarkdownExporter markdownExporter) {
        this.markdownExporter = markdownExporter;
    }

    @Override
    public byte[] export(PostMortem postMortem) {
        try {
            // 1. Get raw markdown
            byte[] markdownBytes = markdownExporter.export(postMortem);
            String markdown = new String(markdownBytes, StandardCharsets.UTF_8);

            // 2. Convert to HTML with SUPPRESS_HTML to strip malicious <script> / <iframe>
            com.vladsch.flexmark.util.data.MutableDataSet options = new com.vladsch.flexmark.util.data.MutableDataSet();
            options.set(HtmlRenderer.SUPPRESS_HTML, true);

            Parser parser = Parser.builder(options).build();
            HtmlRenderer renderer = HtmlRenderer.builder(options).build();

            Node documentNode = parser.parse(markdown);
            String html = renderer.render(documentNode);

            // 3. Convert HTML to PDF using OpenPDF
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                Document document = new Document();
                PdfWriter.getInstance(document, baos);
                document.open();

                HTMLWorker htmlWorker = new HTMLWorker(document);
                htmlWorker.parse(new StringReader(html));

                document.close();
                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to export PostMortem to PDF", e);
        }
    }

    @Override
    public boolean supports(com.postmortemai.application.dto.ExportFormat format) {
        return format == com.postmortemai.application.dto.ExportFormat.PDF;
    }
}
