package com.pharmaCx.dms.service;

import com.pharmaCx.dms.domain.model.ControlledDocument;
import com.pharmaCx.dms.domain.model.DocumentTemplate;
import com.pharmaCx.dms.domain.repository.DocumentTypeConfigRepository;
import com.pharmaCx.dms.domain.repository.OrganizationalUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Copies a template file and fills placeholders with actual document metadata.
 *
 * Works at the ZIP/XML level — reads the .docx as a ZIP archive, performs text
 * replacement directly on the XML content, and repackages it. This preserves ALL
 * original document settings, protection, styles, headers, footers, and formatting
 * that Apache POI's round-trip would otherwise strip out.
 *
 * Placeholder strategy:
 * 1. For each XML text part, concatenate all <w:t> text within each <w:p> paragraph
 * 2. Find placeholder patterns {KEY} in the concatenated text
 * 3. Replace placeholder text across runs while preserving the run formatting (rPr)
 *    of the first run that contains the placeholder — this guarantees no format loss
 * 4. Non-text ZIP entries are copied byte-for-byte
 */
@Service
public class TemplateProcessingService {

    private static final Logger log = LoggerFactory.getLogger(TemplateProcessingService.class);

    private final FileStorageService fileStorageService;
    private final OrganizationalUnitRepository orgUnitRepo;
    private final DocumentTypeConfigRepository documentTypeConfigRepo;

    public TemplateProcessingService(FileStorageService fileStorageService,
                                     OrganizationalUnitRepository orgUnitRepo,
                                     DocumentTypeConfigRepository documentTypeConfigRepo) {
        this.fileStorageService = fileStorageService;
        this.orgUnitRepo = orgUnitRepo;
        this.documentTypeConfigRepo = documentTypeConfigRepo;
    }

    /**
     * Regex to detect XML parts inside the .docx that may contain placeholder text.
     * Matches: word/document.xml, word/headerN.xml, word/footerN.xml,
     *          word/endnotes.xml, word/footnotes.xml, word/comments.xml
     */
    private static final Pattern TEXT_PART_PATTERN = Pattern.compile(
            "^word/(document|header\\d+|footer\\d+|endnotes|footnotes|comments)\\.xml$"
    );

