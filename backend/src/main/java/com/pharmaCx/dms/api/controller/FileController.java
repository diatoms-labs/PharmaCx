package com.pharmaCx.dms.api.controller;

import com.pharmaCx.dms.config.AiConfig;
import com.pharmaCx.dms.config.OnlyOfficeConfig;
import com.pharmaCx.dms.domain.model.SystemSetting;
import com.pharmaCx.dms.domain.repository.SystemSettingRepository;
import com.pharmaCx.dms.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileStorageService fileStorageService;
    private final OnlyOfficeConfig onlyOfficeConfig;
    private final SystemSettingRepository settingRepo;
    private final AiConfig aiConfig;

    /**
     * Tracks when each fileId was last saved via the OnlyOffice callback.
     * Frontend polls this to confirm save before workflow transitions.
     */
    private static final ConcurrentHashMap<String, Long> lastSaveTimestamps = new ConcurrentHashMap<>();

    public FileController(FileStorageService fileStorageService, 
                          OnlyOfficeConfig onlyOfficeConfig,
                          SystemSettingRepository settingRepo,
                          AiConfig aiConfig) {
        this.fileStorageService = fileStorageService;
        this.onlyOfficeConfig = onlyOfficeConfig;
        this.settingRepo = settingRepo;
        this.aiConfig = aiConfig;
    }

    /**
     * Serve a document file to ONLYOFFICE Document Server (or browser download).
     * No auth required — Document Server needs direct access.
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> serveDocumentFile(@PathVariable String fileId) {
        Resource resource = fileStorageService.loadFile(fileId);
        if (resource == null || !resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        String ext = fileStorageService.getFileExtension(fileId);
        String contentType = switch (ext) {
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"file." + ext + "\"")
                .body(resource);
    }

    /**
     * Serve an external knowledge file. Path is relative to background-knowledge-path.
     * No auth required for OnlyOffice service calls.
     */
    @GetMapping("/external")
    public ResponseEntity<Resource> serveExternalFile(@RequestParam String relativePath) {
        String knowledgeRoot = aiConfig.getBackgroundKnowledgePath();
        String publishedRoot = aiConfig.getPublishedDocsPath();
        
        log.info("Request to serve external file: relativePath={}, knowledgeRoot={}, publishedRoot={}", 
                relativePath, knowledgeRoot, publishedRoot);

        // 1. Try Knowledge Root
        if (knowledgeRoot != null) {
            Path path = Paths.get(knowledgeRoot).resolve(relativePath).normalize();
            if (isValidExternalPath(path, knowledgeRoot)) return serveFileResource(path);
            
            // Try stripping prefix if it exists
            if (relativePath.contains("/")) {
                String filename = relativePath.substring(relativePath.lastIndexOf("/") + 1);
                Path altPath = Paths.get(knowledgeRoot).resolve(filename).normalize();
                if (isValidExternalPath(altPath, knowledgeRoot)) return serveFileResource(altPath);
            }
        }

        // 2. Try Published Root
        if (publishedRoot != null) {
            Path path = Paths.get(publishedRoot).resolve(relativePath).normalize();
            if (isValidExternalPath(path, publishedRoot)) return serveFileResource(path);

            if (relativePath.contains("/")) {
                String filename = relativePath.substring(relativePath.lastIndexOf("/") + 1);
                Path altPath = Paths.get(publishedRoot).resolve(filename).normalize();
                if (isValidExternalPath(altPath, publishedRoot)) return serveFileResource(altPath);
            }
        }

        log.warn("External file not consumed: {}", relativePath);
        return ResponseEntity.notFound().build();
    }

    private boolean isValidExternalPath(Path path, String root) {
        try {
            Path rootPath = Paths.get(root).toAbsolutePath().normalize();
            Path absPath = path.toAbsolutePath().normalize();
            return absPath.startsWith(rootPath) && Files.exists(absPath) && !Files.isDirectory(absPath);
        } catch (Exception e) {
            return false;
        }
    }

    private ResponseEntity<Resource> serveFileResource(Path path) {
        Resource resource = new FileSystemResource(path);
        String fileName = path.getFileName().toString();
        String contentType = fileName.toLowerCase().endsWith(".pdf") ? "application/pdf" : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

    /**
     * Callback endpoint for ONLYOFFICE Document Server.
     * Called when document is saved/closed by user.
     * No auth required — Document Server calls this directly.
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, Integer>> handleEditorCallback(
            @RequestParam String fileId,
            @RequestBody Map<String, Object> body) {

        int status = (int) body.getOrDefault("status", 0);

        // Status 4 = document closed with no changes — still counts as "save complete"
        if (status == 4) {
            lastSaveTimestamps.put(fileId, System.currentTimeMillis());
        }

        // Status 2 = document ready for saving, 6 = forcesave
        if (status == 2 || status == 6) {
            String downloadUrl = (String) body.get("url");
            if (downloadUrl != null) {
                try {
                    downloadUrl = resolveInternalDownloadUrl(downloadUrl);

                    URL url = new URL(downloadUrl);
                    try (InputStream in = url.openStream()) {
                        fileStorageService.storeFileWithId(fileId, in, "docx");
                    }
                    lastSaveTimestamps.put(fileId, System.currentTimeMillis());
                    log.info("Editor callback: saved fileId={} (status={})", fileId, status);
                } catch (Exception e) {
                    log.error("Editor callback: failed to save fileId={}: {}", fileId, e.getMessage());
                }
            }
        }

        return ResponseEntity.ok(Map.of("error", 0));
    }

    /**
     * Check if the OnlyOffice callback has saved this file after a given timestamp.
     */
    @GetMapping("/{fileId}/save-status")
    public ResponseEntity<Map<String, Object>> getSaveStatus(
            @PathVariable String fileId,
            @RequestParam long after) {
        Long lastSave = lastSaveTimestamps.get(fileId);
        boolean saved = lastSave != null && lastSave > after;
        return ResponseEntity.ok(Map.of("saved", saved));
    }

    private String resolveInternalDownloadUrl(String originalUrl) {
        try {
            URL original = new URL(originalUrl);
            URL configured = new URL(onlyOfficeConfig.getUrl());

            int port = configured.getPort();
            URL rewritten = new URL(configured.getProtocol(), configured.getHost(),
                    port == -1 ? configured.getDefaultPort() : port, original.getFile());

            return rewritten.toString();
        } catch (Exception e) {
            return originalUrl;
        }
    }
}
