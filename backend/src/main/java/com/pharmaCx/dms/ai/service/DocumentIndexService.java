package com.pharmaCx.dms.ai.service;

import com.pharmaCx.dms.ai.model.DocumentChunk;
import com.pharmaCx.dms.ai.repository.DocumentChunkRepository;
import com.pharmaCx.dms.config.AiConfig;
import com.pharmaCx.dms.domain.model.ControlledDocument;
import com.pharmaCx.dms.domain.repository.ControlledDocumentRepository;
import com.pharmaCx.dms.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Indexes PUBLISHED documents into the document_chunks collection for RAG.
 *
 * Design principles:
 * - All methods are @Async("aiTaskExecutor") — never blocks HTTP threads
 * - Source files are opened READ-ONLY by DocumentTextExtractor — no writes
 * - Old chunks for a document are deleted before re-indexing (idempotent)
 * - Only triggered on PUBLISHED state — never during active editing
 */
@Service
public class DocumentIndexService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIndexService.class);

    private final DocumentChunkRepository chunkRepo;
    private final ControlledDocumentRepository documentRepo;
    private final FileStorageService fileStorageService;
    private final DocumentTextExtractor textExtractor;
    private final OllamaClient ollamaClient;
    private final AiConfig aiConfig;

    public DocumentIndexService(DocumentChunkRepository chunkRepo,
                                ControlledDocumentRepository documentRepo,
                                FileStorageService fileStorageService,
                                DocumentTextExtractor textExtractor,
                                OllamaClient ollamaClient,
                                AiConfig aiConfig) {
        this.chunkRepo = chunkRepo;
        this.documentRepo = documentRepo;
        this.fileStorageService = fileStorageService;
        this.textExtractor = textExtractor;
        this.ollamaClient = ollamaClient;
        this.aiConfig = aiConfig;
    }

    /**
     * On startup, index any PUBLISHED documents that have no chunks yet.
     * Runs async so it doesn't slow down application startup.
     */
    @PostConstruct
    public void indexOnStartup() {
        indexAllPublishedAsync();
    }

    @Async("aiTaskExecutor")
    public void indexAllPublishedAsync() {
        log.info("[AI Index] Startup scan for un-indexed PUBLISHED documents...");
        List<ControlledDocument> published = documentRepo.findByStatus(
                com.pharmaCx.dms.domain.enums.DocumentStatus.PUBLISHED);
        int indexed = 0;
        for (ControlledDocument doc : published) {
            if (doc.getDocumentFileId() != null && chunkRepo.countByDocumentId(doc.getId()) == 0) {
                indexSingleDocument(doc);
                indexed++;
            }
        }
        log.info("[AI Index] Startup scan complete: {} documents indexed.", indexed);
    }

    /**
     * Index a single document. Called when it transitions to PUBLISHED.
     * Safe to call multiple times — deletes old chunks before re-indexing.
     */
    @Async("aiTaskExecutor")
    public void indexDocumentAsync(String documentId) {
        documentRepo.findById(documentId).ifPresent(this::indexSingleDocument);
    }

    /**
     * Return basic index statistics.
     */
    public IndexStatus getIndexStatus() {
        long totalPublished = documentRepo.countByStatus(
                com.pharmaCx.dms.domain.enums.DocumentStatus.PUBLISHED);
        long totalChunks = chunkRepo.count();
        // count distinct indexed doc IDs
        List<ControlledDocument> published = documentRepo.findByStatus(
                com.pharmaCx.dms.domain.enums.DocumentStatus.PUBLISHED);
        long indexedDocs = published.stream()
                .filter(d -> d.getDocumentFileId() != null
                          && chunkRepo.countByDocumentId(d.getId()) > 0)
                .count();
        return new IndexStatus(totalPublished, indexedDocs, totalChunks);
    }

    // ── Core indexing logic ────────────────────────────────────────────────────

    private void indexSingleDocument(ControlledDocument doc) {
        if (doc.getDocumentFileId() == null) {
            log.debug("[AI Index] Skipping {} — no file attached.", doc.getId());
            return;
        }

        Path filePath = fileStorageService.getFilePath(doc.getDocumentFileId());
        if (filePath == null) {
            log.warn("[AI Index] File not found on disk for document {}", doc.getId());
            return;
        }

        log.info("[AI Index] Indexing document: {} ({})", doc.getDocumentNumber(), doc.getId());

        // Extract text (read-only, file never modified)
        String fullText = textExtractor.extract(filePath);
        if (fullText.isBlank()) {
            log.warn("[AI Index] No text extracted from document {}", doc.getId());
            return;
        }

        // Chunk the text
        List<String> chunks = chunkText(fullText,
                aiConfig.getIndexChunkWords(), aiConfig.getIndexChunkOverlap());

        // Delete old chunks (idempotent re-index)
        chunkRepo.deleteByDocumentId(doc.getId());

        // Embed each chunk and persist
        List<DocumentChunk> newChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            
            // Robust check: Attempt embedding with retries on startup
            List<Double> embedding = getEmbeddingWithRetry(chunkText, 3);
            
            if (embedding.isEmpty()) {
                log.warn("[AI Index] Empty embedding for chunk {} of doc {} after retries — skipping", i, doc.getId());
                continue;
            }

            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocumentId(doc.getId());
            chunk.setDocumentTitle(doc.getTitle());
            chunk.setDocumentNumber(doc.getDocumentNumber());
            chunk.setDocumentStatus(doc.getStatus().name());
            chunk.setDocumentTypeId(doc.getDocumentTypeId());
            chunk.setUnitId(doc.getUnitId());
            chunk.setSource("INTERNAL");
            chunk.setChunkIndex(i);
            chunk.setText(chunkText);
            chunk.setEmbedding(embedding);
            chunk.setIndexedAt(Instant.now());
            newChunks.add(chunk);
        }

        chunkRepo.saveAll(newChunks);
        log.info("[AI Index] Indexed {} chunks for document {} ({})",
                newChunks.size(), doc.getDocumentNumber(), doc.getId());

        // Helix AI Export (Physical hot-folder sync)
        if (aiConfig.isCopyOnPublish()) {
            try {
                String safeTitle = doc.getTitle().replaceAll("[^a-zA-Z0-9.-]", "_");
                String fileName = doc.getDocumentNumber() + "_" + safeTitle + "." + fileStorageService.getFileExtension(doc.getDocumentFileId());
                fileStorageService.copyFileToExternal(doc.getDocumentFileId(), aiConfig.getPublishedDocsPath(), fileName);
                log.info("[AI Export] Successfully synchronized to Helix hot-folder: {}", fileName);
            } catch (Exception e) {
                log.error("[AI Export] Failed to sync to hot-folder", e);
            }
        }
    }

    /**
     * Splits text into overlapping word windows.
     *
     * @param text        full document text
     * @param windowSize  number of words per chunk
     * @param overlap     number of words to repeat at the start of the next chunk
     */
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
    private List<Double> getEmbeddingWithRetry(String text, int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            List<Double> embedding = ollamaClient.embedText(text);
            if (!embedding.isEmpty()) return embedding;
            
            if (attempt < maxRetries - 1) {
               log.info("[AI Index] Embedding failed, retrying in 5s (attempt {}/{})", attempt + 1, maxRetries);
               try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            }
        }
        return java.util.Collections.emptyList();
    }

    // ── Status DTO ─────────────────────────────────────────────────────────────

    public record IndexStatus(long totalPublished, long indexedDocs, long totalChunks) {}
}
