package com.pharmaCx.dms.ai.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Read-only text extractor for DOCX and PDF files.
 *
 * DOCX strategy: open as ZIP (same pattern as DocumentProtectionService),
 * locate word/document.xml, parse <w:t> text nodes via SAX.
 * The source file is NEVER modified — no write-back, no POI in-memory
 * re-serialisation risk.
 *
 * PDF strategy: PDFBox PDDocument.load() from InputStream.
 * doc.save() is never called, so the source file is untouched.
 */
@Service
public class DocumentTextExtractor {

    private static final Logger log = LoggerFactory.getLogger(DocumentTextExtractor.class);

    /**
     * Extract all plain text from a document file.
     *
     * @param filePath path to the .docx or .pdf file on disk
     * @return extracted plain text, or empty string on failure
     */
    public String extract(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            log.warn("Cannot extract text — file not found: {}", filePath);
            return "";
        }

        String fileName = filePath.getFileName().toString().toLowerCase();
        try {
            if (fileName.endsWith(".docx")) {
                return extractFromDocx(filePath);
            } else if (fileName.endsWith(".pdf")) {
                return extractFromPdf(filePath);
            } else {
                log.warn("Unsupported file type for AI indexing: {}", fileName);
                return "";
            }
        } catch (Exception e) {
            log.error("Text extraction failed for {}: {}", filePath, e.getMessage());
            return "";
        }
    }

    // ── DOCX: ZIP → word/document.xml → SAX <w:t> ─────────────────────────────

    private String extractFromDocx(Path filePath) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(filePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    byte[] xmlBytes = zis.readAllBytes();
                    return parseDocumentXml(xmlBytes);
                }
            }
        }
        log.warn("word/document.xml not found in DOCX: {}", filePath);
        return "";
    }

    /**
     * SAX parse word/document.xml and collect all <w:t> text runs.
     * Adds a space between runs so words don't run together across XML elements.
     */
    private String parseDocumentXml(byte[] xmlBytes) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(false);
            // Disable external DTD/entity fetching for safety
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            SAXParser parser = factory.newSAXParser();
            WordTextHandler handler = new WordTextHandler();
            parser.parse(new ByteArrayInputStream(xmlBytes), handler);
            return handler.getText();
        } catch (Exception e) {
            log.warn("SAX parse failed on document.xml: {}", e.getMessage());
            return "";
        }
    }

    /** SAX handler that collects text inside <w:t> and <w:delText> elements. */
    private static class WordTextHandler extends DefaultHandler {
        private final StringBuilder sb = new StringBuilder();
        private boolean inTextRun = false;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            // w:t = normal text run, w:delText = tracked-deletion text (include for context)
            inTextRun = "w:t".equals(qName) || "w:delText".equals(qName);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if ("w:t".equals(qName) || "w:delText".equals(qName)) {
                inTextRun = false;
                // Add space between consecutive runs so words don't merge
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
            }
            // Add newline at paragraph end for readable chunking
            if ("w:p".equals(qName)) {
                sb.append('\n');
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (inTextRun) {
                sb.append(ch, start, length);
            }
        }

        public String getText() {
            return sb.toString().trim();
        }
    }

    // ── PDF: PDFBox read-only ──────────────────────────────────────────────────

    private String extractFromPdf(Path filePath) throws IOException {
        // PDFBox 3.x API: Loader.loadPDF(byte[]) — reads bytes into memory,
        // never writes to the source file. doc.save() is never called.
        byte[] pdfBytes = Files.readAllBytes(filePath);
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }
}
