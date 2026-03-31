package com.pharmaCx.dms.ai.service;

import com.pharmaCx.dms.ai.model.DocumentChunk;
import com.pharmaCx.dms.ai.repository.DocumentChunkRepository;
import com.pharmaCx.dms.config.AiConfig;
import com.pharmaCx.dms.domain.enums.DocumentStatus;
import com.pharmaCx.dms.domain.enums.UserRole;
import com.pharmaCx.dms.domain.model.AppUser;
import com.pharmaCx.dms.domain.model.ControlledDocument;
import com.pharmaCx.dms.domain.repository.ControlledDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RBAC-filtered semantic document search.
 *
 * RBAC rule (mirrors DocumentWorkflowService):
 *   SYSTEM_ADMIN  → sees all documents across all statuses
 *   Everyone else → PUBLISHED docs (all) + their own in-progress docs
 *                   (requestedBy, authorId, workflowStep assignee)
 *
 * Search uses cosine similarity between the query embedding and stored
 * chunk embeddings. Results are grouped by document and the best-matching
 * chunk text becomes the snippet.
 */
@Service
public class DocumentSearchService {

    private static final Logger log = LoggerFactory.getLogger(DocumentSearchService.class);

    private final DocumentChunkRepository chunkRepo;
    private final ControlledDocumentRepository documentRepo;
    private final com.pharmaCx.dms.domain.repository.AppUserRepository userRepo;
    private final OllamaClient ollamaClient;
    private final AiConfig aiConfig;

    public DocumentSearchService(DocumentChunkRepository chunkRepo,
                                 ControlledDocumentRepository documentRepo,
                                 com.pharmaCx.dms.domain.repository.AppUserRepository userRepo,
                                 OllamaClient ollamaClient,
                                 AiConfig aiConfig) {
        this.chunkRepo = chunkRepo;
        this.documentRepo = documentRepo;
        this.userRepo = userRepo;
        this.ollamaClient = ollamaClient;
        this.aiConfig = aiConfig;
    }

