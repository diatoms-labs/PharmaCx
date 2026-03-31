package com.pharmaCx.dms.ai.service;

import com.pharmaCx.dms.domain.model.SystemSetting;
import com.pharmaCx.dms.domain.repository.SystemSettingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * Thin HTTP client for the local Ollama REST API.
 *
 * Two operations:
 *   - embedText()    → POST /api/embeddings  (nomic-embed-text)
 *   - generateChat() → POST /api/generate    (pharma-ai, non-streaming)
 *
 * All document text stays local — nothing is sent to external services.
 */
@Service
public class OllamaClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final SystemSettingRepository settingsRepo;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaClient(SystemSettingRepository settingsRepo, 
                        RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.settingsRepo = settingsRepo;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Embed a text string using nomic-embed-text.
     * Returns a 768-dimension vector, or empty list on failure.
     */
    public List<Double> embedText(String text) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", getEmbedModel());

            body.put("prompt", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    getOllamaUrl() + "/api/embeddings", request, String.class);


            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode embeddingNode = root.get("embedding");
                if (embeddingNode != null && embeddingNode.isArray()) {
                    List<Double> embedding = new ArrayList<>();
                    embeddingNode.forEach(n -> embedding.add(n.asDouble()));
                    return embedding;
                }
            }
        } catch (Exception e) {
            log.warn("Embedding failed for text (len={}): {}", text.length(), e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Generate a chat response from pharma-ai with full RAG context injected.
     *
     * @param workflowStage  e.g. "AUTHOR_DRAFT", "PEER_REVIEW", "PUBLISHED" — used for stage tag
     * @param contextChunks  retrieved document chunks to inject between <context> tags
     * @param userMessage    the user's question or request
     * @return the model's response text, or an error message on failure
     */
    @Override
    public String generate(String prompt, String context) {
        return generateChat(null, context != null ? List.of(context) : Collections.emptyList(), prompt, false);
    }

    public String generateChat(String workflowStage, List<String> contextChunks, String userMessage) {
        return generateChat(workflowStage, contextChunks, userMessage, false);
    }

    public String generateChat(String workflowStage, List<String> contextChunks, String userMessage, boolean isLight) {
        try {
            String stageTag = resolveStageTag(workflowStage);
            String contextBlock = buildContextBlock(contextChunks);
            String instruction = "IMPORTANT: Use natural language with proper spaces, punctuation, AND clear paragraph breaks. NEVER merge words together.";
            String fullPrompt = stageTag + "\n" + instruction + "\n\n" + contextBlock + "\n\nUser: " + userMessage;

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", isLight ? getLightChatModel() : getChatModel());

            body.put("prompt", fullPrompt);
            body.put("stream", false);

            ObjectNode options = objectMapper.createObjectNode();
            options.put("temperature", 0.1);
            options.put("num_predict", 128);
            body.set("options", options);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    getOllamaUrl() + "/api/generate", request, String.class);


            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode responseNode = root.get("response");
                if (responseNode != null) {
                    return responseNode.asText();
                }
            }
            return "AI service returned an unexpected response (" + response.getStatusCode() + ").";
        } catch (Exception e) {
            log.error("[AI Chat] Generation failed for model {}: {}", isLight ? getLightChatModel() : getChatModel(), e.getMessage());

            return "AI service is currently unavailable. Error: " + e.getMessage();
        }
    }

    /**
     * Streaming variant of generateChat — calls Ollama with stream=true and
     * invokes onToken for each response token. Calls onComplete when finished.
     */
    @Override
    public void generateChatStream(String workflowStage, List<String> contextChunks,
                                   String userMessage, boolean isLight, Consumer<String> onToken, Runnable onComplete) {
        try {
            String stageTag = resolveStageTag(workflowStage);
            String contextBlock = buildContextBlock(contextChunks);
            String instruction = "IMPORTANT: Use natural language with proper spaces, punctuation, AND clear paragraph breaks. NEVER merge words together.";
            String fullPrompt = stageTag + "\n" + instruction + "\n\n" + contextBlock + "\n\nUser: " + userMessage;

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", isLight ? getLightChatModel() : getChatModel());

            body.put("prompt", fullPrompt);
            body.put("stream", true);

            ObjectNode options = objectMapper.createObjectNode();
            options.put("temperature", 0.1);
            options.put("num_predict", 128);
            body.set("options", options);

            byte[] requestBytes = objectMapper.writeValueAsBytes(body);
            URL url = URI.create(getOllamaUrl() + "/api/generate").toURL();

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBytes);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    JsonNode node = objectMapper.readTree(line);
                    String token = node.path("response").asText("");
                    if (!token.isEmpty()) {
                        log.info("[Ollama] Token: '{}'", token);
                        onToken.accept(token);
                    }
                    if (node.path("done").asBoolean(false)) break;
                }
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            log.error("Streaming chat generation failed: {}", e.getMessage());
            onToken.accept("AI service is currently unavailable. Please check that Ollama is running.");
        } finally {
            onComplete.run();
        }
    }

    /**
     * Ping Ollama to check connectivity.
     */
    public boolean isAvailable() {
        try {
            // Check specific endpoint for tags
            ResponseEntity<String> response = restTemplate.getForEntity(
                    getOllamaUrl() + "/api/tags", String.class);
            boolean available = response.getStatusCode().is2xxSuccessful();
            if (!available) {
                log.warn("[AI Connectivity] Ollama returned status {} for /api/tags", response.getStatusCode());
            }
            return available;
        } catch (Exception e) {
            log.error("[AI Connectivity] Failed to reach Ollama at {}: {}", getOllamaUrl(), e.getMessage());
            return false;
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private String getOllamaUrl() {
        return getSettings().getOllamaUrl();
    }

    private String getEmbedModel() {
        return getSettings().getLocalEmbedModel();
    }

    private String getChatModel() {
        return getSettings().getLocalChatModel();
    }

    private String getLightChatModel() {
        return getSettings().getLocalLightModel();
    }

    private SystemSetting.SettingValues getSettings() {
        return settingsRepo.findByScopeAndScopeIdIsNull("GLOBAL")
                .map(it -> it.getSettings())
                .orElse(new SystemSetting.SettingValues());
    }

    private String resolveStageTag(String workflowStage) {
        if (workflowStage == null) return "[SEARCH-MODE]";
        return switch (workflowStage) {
            case "AUTHOR_DRAFT"         -> "[DRAFT-MODE]";
            case "PEER_REVIEW",
                 "QA_REVIEW"            -> "[REVIEW-MODE]";
            case "PUBLISHED"            -> "[AUDIT-MODE]";
            default                     -> "[SEARCH-MODE]";
        };
    }

    private String buildContextBlock(List<String> chunks) {
        if (chunks == null || chunks.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("<context>\n");
        for (int i = 0; i < chunks.size(); i++) {
            sb.append("[").append(i + 1).append("] ").append(chunks.get(i)).append("\n\n");
        }
        sb.append("</context>");
        return sb.toString();
    }
}
