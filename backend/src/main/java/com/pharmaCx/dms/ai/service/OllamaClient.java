package com.pharmaCx.dms.ai.service;

import com.pharmaCx.dms.config.AiConfig;
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
 * Enterprise HTTP client for the local Ollama REST API.
 * 
 * Logic is driven entirely by AiConfig (Docker environment variables).
 */
@Service
public class OllamaClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final AiConfig aiConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaClient(AiConfig aiConfig, 
                        RestTemplate restTemplate, 
                        ObjectMapper objectMapper) {
        this.aiConfig = aiConfig;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<Double> embedText(String text) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", aiConfig.getEmbedModel());
            body.put("prompt", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    aiConfig.getOllamaUrl() + "/api/embeddings", request, String.class);

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
            log.warn("[AI Embed] Failed for model {}: {}", aiConfig.getEmbedModel(), e.getMessage());
        }
        return Collections.emptyList();
    }

    @Override
    public String generate(String prompt, String context) {
        return generateChat(null, context != null ? List.of(context) : Collections.emptyList(), prompt, false);
    }

    public String generateChat(String workflowStage, List<String> contextChunks, String userMessage, boolean isLight) {
        try {
            String stageTag = resolveStageTag(workflowStage);
            String contextBlock = buildContextBlock(contextChunks);
            String instruction = "IMPORTANT: Use natural language with proper spaces, punctuation, AND clear paragraph breaks.";
            String fullPrompt = stageTag + "\n" + instruction + "\n\n" + contextBlock + "\n\nUser: " + userMessage;

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", isLight ? aiConfig.getLightChatModel() : aiConfig.getChatModel());
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
                    aiConfig.getOllamaUrl() + "/api/generate", request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode responseNode = root.get("response");
                if (responseNode != null) {
                    return responseNode.asText();
                }
            }
            return "AI service returned an unexpected response.";
        } catch (Exception e) {
            log.error("[AI Chat] Generation failed for model {}: {}", 
                    isLight ? aiConfig.getLightChatModel() : aiConfig.getChatModel(), e.getMessage());
            return "AI service is currently unavailable.";
        }
    }

    @Override
    public void generateChatStream(String workflowStage, List<String> contextChunks,
                                   String userMessage, boolean isLight, Consumer<String> onToken, Runnable onComplete) {
        try {
            String stageTag = resolveStageTag(workflowStage);
            String contextBlock = buildContextBlock(contextChunks);
            String instruction = "IMPORTANT: Use natural language with proper spaces and clear paragraph breaks.";
            String fullPrompt = stageTag + "\n" + instruction + "\n\n" + contextBlock + "\n\nUser: " + userMessage;

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", isLight ? aiConfig.getLightChatModel() : aiConfig.getChatModel());
            body.put("prompt", fullPrompt);
            body.put("stream", true);

            ObjectNode options = objectMapper.createObjectNode();
            options.put("temperature", 0.1);
            options.put("num_predict", 128);
            body.set("options", options);

            byte[] requestBytes = objectMapper.writeValueAsBytes(body);
            URL url = URI.create(aiConfig.getOllamaUrl() + "/api/generate").toURL();

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
                        onToken.accept(token);
                    }
                    if (node.path("done").asBoolean(false)) break;
                }
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            log.error("[AI Stream] Generation failed: {}", e.getMessage());
            onToken.accept("AI service is currently unavailable.");
        } finally {
            onComplete.run();
        }
    }

    public boolean isAvailable() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    aiConfig.getOllamaUrl() + "/api/tags", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("[AI Connectivity] Failed to reach Ollama at {}: {}", aiConfig.getOllamaUrl(), e.getMessage());
            return false;
        }
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