    /**
     * Semantic search over documents the current user is allowed to see.
     *
     * @param query       natural language query
     * @param currentUser authenticated user (used for RBAC filtering)
     * @return ranked list of search results
     */
    public List<SearchResult> search(String query, AppUser currentUser, Integer limit) {
        int finalLimit = (limit != null && limit > 0) ? limit : aiConfig.getSearchTopK();
        
        // --- 1. AI-Driven Intent Detection (Enterprise Search Pre-operation) ---
        SearchIntent intent = extractIntent(query);
        
        String lowerQuery = query.toLowerCase();
        String detectedEmail = intent.email();
        DocumentStatus filterStatus = intent.status();
        String searchMode = intent.mode() != null ? intent.mode() : "SEMANTIC";

        // --- 2. Embedding & Base Candidates ---
        List<Double> queryVector = ollamaClient.embedText(query);
        if (queryVector.isEmpty()) {
            log.warn("[AI Search] Could not embed query: {}", query);
            return Collections.emptyList();
        }

        // 2. Resolve accessible document IDs for this user
        Set<String> accessibleDocIds = resolveAccessibleDocumentIds(currentUser);

        // 3. Load chunks for accessible documents
        List<DocumentChunk> candidates;
        if (accessibleDocIds == null) {
            // SYSTEM_ADMIN — no filter
            candidates = chunkRepo.findAll();
        } else if (accessibleDocIds.isEmpty()) {
            return Collections.emptyList();
        } else {
            candidates = chunkRepo.findByDocumentIdIn(accessibleDocIds);
        }

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // --- 3. Scoring with Metadata Boosts ---
        String[] keywords = lowerQuery.split("\\W+");
        Set<String> keywordSet = Arrays.stream(keywords)
                .filter(k -> k.length() > 2)
                .collect(Collectors.toSet());

        // Optimization: Resolve author match once
        Set<String> targetDocIds = null;
        if (detectedEmail != null) {
            String targetUserId = userRepo.findByEmail(detectedEmail).map(AppUser::getId).orElse(null);
            if (targetUserId != null) {
                targetDocIds = documentRepo.findByAuthorId(targetUserId).stream()
                        .map(ControlledDocument::getId)
                        .collect(Collectors.toSet());
            }
        }

        final Set<String> finalTargetDocIds = targetDocIds;
        final DocumentStatus finalStatus = filterStatus;

        List<ScoredChunk> scored = candidates.stream()
                .filter(c -> c.getEmbedding() != null && !c.getEmbedding().isEmpty())
                .map(c -> {
                    double vectorScore = cosineSimilarity(queryVector, c.getEmbedding());
                    double boost = 0;
                    
                    // a) Keyword boost (max 0.2)
                    if (!keywordSet.isEmpty()) {
                        String text = c.getText().toLowerCase();
                        long matches = keywordSet.stream().filter(text::contains).count();
                        boost += (double) matches / keywordSet.size() * 0.2;
                    }

                    // b) Metadata: Author Match (High boost 1.0)
                    if (finalTargetDocIds != null && finalTargetDocIds.contains(c.getDocumentId())) {
                        boost += 1.0;
                    }

                    // c) Metadata: Status Match (Moderate boost 0.5)
                    if (finalStatus != null && finalStatus.name().equals(c.getDocumentStatus())) {
                        boost += 0.5;
                    }

                    // d) Metadata: Title Match (Moderate boost 0.3)
                    if (c.getDocumentTitle() != null && lowerQuery.contains(c.getDocumentTitle().toLowerCase())) {
                        boost += 0.3;
                    }

                    return new ScoredChunk(c, vectorScore + boost);
                })
                .filter(s -> s.score() >= 0.70)
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .collect(Collectors.toList());

        // --- 4. Group & Build Results (Aggressive Deduplication) ---
        // Group by sanitized title to merge "SOP-004..." and "SOP-004_...docx".
        // This ensures logically identical documents (internal & external) appear once.
        Map<String, ScoredChunk> bestByDoc = new LinkedHashMap<>();
        for (ScoredChunk sc : scored) {
            String rawTitle = sc.chunk().getDocumentTitle() != null ? sc.chunk().getDocumentTitle() : "";
            // Sanitize: lowercase, remove extensions, remove underscores/dashes, trim
            String sanitizedTitle = rawTitle.toLowerCase()
                    .replaceAll("\\.docx$|\\.pdf$|\\.doc$", "")
                    .replaceAll("[_\\-]", " ")
                    .replaceAll("\\s+", " ")
                    .trim();
            
            // If sanitized title is too short, fallback to documentId
            String dedupeKey = sanitizedTitle.length() > 3 ? sanitizedTitle : sc.chunk().getDocumentId();
            
            bestByDoc.merge(dedupeKey, sc,
                    (a, b) -> a.score() >= b.score() ? a : b);
        }

        List<SearchResult> results = bestByDoc.values().stream()
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(finalLimit)
                .map(sc -> {
                    DocumentChunk c = sc.chunk();
                    String highlightedSnippet = createHighlightedSnippet(c.getText(), query);
                    
                    // Post-processing: Tag external sources
                    boolean isExternal = "EXTERNAL".equalsIgnoreCase(c.getSource());
                    String sourceTag = isExternal ? "[EXTERNAL]" : "[INTERNAL]";

                    return new SearchResult(
                            c.getDocumentId(),
                            c.getDocumentTitle(),
                            c.getDocumentNumber(),
                            c.getDocumentStatus(),
                            c.getDocumentTypeId(),
                            c.getUnitId(),
                            sourceTag + " " + highlightedSnippet,
                            Math.round(sc.score() * 100.0) / 100.0
                    );
                })
                .collect(Collectors.toList());

        // --- 5. Post-processing: Result Ranking & Summarization (Enterprise Grade) ---
        if ("SUMMARY".equalsIgnoreCase(searchMode) && !results.isEmpty()) {
            return summarizeResults(results, query);
        }

        return results;
    }

    private List<SearchResult> summarizeResults(List<SearchResult> results, String query) {
        log.info("[AI Search] Post-processing: Summarizing {} results for query: {}", results.size(), query);
        // In a real implementation, we would use local AI to create a combined summary
        // For now, we'll keep the top result but add a 'summary' flag or prefix
        if (results.isEmpty()) return results;
        
        SearchResult top = results.get(0);
        String summary = "Summary based on " + results.size() + " documents: " + top.snippet();
        
        List<SearchResult> summarized = new ArrayList<>();
        summarized.add(new SearchResult(
                top.documentId(), "Search Summary", "AI-SUMM", "N/A", "AI", "SYSTEM", 
                summary, 1.0));
        summarized.addAll(results);
        return summarized;
    }

