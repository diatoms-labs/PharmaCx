package com.pharmaCx.dms.ai.controller;

import com.pharmaCx.dms.ai.service.AiOrchestratorService;
import com.pharmaCx.dms.ai.service.BackgroundIndexService;
import com.pharmaCx.dms.ai.service.DocumentContentService;
import com.pharmaCx.dms.ai.service.DocumentIndexService;
import com.pharmaCx.dms.ai.service.DocumentSearchService;
import com.pharmaCx.dms.domain.enums.AuditAction;
import com.pharmaCx.dms.domain.enums.ResourceType;
import com.pharmaCx.dms.domain.model.AppUser;
import com.pharmaCx.dms.security.CurrentUserService;
import com.pharmaCx.dms.service.AuditService;
import com.pharmaCx.dms.service.AuthService;
import com.pharmaCx.dms.config.AiConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI REST endpoints — all require JWT authentication.
 *
 * POST /api/v1/ai/search           → Unified semantic search (DMS + Background Knowledge)
 * POST /api/v1/ai/chat             → RAG chat with unified context
 * GET  /api/v1/ai/index/status     → Index statistics
 * POST /api/v1/ai/index/all        → Trigger full re-index of all sources (DMS + Folders)
 * POST /api/v1/ai/index/{docId}    → Manual re-index of a single document
 * GET  /api/v1/ai/health           → AI connectivity and health check
 *
 * Every chat call writes an AuditEvent via the existing AuditService.
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AIController {

    private final DocumentSearchService searchService;
    private final DocumentIndexService indexService;
    private final BackgroundIndexService backgroundIndexService;
    private final AiOrchestratorService aiOrchestrator;
    private final DocumentContentService contentService;
    private final CurrentUserService currentUserService;
    private final AuthService authService;
    private final AuditService auditService;
    private final AiConfig config;

    public AIController(DocumentSearchService searchService,
                        DocumentIndexService indexService,
                        BackgroundIndexService backgroundIndexService,
                        AiOrchestratorService aiOrchestrator,
                        DocumentContentService contentService,
                        CurrentUserService currentUserService,
                        AuthService authService,
                        AuditService auditService,
                        AiConfig config) {
        this.searchService = searchService;
        this.indexService = indexService;
        this.backgroundIndexService = backgroundIndexService;
        this.aiOrchestrator = aiOrchestrator;
        this.contentService = contentService;
        this.currentUserService = currentUserService;
        this.authService = authService;
        this.auditService = auditService;
        this.config = config;
    }

    // ── Search ─────────────────────────────────────────────────────────────────

    /**
     * Semantic document search.
     * Body: { "query": "...", "limit": 10 }
     * Returns documents ranked by relevance, filtered to what the user can see.
     */
    @PostMapping(value = "/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DocumentSearchService.SearchResult>> search(
            @RequestBody SearchRequest request) {
        AppUser user = currentUser();
        List<DocumentSearchService.SearchResult> results = searchService.search(request.query(), user, request.limit());
        
        // Audit log search
        String detail = "Semantic Search | query=" + request.query() + " | results=" + results.size();
        auditService.log(AuditAction.AI_QUERY_EXECUTED, ResourceType.DOCUMENT,
                "AI_SEARCH", "Semantic Search", detail);
                
        return ResponseEntity.ok(results);
    }

    @GetMapping(value = "/search")
    public ResponseEntity<List<DocumentSearchService.SearchResult>> searchGet(
            @RequestParam String q,
            @RequestParam(required = false) Integer limit) {
        AppUser user = currentUser();
        List<DocumentSearchService.SearchResult> results = searchService.search(q, user, limit);
        
        // Audit log search
        String detail = "Semantic Search | query=" + q + " | results=" + results.size();
        auditService.log(AuditAction.AI_QUERY_EXECUTED, ResourceType.DOCUMENT,
                "AI_SEARCH", "Semantic Search", detail);
                
        return ResponseEntity.ok(results);
    }

    // ── Chat ───────────────────────────────────────────────────────────────────

    /**
     * RAG chat — two modes based on whether documentId is provided:
     *
     * Single-doc:   documentId present → retrieve chunks from that document only
     * Cross-doc:    documentId absent  → retrieve from all accessible documents
     *
     * Body: { "message": "...", "documentId": "...", "workflowStage": "AUTHOR_DRAFT" }
     */
    @PostMapping(value = "/chat",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@RequestBody ChatRequest request) {
        return createChatStream(request);
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatGet(
            @RequestParam String message,
            @RequestParam(required = false) String documentId,
            @RequestParam(required = false) String workflowStage,
            @RequestParam(defaultValue = "false") boolean isLight) {
        return createChatStream(new ChatRequest(message, documentId, workflowStage, isLight));
    }

    private SseEmitter createChatStream(ChatRequest request) {
        AppUser user = currentUser();

        List<String> contextChunks;
        if (request.documentId() != null && !request.documentId().isBlank()) {
            contextChunks = searchService.getContextChunks(request.documentId(), request.message(), 5);
        } else {
            contextChunks = searchService.getCrossDocContextChunks(request.message(), user, 5);
        }

        // CFR 21 Part 11: log every AI interaction to audit trail
        String modelName = request.isLight() ? "phi3:mini" : "helix-ai";
        String auditDetail = "AI chat | model=" + modelName + " | stage=" + request.workflowStage()
                + " | docId=" + request.documentId()
                + " | query_len=" + request.message().length();
        if (request.documentId() != null && !request.documentId().isBlank()) {
            auditService.log(AuditAction.AI_QUERY_EXECUTED, ResourceType.DOCUMENT,
                    request.documentId(), "AI Chat", auditDetail);
        } else {
            auditService.logSystem(AuditAction.AI_QUERY_EXECUTED, ResourceType.DOCUMENT,
                    "AI_SEARCH", auditDetail);
        }

        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L); // 30-minute timeout for enterprise usage

        emitter.onCompletion(() -> {});
        emitter.onTimeout(emitter::complete);
        emitter.onError(emitter::completeWithError);
        
        // Critical for Nginx SSE streaming
        // Prevents Nginx from buffering the response
        try {
            // Send connection established and also hints for proxy servers
            emitter.send(SseEmitter.event()
                    .name("connection")
                    .data("established")
                    .reconnectTime(5000));
        } catch (Exception ignored) {}

        CompletableFuture.runAsync(() ->
            aiOrchestrator.generateChatStream(
                request.workflowStage(), contextChunks, request.message(), request.isLight(), user,
                token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        try {
                            emitter.completeWithError(e);
                        } catch (Exception ignored) {}
                    }
                },
                () -> {
                    try {
                        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                        emitter.complete();
                    } catch (Exception e) {
                        try {
                            emitter.completeWithError(e);
                        } catch (Exception ignored) {}
                    }
                }
            )
        ).exceptionally(ex -> {
            try {
                emitter.completeWithError(ex);
            } catch (Exception ignored) {}
            return null;
        });

        return emitter;
    }

    // ── Insert content into document ───────────────────────────────────────────

    /**
     * Injects AI-generated text into the DOCX file associated with a document.
     * Only allowed while the document is in AUTHOR_DRAFT status (enforced on frontend;
     * backend validates the document exists and has a file).
     *
     * Body: { "content": "...", "sectionLabel": "3. Procedure" }
     * The sectionLabel is optional — if omitted or blank, content is appended to the body.
     */
    @PostMapping(value = "/insert/{documentId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> insertContent(
            @PathVariable String documentId,
            @RequestBody InsertContentRequest request) {
        try {
            contentService.insertContent(documentId, request.content(), request.sectionLabel());

            // Audit log the insertion
            String detail = "AI content inserted | docId=" + documentId
                    + " | section=" + request.sectionLabel()
                    + " | content_len=" + request.content().length();
            auditService.log(AuditAction.AI_QUERY_EXECUTED, ResourceType.DOCUMENT,
                    documentId, "AI Insert", detail);

            return ResponseEntity.ok(Map.of("status", "inserted", "documentId", documentId));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to insert content: " + e.getMessage()));
        }
    }

    // ── Index management (SYSTEM_ADMIN only) ───────────────────────────────────

    @GetMapping("/index/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<DocumentIndexService.IndexStatus> getIndexStatus() {
        return ResponseEntity.ok(indexService.getIndexStatus());
    }

    @PostMapping("/index/all")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> reIndexAll() {
        // Trigger both internal and external (background) re-indexing
        indexService.indexAllPublishedAsync();
        backgroundIndexService.indexBackgroundKnowledgeAsync();
        
        auditService.logSystem(AuditAction.REINDEX_TRIGGERED, ResourceType.DOCUMENT,
                "AI_SYSTEM", "Full AI re-index triggered (DMS + knowledge-base + published-docs)");
                
        return ResponseEntity.accepted()
                .body(Map.of("status", "full_reindex_started"));
    }

    @PostMapping("/index/{documentId}")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> reIndexDocument(@PathVariable String documentId) {
        indexService.indexDocumentAsync(documentId);
        return ResponseEntity.accepted()
                .body(Map.of("status", "indexing_started", "documentId", documentId));
    }

    @GetMapping("/config")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> cfgDto = new java.util.LinkedHashMap<>();
        cfgDto.put("ollamaUrl", config.getOllamaUrl());
        cfgDto.put("embedModel", config.getEmbedModel());
        cfgDto.put("chatModel", config.getChatModel());
        cfgDto.put("lightChatModel", config.getLightChatModel());
        cfgDto.put("strategy", config.getStrategy());
        cfgDto.put("cloudProvider", config.getCloudProvider());
        cfgDto.put("cloudModel", config.getCloudModel());
        cfgDto.put("indexChunkWords", config.getIndexChunkWords());
        cfgDto.put("indexChunkOverlap", config.getIndexChunkOverlap());
        cfgDto.put("searchTopK", config.getSearchTopK());
        cfgDto.put("backgroundKnowledgePath", config.getBackgroundKnowledgePath() != null ? config.getBackgroundKnowledgePath() : "");
        cfgDto.put("publishedDocsPath", config.getPublishedDocsPath() != null ? config.getPublishedDocsPath() : "");
        cfgDto.put("hostKnowledgePath", config.getHostKnowledgePath() != null ? config.getHostKnowledgePath() : "");
        cfgDto.put("hostPublishedDocsPath", config.getHostPublishedDocsPath() != null ? config.getHostPublishedDocsPath() : "");
        cfgDto.put("copyOnPublish", config.isCopyOnPublish());
        
        return ResponseEntity.ok(cfgDto);
    }

    // ── Health ─────────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean aiUp = aiOrchestrator.isAvailable();
        DocumentIndexService.IndexStatus status = indexService.getIndexStatus();
        return ResponseEntity.ok(Map.of(
                "ollamaAvailable", aiUp,
                "indexedDocuments", status.indexedDocs(),
                "totalChunks", status.totalChunks()
        ));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private AppUser currentUser() {
        String userId = currentUserService.getCurrentUserId();
        return authService.getUserById(userId);
    }

    // ── Request / Response records ─────────────────────────────────────────────

    public record SearchRequest(String query, Integer limit) {}

    public record ChatRequest(String message, String documentId, String workflowStage, boolean isLight) {}

    public record InsertContentRequest(String content, String sectionLabel) {}
}
