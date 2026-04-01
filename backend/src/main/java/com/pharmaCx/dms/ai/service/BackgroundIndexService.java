package com.pharmaCx.dms.ai.service;

import com.pharmaCx.dms.ai.model.DocumentChunk;
import com.pharmaCx.dms.ai.repository.DocumentChunkRepository;
import com.pharmaCx.dms.config.AiConfig;
import com.pharmaCx.dms.domain.enums.DocumentStatus;
import com.pharmaCx.dms.domain.model.ControlledDocument;
import com.pharmaCx.dms.domain.repository.ControlledDocumentRepository;
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
 * Scans external folders based on AiConfig (Docker env vars), 
 * registers files as VIRTUAL documents in the DMS,
 * and indexes them for RAG and unified search.
 */
@Service
public class BackgroundIndexService {

    private static final Logger log = LoggerFactory.getLogger(BackgroundIndexService.class);

    private final DocumentChunkRepository chunkRepo;
    private final ControlledDocumentRepository documentRepo;
    private final OllamaClient ollamaClient;
    private final AiConfig aiConfig;
    private final DocumentTextExtractor textExtractor;

    public BackgroundIndexService(DocumentChunkRepository chunkRepo,
                                  ControlledDocumentRepository documentRepo,
                                  OllamaClient ollamaClient,
                                  AiConfig aiConfig,
                                  DocumentTextExtractor textExtractor) {
        this.chunkRepo = chunkRepo;
        this.documentRepo = documentRepo;
        this.ollamaClient = ollamaClient;
        this.aiConfig = aiConfig;
        this.textExtractor = textExtractor;
    }

    @Async("aiTaskExecutor")
    public void indexBackgroundKnowledgeAsync() {
        String knowledgePath = aiConfig.getBackgroundKnowledgePath();
        String publishedPath = aiConfig.getPublishedDocsPath();

        log.info("[Background AI] Starting index scan (Source 1: {}, Source 2: {})", knowledgePath, publishedPath);

        scanAndIndexFolder(knowledgePath, "KNOWLEDGE");
        scanAndIndexFolder(publishedPath, "PUBLISHED_SYNC");

        log.info("[Background AI] Unified index scan complete.");
    }

    private void scanAndIndexFolder(String pathStr, String originLabel) {
        if (pathStr == null || pathStr.isBlank()) return;
        Path root = Paths.get(pathStr);
        if (!Files.exists(root) || !Files.isDirectory(root)) {
            log.warn("[Background AI] Directory not found for {}: {}", originLabel, pathStr);
            return;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                 .filter(this::isSupportedFile)
                 .forEach(filePath -> indexExternalFile(root, filePath, originLabel));
        } catch (IOException e) {
            log.error("[Background AI] Failed to scan {}: {}", originLabel, e.getMessage());
        }
    }

    private boolean isSupportedFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".docx") || name.endsWith(".pdf") || name.endsWith(".txt") || name.endsWith(".md");
    }

    private void indexExternalFile(Path root, Path filePath, String originLabel) {
        String fileName = filePath.getFileName().toString();
        String relPath = originLabel + "/" + root.relativize(filePath).toString();
        
        try {
            long currentSize = Files.size(filePath);
            long currentMod = Files.getLastModifiedTime(filePath).toMillis();

            ControlledDocument doc = documentRepo.findByExternalPath(relPath).orElse(null);
            
            if (doc != null) {
                boolean sizeChanged = doc.getExternalFileSize() == null || doc.getExternalFileSize() != currentSize;
                boolean modChanged = doc.getExternalLastModified() == null || doc.getExternalLastModified() != currentMod;
                boolean chunksMissing = chunkRepo.countByDocumentId(doc.getId()) == 0;

                if (!sizeChanged && !modChanged && !chunksMissing) {
                    log.debug("[Background AI] Skipping {} (unchanged)", fileName);
                    return;
                }
                
                log.info("[Background AI] Re-indexing {} (changed or missing chunks)", fileName);
                doc.setUpdatedAt(Instant.now());
                doc.setExternalFileSize(currentSize);
                doc.setExternalLastModified(currentMod);
                documentRepo.save(doc);
            } else {
                log.info("[Background AI] New file detected: {}", fileName);
                doc = new ControlledDocument();
                doc.setTitle(fileName + " (" + originLabel + ")");
                doc.setDocumentNumber("KB-" + originLabel + "-" + (int)(Math.random() * 9000 + 1000));
                doc.setStatus(DocumentStatus.EXTERNAL_KNOWLEDGE);
                doc.setExternalPath(relPath);
                doc.setExternalFileSize(currentSize);
                doc.setExternalLastModified(currentMod);
                doc.setCreatedAt(Instant.now());
                doc.setUpdatedAt(Instant.now());
                doc = documentRepo.save(doc);
            }

            indexDocChunks(doc, filePath);

        } catch (Exception e) {
            log.error("[Background AI] Error processing {}: {}", filePath, e.getMessage());
        }
    }

    private void indexDocChunks(ControlledDocument doc, Path filePath) {
        try {
            String fullText = textExtractor.extract(filePath);
            if (fullText == null || fullText.isBlank()) {
                log.warn("[Background AI] No text in {}", doc.getTitle());
                return;
            }

            List<String> chunks = chunkText(fullText, 
                    aiConfig.getIndexChunkWords(), aiConfig.getIndexChunkOverlap());

            chunkRepo.deleteByDocumentId(doc.getId());

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

            if (!newChunks.isEmpty()) {
                chunkRepo.saveAll(newChunks);
                log.info("[Background AI] Indexed {} chunks for {}", newChunks.size(), doc.getTitle());
            }
        } catch (Exception e) {
            log.error("[Background AI] Chunking failed for {}: {}", doc.getTitle(), e.getMessage());
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