    /**
     * Retrieve the top-K chunk texts for a specific document to use as RAG
     * context in the chat panel. Returns text of the most relevant chunks.
     */
    public List<String> getContextChunks(String documentId, String query, int topK) {
        List<Double> queryVector = ollamaClient.embedText(query);
        List<DocumentChunk> chunks = chunkRepo.findByDocumentId(documentId);
        List<DocumentChunk> externalKnowledge = chunkRepo.findBySource("EXTERNAL");
        
        List<DocumentChunk> candidates = new ArrayList<>(chunks);
        candidates.addAll(externalKnowledge);

        if (queryVector.isEmpty() || candidates.isEmpty()) {
            return candidates.stream()
                    .sorted(Comparator.comparingInt(DocumentChunk::getChunkIndex))
                    .limit(topK)
                    .map(DocumentChunk::getText)
                    .collect(Collectors.toList());
        }

        return candidates.stream()
                .filter(c -> c.getEmbedding() != null && !c.getEmbedding().isEmpty())
                .map(c -> new ScoredChunk(c, cosineSimilarity(queryVector, c.getEmbedding())))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .map(sc -> sc.chunk().getText())
                .collect(Collectors.toList());
    }

    /**
     * Retrieve top-K cross-document context chunks for the AI Search chat.
     * Respects RBAC — only returns chunks from accessible documents.
     */
    public List<String> getCrossDocContextChunks(String query, AppUser currentUser, int topK) {
        List<Double> queryVector = ollamaClient.embedText(query);
        if (queryVector.isEmpty()) return Collections.emptyList();

        Set<String> accessibleDocIds = resolveAccessibleDocumentIds(currentUser);
        List<DocumentChunk> external = chunkRepo.findBySource("EXTERNAL");
        List<DocumentChunk> internal = accessibleDocIds == null
                ? chunkRepo.findAll()
                : accessibleDocIds.isEmpty() ? Collections.emptyList()
                : chunkRepo.findByDocumentIdIn(accessibleDocIds);

        List<DocumentChunk> candidates = new ArrayList<>(external);
        candidates.addAll(internal);

        return candidates.stream()
                .filter(c -> c.getEmbedding() != null && !c.getEmbedding().isEmpty())
                .map(c -> new ScoredChunk(c, cosineSimilarity(queryVector, c.getEmbedding())))
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                // Prefix each chunk with its source document for citation
                .map(sc -> {
                    String docRef = sc.chunk().getDocumentNumber() != null ? sc.chunk().getDocumentNumber() : "Global Knowledge";
                    return "[" + docRef + "] " + sc.chunk().getText();
                })
                .collect(Collectors.toList());
    }

    // ── RBAC resolution ────────────────────────────────────────────────────────

    /**
     * Returns the set of documentIds the user may see, or null for SYSTEM_ADMIN (all).
     * Mirrors DocumentWorkflowService.getMyDocuments() + getByStatus(PUBLISHED).
     */
    private Set<String> resolveAccessibleDocumentIds(AppUser user) {
        if (user.getRole() == UserRole.SYSTEM_ADMIN) {
            return null; // no filter
        }

        Set<String> ids = new HashSet<>();

        // All PUBLISHED documents are visible to every authenticated user
        documentRepo.findByStatus(DocumentStatus.PUBLISHED)
                .forEach(d -> ids.add(d.getId()));

        // Their own in-progress documents (requestedBy / authorId / workflow assignee)
        documentRepo.findActiveDocumentsByUser(user.getId())
                .forEach(d -> ids.add(d.getId()));

        return ids;
    }

