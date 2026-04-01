package com.pharmaCx.dms.ai.service;

import com.pharmaCx.dms.config.AiConfig;
import com.pharmaCx.dms.domain.enums.AuditAction;
import com.pharmaCx.dms.domain.enums.ResourceType;
import com.pharmaCx.dms.service.AuditService;
import com.pharmaCx.dms.utils.SensitiveDataSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.pharmaCx.dms.domain.model.AppUser;
import java.util.List;
import java.util.function.Consumer;

/**
 * Enterprise Orchestrator for Hybrid AI.
 * Routes requests based on AiConfig (Docker environment variables).
 * Handles data sanitization and 21 CFR Part 11 audit logging.
 */
@Service
public class AiOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AiOrchestratorService.class);

    private final AiConfig aiConfig;
    private final AiClient localClient;
    private final GeminiClient geminiClient;
    private final AuditService auditService;

    public AiOrchestratorService(AiConfig aiConfig, 
                                 OllamaClient localClient, 
                                 GeminiClient geminiClient,
                                 AuditService auditService) {
        this.aiConfig = aiConfig;
        this.localClient = localClient;
        this.geminiClient = geminiClient;
        this.auditService = auditService;
    }

    public void generateChatStream(String workflowStage, 
                                   List<String> contextChunks, 
                                   String message, 
                                   boolean isLight, 
                                   AppUser user,
                                   Consumer<String> onToken, 
                                   Runnable onDone) {
        
        String strategy = aiConfig.getStrategy();
        
        if ("HYBRID".equalsIgnoreCase(strategy) || "CLOUD".equalsIgnoreCase(strategy)) {
            if ("CLOUD".equalsIgnoreCase(strategy) || isComplexQuery(message)) {
                executeCloudChatStream(workflowStage, contextChunks, message, user, onToken, onDone);
                return;
            }
        }

        executeLocalChatStream(workflowStage, contextChunks, message, isLight, user, onToken, onDone);
    }

    private void executeLocalChatStream(String workflowStage, List<String> contextChunks, String message, 
                                        boolean isLight, AppUser user, Consumer<String> onToken, Runnable onDone) {
        log.info("[AI Orchestrator] Routing to LOCAL model for user: {}", user.getUsername());
        
        auditUsage(user, "LOCAL", isLight ? aiConfig.getLightChatModel() : aiConfig.getChatModel(), message.length());
        
        localClient.generateChatStream(workflowStage, contextChunks, message, isLight, onToken, onDone);
    }

    private void executeCloudChatStream(String workflowStage, List<String> contextChunks, String message, 
                                        AppUser user, Consumer<String> onToken, Runnable onDone) {
        
        String provider = aiConfig.getCloudProvider();
        String model = aiConfig.getCloudModel();
        
        log.info("[AI Orchestrator] Routing to CLOUD provider: {}/{} for user: {}", provider, model, user.getEmail());

        // 1. LOCAL AI SANITIZATION (Enterprise Grade)
        String sanitizedMessage = sanitizePromptWithLocalModel(message);
        
        // 2. REGEX BACKUP SANITIZATION
        sanitizedMessage = SensitiveDataSanitizer.sanitize(sanitizedMessage);
        
        // 3. AUDIT LOGGING (21 CFR Part 11)
        auditUsage(user, "CLOUD", model, message.length());

        // 4. SECURE BRIDGE (Gemini Implementation)
        if ("GOOGLE".equalsIgnoreCase(provider) || "GEMINI".equalsIgnoreCase(provider)) {
            String apiKey = aiConfig.getCloudApiKey();
            if (apiKey == null || apiKey.isEmpty()) {
                onToken.accept("[CLOUD ERROR] API Key missing in infrastructure config.\n");
                onDone.run();
                return;
            }
            
            String contextBlock = buildContextBlock(contextChunks);
            String fullPrompt = "You are a Pharma Compliance AI assistant operating via a secure enterprise bridge.\n\n" +
                    "Context received:\n" + contextBlock + "\n\n" +
                    "User Request: " + sanitizedMessage;
                    
            geminiClient.generateChatStreamWithKey(model, apiKey, fullPrompt, onToken, onDone);
        } else {
            onToken.accept("[EXTERNAL BRIDGE - " + provider.toUpperCase() + "]\n");
            onToken.accept("Currently only Google Gemini is configured for cloud POC.\n");
            onDone.run();
        }
    }

    private String sanitizePromptWithLocalModel(String message) {
        String sanitizationPrompt = "Rewrite following query to REMOVE all sensitive information like PII or secrets. " +
                "User Query: \"" + message + "\"\n" +
                "Sanitized Query (return ONLY text):";
        
        try {
            return localClient.generate(sanitizationPrompt, null).trim();
        } catch (Exception e) {
            log.warn("[Sanitizer] Local AI failed, falling back to regex: {}", e.getMessage());
            return message;
        }
    }

    private boolean isComplexQuery(String prompt) {
        String p = prompt.toLowerCase();
        return p.contains("summarize all") || p.contains("compare") || p.contains("analysis") || prompt.length() > 500;
    }

    private void auditUsage(AppUser user, String type, String model, int promptLen) {
        String detail = String.format("Model: %s | Type: %s | Len: %d", model, type, promptLen);
        auditService.logSystem(AuditAction.AI_QUERY_EXECUTED, ResourceType.SYSTEM, "AI_USAGE", detail);
    }

    public boolean isAvailable() {
        return localClient.isAvailable();
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
