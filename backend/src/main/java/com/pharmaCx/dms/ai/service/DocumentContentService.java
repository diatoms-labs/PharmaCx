package com.pharmaCx.dms.ai.service;

import com.pharmaCx.dms.domain.model.ControlledDocument;
import com.pharmaCx.dms.domain.repository.ControlledDocumentRepository;
import com.pharmaCx.dms.exception.ResourceNotFoundException;
import com.pharmaCx.dms.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Injects AI-generated text into a DOCX file by directly modifying word/document.xml
 * inside the ZIP container — no external library dependencies, no full-document re-parse.
 *
 * Insertion strategy:
 *   1. If sectionLabel is provided: grep for the label text in the XML (case-insensitive),
 *      locate the closing </w:p> tag of the paragraph that contains it, insert after it.
 *   2. Fallback: insert immediately before </w:body>.
 *
 * The original file is replaced atomically via a temp-file rename.
 * After replacement, doc.updatedAt is bumped so OnlyOffice generates a new document key
 * and reloads from disk rather than serving the stale cached version.
 */
@Service
public class DocumentContentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentContentService.class);

    private final ControlledDocumentRepository documentRepo;
    private final FileStorageService fileStorageService;

    public DocumentContentService(ControlledDocumentRepository documentRepo,
                                   FileStorageService fileStorageService) {
        this.documentRepo = documentRepo;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Insert content into the DOCX for the given document.
     *
     * @param documentId   MongoDB document ID
     * @param content      plain text content to insert (may contain newlines)
     * @param sectionLabel optional section hint — if found in doc XML, content is inserted
     *                     after that paragraph; otherwise appended before &lt;/w:body&gt;
     * @throws IOException if file I/O fails
     */
    public void insertContent(String documentId, String content, String sectionLabel) throws IOException {
        ControlledDocument doc = documentRepo.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        if (doc.getDocumentFileId() == null) {
            throw new IllegalStateException("Document has no file attached: " + documentId);
        }

        Path filePath = fileStorageService.getFilePath(doc.getDocumentFileId());
        if (filePath == null || !Files.exists(filePath)) {
            throw new IllegalStateException("Document file not found on disk: " + doc.getDocumentFileId());
        }

        // Only DOCX files are supported for injection
        if (!filePath.getFileName().toString().toLowerCase().endsWith(".docx")) {
            throw new IllegalStateException("AI content insertion only supports DOCX files.");
        }

        Path tempFile = Files.createTempFile("app.ai-insert-", ".docx");
        try {
            rewriteDocx(filePath, tempFile, content, sectionLabel);
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Files.deleteIfExists(tempFile);
            throw e;
        }

        // Bump updatedAt so OnlyOffice generates a new document key on next load
        doc.setUpdatedAt(Instant.now());
        documentRepo.save(doc);

        log.info("AI content injected into document {} (fileId={})", documentId, doc.getDocumentFileId());
    }

    // ── ZIP rewrite ───────────────────────────────────────────────────────────

    /**
     * Copy the DOCX ZIP entry-by-entry. Only word/document.xml is modified;
     * all other entries (styles, relationships, media, etc.) are copied as-is.
     */
    private void rewriteDocx(Path source, Path dest, String content, String sectionLabel) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(source));
             ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(dest))) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ZipEntry outEntry = new ZipEntry(entry.getName());

                if ("word/document.xml".equals(entry.getName())) {
                    byte[] originalXml = zis.readAllBytes();
                    String xml = new String(originalXml, StandardCharsets.UTF_8);
                    String modified = injectIntoDocumentXml(xml, content, sectionLabel);
                    byte[] modifiedBytes = modified.getBytes(StandardCharsets.UTF_8);
                    zos.putNextEntry(outEntry);
                    zos.write(modifiedBytes);
                } else {
                    zos.putNextEntry(outEntry);
                    copyStream(zis, zos);
                }
                zos.closeEntry();
            }
        }
    }

    // ── XML injection ─────────────────────────────────────────────────────────

    /**
     * Inject content paragraphs into the document XML string.
     *
     * If sectionLabel is provided and found in the XML, the new paragraphs are
     * inserted after the closing &lt;/w:p&gt; of the paragraph containing that text.
     * Otherwise they are inserted immediately before &lt;/w:body&gt;.
     */
    private String injectIntoDocumentXml(String xml, String content, String sectionLabel) {
        String paragraphsXml = buildParagraphsXml(content, sectionLabel);

        // Try section-label-based insertion
        if (sectionLabel != null && !sectionLabel.isBlank()) {
            String needle = sectionLabel.trim().toLowerCase();
            String xmlLower = xml.toLowerCase();
            int labelIdx = xmlLower.indexOf(needle);
            if (labelIdx >= 0) {
                // Find the </w:p> that closes the paragraph containing the label
                int pEndIdx = xml.indexOf("</w:p>", labelIdx);
                if (pEndIdx >= 0) {
                    int insertAt = pEndIdx + "</w:p>".length();
                    log.debug("Inserting after section '{}' at XML offset {}", sectionLabel, insertAt);
                    return xml.substring(0, insertAt) + paragraphsXml + xml.substring(insertAt);
                }
            }
            log.info("Section label '{}' not found in document XML — appending to body", sectionLabel);
        }

        // Fallback: append before </w:body>
        int bodyEnd = xml.lastIndexOf("</w:body>");
        if (bodyEnd >= 0) {
            return xml.substring(0, bodyEnd) + paragraphsXml + xml.substring(bodyEnd);
        }

        // Last resort — just append (malformed doc)
        log.warn("No </w:body> found in document XML — appending content at end");
        return xml + paragraphsXml;
    }

    /**
     * Build OOXML paragraphs for the given content.
     *
     * The optional sectionLabel is rendered as a bold/italic header paragraph
     * (using a distinct run property) to visually identify AI-inserted blocks.
     * Content lines are split on newline; empty lines produce empty paragraphs.
     */
    private String buildParagraphsXml(String content, String sectionLabel) {
        StringBuilder sb = new StringBuilder();

        // Header paragraph: "[AI — <sectionLabel>]" or just "[AI]" if no label
        String headerText = (sectionLabel != null && !sectionLabel.isBlank())
                ? "[AI \u2014 " + sectionLabel.trim() + "]"
                : "[AI]";
        sb.append("<w:p>")
          .append("<w:pPr><w:spacing w:before=\"120\" w:after=\"60\"/></w:pPr>")
          .append("<w:r>")
          .append("<w:rPr><w:i/><w:color w:val=\"4472C4\"/><w:sz w:val=\"18\"/></w:rPr>")
          .append("<w:t xml:space=\"preserve\">").append(escapeXml(headerText)).append("</w:t>")
          .append("</w:r>")
          .append("</w:p>");

        // Content paragraphs — one per line
        String[] lines = content.split("\n", -1);
        for (String line : lines) {
            sb.append("<w:p>")
              .append("<w:pPr><w:spacing w:after=\"60\"/></w:pPr>")
              .append("<w:r>")
              .append("<w:t xml:space=\"preserve\">").append(escapeXml(line)).append("</w:t>")
              .append("</w:r>")
              .append("</w:p>");
        }

        // Trailing blank paragraph for separation
        sb.append("<w:p/>");

        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String escapeXml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static void copyStream(InputStream in, java.io.OutputStream out) throws IOException {
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            out.write(buf, 0, n);
        }
    }
}