    // ── Math ───────────────────────────────────────────────────────────────────

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size() || a.isEmpty()) return 0.0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.size(); i++) {
            dot   += a.get(i) * b.get(i);
            normA += a.get(i) * a.get(i);
            normB += b.get(i) * b.get(i);
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0.0 : dot / denom;
    }

    private String snippet(String text) {
        if (text == null) return "";
        String clean = text.replaceAll("\\s+", " ").trim();
        return clean.length() > 300 ? clean.substring(0, 297) + "..." : clean;
    }

    /**
     * Creates a snippet that highlights query terms.
     * Finds a window of ~300 chars that contains the most keywords.
     */
    private String createHighlightedSnippet(String text, String query) {
        if (text == null || query == null || query.isBlank()) return snippet(text);

        String[] keywords = query.toLowerCase().split("\\W+");
        Set<String> keywordSet = Arrays.stream(keywords)
                .filter(k -> k.length() > 2)
                .collect(Collectors.toSet());

        if (keywordSet.isEmpty()) return snippet(text);

        String lowerText = text.toLowerCase();
        int bestStart = 0;
        int maxMatches = -1;

        // Simple sliding window (step 50 chars)
        for (int i = 0; i < Math.max(1, text.length() - 300); i += 50) {
            String window = lowerText.substring(i, Math.min(i + 300, text.length()));
            int matches = 0;
            for (String kw : keywordSet) {
                if (window.contains(kw)) matches++;
            }
            if (matches > maxMatches) {
                maxMatches = matches;
                bestStart = i;
            }
        }

        String rawSnippet = text.substring(bestStart, Math.min(bestStart + 300, text.length()));
        if (bestStart > 0) rawSnippet = "..." + rawSnippet;
        if (bestStart + 300 < text.length()) rawSnippet = rawSnippet + "...";

        // Highlight keywords by wrapping in <mark> (frontend will handle this)
        String highlighted = rawSnippet;
        for (String kw : keywordSet) {
            // Case-insensitive replacement with <mark>
            highlighted = highlighted.replaceAll("(?i)(" + java.util.regex.Pattern.quote(kw) + ")", "<mark>$1</mark>");
        }

        return highlighted;
    }

    // ── Intent Extraction ─────────────────────────────────────────────────────

    private SearchIntent extractIntent(String query) {
        String lowerQuery = query.toLowerCase();
        
        // --- FAST PATH: Regex-based extraction ---
        String fastEmail = null;
        DocumentStatus fastStatus = null;
        String fastMode = "SEARCH";
        
        // 1. Detect Email (regex for [user]@domain.com)
        java.util.regex.Matcher emailMatcher = java.util.regex.Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b").matcher(query);
        if (emailMatcher.find()) fastEmail = emailMatcher.group();
        
        // 2. Detect Status
        if (lowerQuery.contains("completed") || lowerQuery.contains("published")) {
            fastStatus = DocumentStatus.PUBLISHED;
        } else if (lowerQuery.contains("draft") || lowerQuery.contains("author")) {
            fastStatus = DocumentStatus.AUTHOR_DRAFT;
        }
        
        // 3. Detect Summary Intent
        if (lowerQuery.contains("summarize") || lowerQuery.contains("summary") || lowerQuery.contains("short version")) {
            fastMode = "SUMMARY";
        }

        // If we found both status/email or this is a very short query, skip AI
        if ((fastEmail != null && fastStatus != null) || query.length() < 20) {
            log.info("[AI Search] Using fast-path regex intent extraction");
            return new SearchIntent(fastEmail, fastStatus, fastMode);
        }

        // --- SLOW PATH: AI-driven extraction (for complex phrasing) ---
        String prompt = "You are a PharmaCX Search Assistant. Extract metadata filters and intent from this user query.\n" +
                "Query: \"" + query + "\"\n" +
                "JSON format ONLY: { \"email\": string|null, \"status\": \"PUBLISHED\"|\"AUTHOR_DRAFT\"|null, \"mode\": \"SUMMARY\"|\"SEARCH\"|null }\n" +
                "Do not include preamble or explanation.";

        try {
            // Use light model for fast intent extraction
            String response = ollamaClient.generateChat(null, Collections.emptyList(), prompt, true);
            
            // Basic JSON extraction (looking for { ... })
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}");
            if (start != -1 && end != -1) {
                String json = response.substring(start, end + 1);
                // Use Jackson from OllamaClient if we really need full parsing, 
                // but for speed and robustness with sparse models, simple scan is often better.
                String email = null;
                DocumentStatus status = null;
                String mode = "SEARCH";

                if (json.contains("\"email\"") && json.contains("@")) {
                   java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"email\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
                   if (m.find()) email = m.group(1);
                }
                if (json.contains("\"status\"")) {
                    if (json.contains("PUBLISHED")) status = DocumentStatus.PUBLISHED;
                    else if (json.contains("AUTHOR_DRAFT")) status = DocumentStatus.AUTHOR_DRAFT;
                }
                if (json.contains("\"mode\"")) {
                    if (json.contains("SUMMARY")) mode = "SUMMARY";
                }
                return new SearchIntent(email, status, mode);
            }
        } catch (Exception e) {
            log.warn("AI Intent extraction failed: {}", e.getMessage());
        }
        return new SearchIntent(null, null, "SEARCH");
    }

    // ── DTOs ───────────────────────────────────────────────────────────────────

    private record SearchIntent(String email, DocumentStatus status, String mode) {}

    private record ScoredChunk(DocumentChunk chunk, double score) {}

    public record SearchResult(
            String documentId,
            String documentTitle,
            String documentNumber,
            String documentStatus,
            String documentTypeId,
            String unitId,
            String snippet,
            double score
    ) {}
}
