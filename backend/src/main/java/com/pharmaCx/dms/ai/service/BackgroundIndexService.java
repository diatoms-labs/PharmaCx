package com.pharmaCx.dms.ai.service;

import com.pharmaCx.dms.ai.model.DocumentChunk;
import com.pharmaCx.dms.ai.repository.DocumentChunkRepository;
import com.pharmaCx.dms.config.AiConfig;
import com.pharmaCx.dms.domain.enums.DocumentStatus;
import com.pharmaCx.dms.domain.model.ControlledDocument;
import com.pharmaCx.dms.domain.model.SystemSetting;
import com.pharmaCx.dms.domain.repository.ControlledDocumentRepository;
import com.pharmaCx.dms.domain.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Enterprise Background Knowledge Indexer & Virtualizer.
 * Scans external folders, registers files as VIRTUAL documents in the DMS,
 * and indexes them for RAG and unified search.
 */
@Service
public class BackgroundIndexService {

    private static final Logger log = LoggerFactory.getLogger(BackgroundIndexService.class);

    private final DocumentChunkRepository chunkRepo;
    private final ControlledDocumentRepository documentRepo;
    private final SystemSettingRepository settingRepo;
    private final OllamaClient ollamaClient;
    private final AiConfig aiConfig;
    private final DocumentTextExtractor textExtractor;

    public BackgroundIndexService(DocumentChunkRepository chunkRepo,
                                  ControlledDocumentRepository documentRepo,
                                  SystemSettingRepository settingRepo,
                                  OllamaClient ollamaClient,
                                  AiConfig aiConfig,
                                  DocumentTextExtractor textExtractor) {
        this.chunkRepo = chunkRepo;
        this.documentRepo = documentRepo;
        this.settingRepo = settingRepo;
        this.ollamaClient = ollamaClient;
        this.aiConfig = aiConfig;
        this.textExtractor = textExtractor;
    }

    @PostConstruct
    public void startBackgroundIndexing() {
        indexBackgroundKnowledgeAsync();
    }

    @Async("aiTaskExecutor")
    public void indexBackgroundKnowledgeAsync() {
        String pathStr = settingRepo.findByScopeAndScopeIdIsNull("GLOBAL")
                .map(SystemSetting::getSettings)
                .map(SystemSetting.SettingValues::getExternalKnowledgePath)
                .orElse(aiConfig.getBackgroundKnowledgePath());

        if (pathStr == null || pathStr.isBlank()) {
            log.info("[Background AI] No background knowledge path configured. Skipping.");
            return;
        }

        Path root = Paths.get(pathStr);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            log.warn("[Background AI] Background knowledge path does not exist or is not a directory: {}", pathStr);
            return;
        }

        log.info("[Background AI] Starting index scan of external folder: {}", pathStr);

        // Delete previous external knowledge chunks (idempotent re-index)
        chunkRepo.deleteBySource("EXTERNAL");

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                 .filter(this::isSupportedFile)
                 .forEach(filePath -> indexExternalFile(root, filePath));
        } catch (IOException e) {
            log.error("[Background AI] Failed to scan background knowledge folder: {}", e.getMessage());
        }

        log.info("[Background AI] Index scan complete for {}", pathStr);
    }

    private boolean isSupportedFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".docx") || name.endsWith(".pdf") || name.endsWith(".txt") || name.endsWith(".md");
    }

    private void indexExternalFile(Path root, Path filePath) {
        String fileName = filePath.getFileName().toString();
        String relPath = root.relativize(filePath).toString();
        log.info("[Background AI] Processing external file: {}", fileName);

        // 1. Virtualize document if not already registered
        ControlledDocument doc = documentRepo.findByExternalPath(relPath)
                .orElseGet(() -> {
                    ControlledDocument d = new ControlledDocument();
                    d.setTitle(fileName);
                    d.setDocumentNumber("KB-" + fileName);
                    d.setStatus(DocumentStatus.EXTERNAL_KNOWLEDGE);
                    d.setExternalPath(relPath);
                    d.setCreatedAt(Instant.now());
                    d.setUpdatedAt(Instant.now());
                    return documentRepo.save(d);
                });

        // 2. Extract and index chunks
        try {
            String fullText = textExtractor.extract(filePath);
            if (fullText == null || fullText.isBlank()) {
                return;
            }

            List<String> chunks = chunkText(fullText, 
                    aiConfig.getIndexChunkWords(), aiConfig.getIndexChunkOverlap());

            List<DocumentChunk> newChunks = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                String chunkText = chunks.get(i);
                List<Double> embedding = ollamaClient.embedText(chunkText);
                if (embedding.isEmpty()) continue;

                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocumentId(doc.getId());
                chunk.setDocumentTitle(doc.getTitle());
                chunk.setDocumentNumber(doc.getDocumentNumber());
                chunk.setSource("EXTERNAL");
                chunk.setChunkIndex(i);
                chunk.setText(chunkText);
                chunk.setEmbedding(embedding);
                chunk.setIndexedAt(Instant.now());
                newChunks.add(chunk);
            }

            chunkRepo.saveAll(newChunks);
            log.debug("[Background AI] Indexed {} chunks for {}", newChunks.size(), fileName);

        } catch (Exception e) {
            log.error("[Background AI] Error indexing {}: {}", filePath, e.getMessage());
        }
    }

    private List<String> chunkText(String text, int windowSize, int overlap) {
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();
        int step = Math.max(1, windowSize - overlap);
        for (int start = 0; start < words.length; start += step) {
            int end = Math.min(start + windowSize, words.length);
            String chunk = String.join(" ", Arrays.copyOfRange(words, start, end));
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (end == words.length) break;
        }
        return chunks;
    }
}