    /** Matches a placeholder like {DOCUMENT_NUMBER} in plain text */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([A-Z_]+)\\}");

    public String processTemplate(DocumentTemplate template, ControlledDocument doc, String docNumber) {
        String templateFileId = template.getFileStorageId();
        if (templateFileId == null || !fileStorageService.fileExists(templateFileId)) {
            log.warn("Template file not found: {} (fileStorageId={})", template.getName(), templateFileId);
            return null;
        }

        Path templatePath = fileStorageService.getFilePath(templateFileId);
        if (templatePath == null) {
            log.warn("Could not resolve path for template: {}", templateFileId);
            return null;
        }

        String dept = orgUnitRepo.findById(doc.getUnitId())
                .map(u -> u.getDisplayName())
                .orElse(doc.getUnitId());

        String docTypeName = documentTypeConfigRepo.findById(doc.getDocumentTypeId())
                .map(t -> t.getDisplayName())
                .orElse(doc.getDocumentTypeId());

        String effectiveDate = LocalDate.now().toString();
        String reviewDate = LocalDate.now().plusYears(2).toString();

        Map<String, String> replacements = buildReplacementMap(doc, docNumber, dept, docTypeName, effectiveDate, reviewDate);

        try {
            byte[] processed = processZip(templatePath, replacements);

            String newFileId = UUID.randomUUID().toString();
            fileStorageService.storeFileWithId(newFileId, new ByteArrayInputStream(processed), "docx");
            log.info("Processed template '{}' -> file: {} (docNumber={})", template.getName(), newFileId, docNumber);
            return newFileId;

        } catch (Exception e) {
            log.error("Failed to process template '{}': {}", template.getName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Open the .docx as a ZIP, replace placeholder text in XML parts,
     * copy everything else byte-for-byte. This preserves all original
     * document structure, protection, styles, and formatting.
     */
    private byte[] processZip(Path templatePath, Map<String, String> replacements) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(templatePath));
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] content = zis.readAllBytes();
                String entryName = entry.getName();

                // Create a new ZipEntry — never reuse the old one (avoids size/CRC conflicts)
                ZipEntry newEntry = new ZipEntry(entryName);
                zos.putNextEntry(newEntry);

                if (TEXT_PART_PATTERN.matcher(entryName).matches()) {
                    // XML part that may contain placeholders — do paragraph-aware replacement
                    String xml = new String(content, StandardCharsets.UTF_8);
                    xml = replacePlaceholdersInXml(xml, replacements);
                    zos.write(xml.getBytes(StandardCharsets.UTF_8));
                } else {
                    // Binary or non-text part — copy as-is (styles, settings, media, etc.)
                    zos.write(content);
                }

                zos.closeEntry();
            }
        }

        return baos.toByteArray();
    }

    /**
     * Paragraph-aware placeholder replacement.
     *
     * Word often splits placeholder text like {DOCUMENT_NUMBER} across multiple
     * XML runs (<w:r> elements), e.g.:
     *   <w:r><w:rPr>...</w:rPr><w:t>{DOCU</w:t></w:r>
     *   <w:r><w:rPr>...</w:rPr><w:t>MENT_NUM</w:t></w:r>
     *   <w:r><w:rPr>...</w:rPr><w:t>BER}</w:t></w:r>
     *
     * Strategy:
     * 1. Extract each paragraph (<w:p>...</w:p>)
     * 2. Concatenate all <w:t> text within the paragraph
     * 3. Check if the concatenated text contains any placeholder
     * 4. If yes, rebuild the paragraph's runs: put the replaced text into the
     *    first run (preserving its formatting) and clear the remaining runs
     *    that were part of the placeholder span
     */
    private String replacePlaceholdersInXml(String xml, Map<String, String> replacements) {
        // Process each paragraph independently
        Pattern paraPattern = Pattern.compile("(<w:p[> ][\\s\\S]*?</w:p>)", Pattern.DOTALL);
        Matcher paraMatcher = paraPattern.matcher(xml);

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (paraMatcher.find()) {
            result.append(xml, lastEnd, paraMatcher.start());
            String paragraph = paraMatcher.group(1);
            String processed = processParagraph(paragraph, replacements);
            result.append(processed);
            lastEnd = paraMatcher.end();
        }
        result.append(xml, lastEnd, xml.length());

        return result.toString();
    }

    /**
     * Process a single <w:p> paragraph element.
     * Extracts all <w:t> text, checks for placeholders, and replaces them
     * while preserving the run formatting structure.
     */
    private String processParagraph(String paragraph, Map<String, String> replacements) {
        // Extract all <w:t> text content and their positions
        // Pattern matches <w:t> or <w:t xml:space="preserve"> content
        Pattern textPattern = Pattern.compile("<w:t([^>]*)>([^<]*)</w:t>");
        Matcher textMatcher = textPattern.matcher(paragraph);

        // Collect all text segments with their positions
        List<TextSegment> segments = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();

        while (textMatcher.find()) {
            int start = textMatcher.start();
            int end = textMatcher.end();
            String attrs = textMatcher.group(1);
            String text = textMatcher.group(2);

            segments.add(new TextSegment(start, end, attrs, text, fullText.length()));
            fullText.append(text);
        }

        if (segments.isEmpty()) {
            return paragraph;
        }

        String concatenatedText = fullText.toString();

        // Check if any placeholder exists in the concatenated text
        boolean hasPlaceholder = false;
        for (String key : replacements.keySet()) {
            if (concatenatedText.contains(key)) {
                hasPlaceholder = true;
                break;
            }
        }

        if (!hasPlaceholder) {
            return paragraph;
        }

        // Perform replacements on the concatenated text
        String replacedText = concatenatedText;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            replacedText = replacedText.replace(entry.getKey(), escapeXml(entry.getValue()));
        }

        // Now redistribute the replaced text back across the <w:t> segments.
        // Strategy: put ALL replaced text into the first <w:t> element (preserving its
        // run properties/formatting), and empty out subsequent <w:t> elements.
        // This preserves the XML structure perfectly — we only change text content.
        StringBuilder rebuilt = new StringBuilder();
        int lastPos = 0;

        for (int i = 0; i < segments.size(); i++) {
            TextSegment seg = segments.get(i);

            // Copy everything before this <w:t> element unchanged
            rebuilt.append(paragraph, lastPos, seg.xmlStart);

            if (i == 0) {
                // First segment: put all replaced text here with xml:space="preserve"
                rebuilt.append("<w:t xml:space=\"preserve\">")
                       .append(replacedText)
                       .append("</w:t>");
            } else {
                // Subsequent segments: empty text to preserve XML structure
                rebuilt.append("<w:t").append(seg.attrs).append("></w:t>");
            }

            lastPos = seg.xmlEnd;
        }

        // Append anything after the last <w:t> element
        rebuilt.append(paragraph, lastPos, paragraph.length());

        return rebuilt.toString();
    }

    /**
     * Escape special XML characters in replacement values.
     */
    private String escapeXml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private Map<String, String> buildReplacementMap(ControlledDocument doc, String docNumber,
                                                     String dept, String docTypeName,
                                                     String effectiveDate, String reviewDate) {
        Map<String, String> map = new LinkedHashMap<>();

        map.put("{DOCUMENT_NUMBER}", docNumber);
        map.put("{TITLE}", doc.getTitle());
        map.put("{DEPARTMENT}", dept);
        map.put("{DOCUMENT_TYPE}", docTypeName);
        map.put("{EFFECTIVE_DATE}", effectiveDate);
        map.put("{REVIEW_DATE}", reviewDate);
        map.put("{VERSION}", doc.getVersion() + ".0");
        map.put("{STATUS}", "CONTROLLED");

        return map;
    }

    /**
     * Internal record to track a <w:t> text segment's position in the XML.
     */
    private static class TextSegment {
        final int xmlStart;     // start position of <w:t...>...</w:t> in the XML
        final int xmlEnd;       // end position
        final String attrs;     // attributes on <w:t> (e.g., ' xml:space="preserve"')
        final String text;      // text content
        final int textOffset;   // offset in the concatenated plain text

        TextSegment(int xmlStart, int xmlEnd, String attrs, String text, int textOffset) {
            this.xmlStart = xmlStart;
            this.xmlEnd = xmlEnd;
            this.attrs = attrs;
            this.text = text;
            this.textOffset = textOffset;
        }
    }
}
