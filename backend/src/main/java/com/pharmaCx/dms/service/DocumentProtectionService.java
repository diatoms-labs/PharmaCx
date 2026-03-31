package com.pharmaCx.dms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Manages OOXML-level document protection in .docx files.
 *
 * Word documents can have protection set in word/settings.xml via
 * <w:documentProtection> element. This service can:
 * - Strip protection so authors can edit the document body
 * - Apply read-only protection for published/view-only documents
 *
 * All operations work at the ZIP/XML level and preserve every other
 * aspect of the document (styles, formatting, media, headers, footers, etc.).
 */
@Service
public class DocumentProtectionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProtectionService.class);

    private final FileStorageService fileStorageService;

    public DocumentProtectionService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /** Matches the <w:documentProtection .../> element (self-closing or with content) */
    private static final Pattern PROTECTION_PATTERN = Pattern.compile(
            "<w:documentProtection[^>]*/>|<w:documentProtection[^>]*>[\\s\\S]*?</w:documentProtection>",
            Pattern.DOTALL
    );

    /** Matches <w:enforceProtection .../> (used in some Word versions) */
    private static final Pattern ENFORCE_PROTECTION_PATTERN = Pattern.compile(
            "<w:enforceProtection[^>]*/>"
    );

    /** Matches <w:comment> elements in word/comments.xml */
    private static final Pattern COMMENT_ELEMENT_PATTERN = Pattern.compile(
            "<w:comment\\b[^>]*/>|<w:comment\\b[^>]*>[\\s\\S]*?</w:comment>",
            Pattern.DOTALL
    );

    /** Matches comment range markers and references in document body XML */
    private static final Pattern COMMENT_RANGE_START_PATTERN = Pattern.compile(
            "<w:commentRangeStart[^>]*/>"
    );
    private static final Pattern COMMENT_RANGE_END_PATTERN = Pattern.compile(
            "<w:commentRangeEnd[^>]*/>"
    );
    private static final Pattern COMMENT_REFERENCE_PATTERN = Pattern.compile(
            "<w:commentReference[^>]*/>"
    );

    // ═══════ Tracked change acceptance patterns ═══════

    /** Deletions: remove entirely (deleted text disappears when accepted) */
    private static final Pattern DEL_PATTERN = Pattern.compile(
            "<w:del\\b[^>]*>[\\s\\S]*?</w:del>", Pattern.DOTALL
    );

    /** Insertions: unwrap (keep content, remove wrapper) */
    private static final Pattern INS_OPEN_PATTERN = Pattern.compile("<w:ins\\b[^>]*>");
    private static final Pattern INS_CLOSE_PATTERN = Pattern.compile("</w:ins>");

    /** Move-from: remove entirely (like deletion — the original location text disappears) */
    private static final Pattern MOVE_FROM_PATTERN = Pattern.compile(
            "<w:moveFrom\\b[^>]*>[\\s\\S]*?</w:moveFrom>", Pattern.DOTALL
    );

    /** Move-to: unwrap (like insertion — keep the moved content at new location) */
    private static final Pattern MOVE_TO_OPEN_PATTERN = Pattern.compile("<w:moveTo\\b[^>]*>");
    private static final Pattern MOVE_TO_CLOSE_PATTERN = Pattern.compile("</w:moveTo>");

    /** Move range bookmark markers */
    private static final Pattern MOVE_FROM_RANGE_START = Pattern.compile("<w:moveFromRangeStart[^>]*/>");
    private static final Pattern MOVE_FROM_RANGE_END = Pattern.compile("<w:moveFromRangeEnd[^>]*/>");
    private static final Pattern MOVE_TO_RANGE_START = Pattern.compile("<w:moveToRangeStart[^>]*/>");
    private static final Pattern MOVE_TO_RANGE_END = Pattern.compile("<w:moveToRangeEnd[^>]*/>");

    /** Property change elements: remove (keep current formatting, discard change history) */
    private static final Pattern RPR_CHANGE_PATTERN = Pattern.compile(
            "<w:rPrChange\\b[^>]*>[\\s\\S]*?</w:rPrChange>", Pattern.DOTALL
    );
    private static final Pattern PPR_CHANGE_PATTERN = Pattern.compile(
            "<w:pPrChange\\b[^>]*>[\\s\\S]*?</w:pPrChange>", Pattern.DOTALL
    );
    private static final Pattern SECT_PR_CHANGE_PATTERN = Pattern.compile(
            "<w:sectPrChange\\b[^>]*>[\\s\\S]*?</w:sectPrChange>", Pattern.DOTALL
    );
    private static final Pattern TBL_PR_CHANGE_PATTERN = Pattern.compile(
            "<w:tblPrChange\\b[^>]*>[\\s\\S]*?</w:tblPrChange>", Pattern.DOTALL
    );
    private static final Pattern TR_PR_CHANGE_PATTERN = Pattern.compile(
            "<w:trPrChange\\b[^>]*>[\\s\\S]*?</w:trPrChange>", Pattern.DOTALL
    );
    private static final Pattern TC_PR_CHANGE_PATTERN = Pattern.compile(
            "<w:tcPrChange\\b[^>]*>[\\s\\S]*?</w:tcPrChange>", Pattern.DOTALL
    );

    /** Pattern to match XML entries that contain tracked changes */
    private static final Pattern TRACKED_CHANGE_TARGET = Pattern.compile(
            "^word/(document|header\\d+|footer\\d+|footnotes|endnotes)\\.xml$"
    );

    /**
     * Remove document protection from a stored file so it can be freely edited.
     * This modifies the file in-place (same fileId).
     *
     * Called when a document enters AUTHOR_DRAFT state — the author needs
     * full editing access regardless of any protection the template had.
     */
    public void stripProtection(String fileId) {
        if (fileId == null || !fileStorageService.fileExists(fileId)) {
            log.warn("Cannot strip protection — file not found: {}", fileId);
            return;
        }

        Path filePath = fileStorageService.getFilePath(fileId);
        if (filePath == null) return;

        try {
            byte[] processed = processZipForProtection(filePath, ProtectionAction.STRIP);
            fileStorageService.storeFileWithId(fileId, new ByteArrayInputStream(processed), "docx");
            log.info("Stripped document protection from file: {}", fileId);
        } catch (Exception e) {
            log.error("Failed to strip document protection from file {}: {}", fileId, e.getMessage(), e);
        }
    }

    /**
     * Apply read-only protection to a stored file.
     * This modifies the file in-place (same fileId).
     *
     * Called when a document is PUBLISHED — prevents accidental edits
     * even if someone opens the raw file.
     */
    public void applyReadOnlyProtection(String fileId) {
        if (fileId == null || !fileStorageService.fileExists(fileId)) {
            log.warn("Cannot apply protection — file not found: {}", fileId);
            return;
        }

        Path filePath = fileStorageService.getFilePath(fileId);
        if (filePath == null) return;

        try {
            byte[] processed = processZipForProtection(filePath, ProtectionAction.APPLY_READONLY);
            fileStorageService.storeFileWithId(fileId, new ByteArrayInputStream(processed), "docx");
            log.info("Applied read-only protection to file: {}", fileId);
        } catch (Exception e) {
            log.error("Failed to apply protection to file {}: {}", fileId, e.getMessage(), e);
        }
    }

    /**
     * Apply tracked-changes-only protection to a stored file.
     * Users can only make tracked changes (review mode) — no direct editing.
     *
     * Called when a document enters PEER_REVIEW or QA_REVIEW state.
     */
    public void applyTrackedChangesProtection(String fileId) {
        if (fileId == null || !fileStorageService.fileExists(fileId)) {
            log.warn("Cannot apply tracked changes protection — file not found: {}", fileId);
            return;
        }

        Path filePath = fileStorageService.getFilePath(fileId);
        if (filePath == null) return;

        try {
            byte[] processed = processZipForProtection(filePath, ProtectionAction.APPLY_TRACKED_CHANGES);
            fileStorageService.storeFileWithId(fileId, new ByteArrayInputStream(processed), "docx");
            log.info("Applied tracked-changes protection to file: {}", fileId);
        } catch (Exception e) {
            log.error("Failed to apply tracked changes protection to file {}: {}", fileId, e.getMessage(), e);
        }
    }

    /**
     * Remove all comments from a stored DOCX file.
     * Clears word/comments.xml content and removes comment range markers
     * from word/document.xml. Tracked changes are preserved.
     *
     * Called when a document is PUBLISHED — comments are no longer needed
     * but the change tracking history remains visible.
     */
    public void clearComments(String fileId) {
        if (fileId == null || !fileStorageService.fileExists(fileId)) {
            log.warn("Cannot clear comments — file not found: {}", fileId);
            return;
        }

        Path filePath = fileStorageService.getFilePath(fileId);
        if (filePath == null) return;

        try {
            byte[] processed = processZipClearComments(filePath);
            fileStorageService.storeFileWithId(fileId, new ByteArrayInputStream(processed), "docx");
            log.info("Cleared all comments from file: {}", fileId);
        } catch (Exception e) {
            log.error("Failed to clear comments from file {}: {}", fileId, e.getMessage(), e);
        }
    }

    /**
     * Accept all tracked changes in a stored DOCX file at the OOXML level.
     * - Insertions (w:ins): unwrapped — inserted text becomes normal text
     * - Deletions (w:del): removed entirely — deleted text disappears
     * - Move-from (w:moveFrom): removed entirely
     * - Move-to (w:moveTo): unwrapped — moved text stays at new location
     * - Property changes (*PrChange): removed — current formatting kept
     *
     * Called when a review step approves (to give next reviewer a clean document)
     * and at publish time (to produce a clean final document per pharma SOP).
     */
    public void acceptAllTrackedChanges(String fileId) {
        if (fileId == null || !fileStorageService.fileExists(fileId)) {
            log.warn("Cannot accept tracked changes — file not found: {}", fileId);
            return;
        }

        Path filePath = fileStorageService.getFilePath(fileId);
        if (filePath == null) return;

        try {
            byte[] processed = processZipAcceptTrackedChanges(filePath);
            fileStorageService.storeFileWithId(fileId, new ByteArrayInputStream(processed), "docx");
            log.info("Accepted all tracked changes in file: {}", fileId);
        } catch (Exception e) {
            log.error("Failed to accept tracked changes in file {}: {}", fileId, e.getMessage(), e);
        }
    }

    private byte[] processZipAcceptTrackedChanges(Path filePath) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(filePath));
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] content = zis.readAllBytes();
                String entryName = entry.getName();

                ZipEntry newEntry = new ZipEntry(entryName);
                zos.putNextEntry(newEntry);

                if (TRACKED_CHANGE_TARGET.matcher(entryName).matches()) {
                    String xml = new String(content, StandardCharsets.UTF_8);
                    xml = acceptTrackedChangesInXml(xml);
                    zos.write(xml.getBytes(StandardCharsets.UTF_8));
                } else {
                    zos.write(content);
                }

                zos.closeEntry();
            }
        }

        return baos.toByteArray();
    }

    private String acceptTrackedChangesInXml(String xml) {
        // Order matters: remove deletions FIRST, then unwrap insertions, then property changes

        // Step 1: Remove deletions and move-from entirely
        xml = DEL_PATTERN.matcher(xml).replaceAll("");
        xml = MOVE_FROM_PATTERN.matcher(xml).replaceAll("");

        // Step 2: Unwrap insertions and move-to (keep content, remove wrapper tags)
        xml = INS_OPEN_PATTERN.matcher(xml).replaceAll("");
        xml = INS_CLOSE_PATTERN.matcher(xml).replaceAll("");
        xml = MOVE_TO_OPEN_PATTERN.matcher(xml).replaceAll("");
        xml = MOVE_TO_CLOSE_PATTERN.matcher(xml).replaceAll("");

        // Step 3: Remove move range bookmark markers
        xml = MOVE_FROM_RANGE_START.matcher(xml).replaceAll("");
        xml = MOVE_FROM_RANGE_END.matcher(xml).replaceAll("");
        xml = MOVE_TO_RANGE_START.matcher(xml).replaceAll("");
        xml = MOVE_TO_RANGE_END.matcher(xml).replaceAll("");

        // Step 4: Remove all property change elements (keep current formatting)
        xml = RPR_CHANGE_PATTERN.matcher(xml).replaceAll("");
        xml = PPR_CHANGE_PATTERN.matcher(xml).replaceAll("");
        xml = SECT_PR_CHANGE_PATTERN.matcher(xml).replaceAll("");
        xml = TBL_PR_CHANGE_PATTERN.matcher(xml).replaceAll("");
        xml = TR_PR_CHANGE_PATTERN.matcher(xml).replaceAll("");
        xml = TC_PR_CHANGE_PATTERN.matcher(xml).replaceAll("");

        return xml;
    }

    private byte[] processZipClearComments(Path filePath) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(filePath));
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] content = zis.readAllBytes();
                String entryName = entry.getName();

                ZipEntry newEntry = new ZipEntry(entryName);
                zos.putNextEntry(newEntry);

                if ("word/comments.xml".equals(entryName)) {
                    // Remove all <w:comment> elements but keep the XML shell
                    String xml = new String(content, StandardCharsets.UTF_8);
                    xml = COMMENT_ELEMENT_PATTERN.matcher(xml).replaceAll("");
                    zos.write(xml.getBytes(StandardCharsets.UTF_8));
                } else if ("word/commentsExtended.xml".equals(entryName)) {
                    // Also clear extended comments (used by newer Word/OnlyOffice)
                    String xml = new String(content, StandardCharsets.UTF_8);
                    xml = xml.replaceAll("<w15:commentEx[^>]*/>", "");
                    zos.write(xml.getBytes(StandardCharsets.UTF_8));
                } else if ("word/document.xml".equals(entryName)) {
                    // Remove comment range markers and references from document body
                    String xml = new String(content, StandardCharsets.UTF_8);
                    xml = COMMENT_RANGE_START_PATTERN.matcher(xml).replaceAll("");
                    xml = COMMENT_RANGE_END_PATTERN.matcher(xml).replaceAll("");
                    xml = COMMENT_REFERENCE_PATTERN.matcher(xml).replaceAll("");
                    zos.write(xml.getBytes(StandardCharsets.UTF_8));
                } else {
                    zos.write(content);
                }

                zos.closeEntry();
            }
        }

        return baos.toByteArray();
    }

    private byte[] processZipForProtection(Path filePath, ProtectionAction action) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(filePath));
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                byte[] content = zis.readAllBytes();
                String entryName = entry.getName();

                ZipEntry newEntry = new ZipEntry(entryName);
                zos.putNextEntry(newEntry);

                if ("word/settings.xml".equals(entryName)) {
                    String xml = new String(content, StandardCharsets.UTF_8);
                    xml = modifyProtection(xml, action);
                    zos.write(xml.getBytes(StandardCharsets.UTF_8));
                } else {
                    zos.write(content);
                }

                zos.closeEntry();
            }
        }

        return baos.toByteArray();
    }

    private String modifyProtection(String settingsXml, ProtectionAction action) {
        switch (action) {
            case STRIP:
                // Remove all protection elements
                settingsXml = PROTECTION_PATTERN.matcher(settingsXml).replaceAll("");
                settingsXml = ENFORCE_PROTECTION_PATTERN.matcher(settingsXml).replaceAll("");
                break;

            case APPLY_READONLY:
                // First strip existing protection, then add read-only
                settingsXml = PROTECTION_PATTERN.matcher(settingsXml).replaceAll("");
                settingsXml = ENFORCE_PROTECTION_PATTERN.matcher(settingsXml).replaceAll("");
                // Insert before </w:settings>
                String readOnlyProtection = "<w:documentProtection w:edit=\"readOnly\" w:enforcement=\"1\"/>";
                settingsXml = settingsXml.replace("</w:settings>", readOnlyProtection + "</w:settings>");
                break;

            case APPLY_TRACKED_CHANGES:
                // First strip existing protection, then add tracked changes
                settingsXml = PROTECTION_PATTERN.matcher(settingsXml).replaceAll("");
                settingsXml = ENFORCE_PROTECTION_PATTERN.matcher(settingsXml).replaceAll("");
                // trackedChanges enforcement: users can only make tracked changes
                String tcProtection = "<w:documentProtection w:edit=\"trackedChanges\" w:enforcement=\"1\"/>";
                settingsXml = settingsXml.replace("</w:settings>", tcProtection + "</w:settings>");
                break;
        }
        return settingsXml;
    }

    private enum ProtectionAction {
        STRIP,
        APPLY_READONLY,
        APPLY_TRACKED_CHANGES
    }
}
