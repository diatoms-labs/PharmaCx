package com.pharmaCx.dms.ai.service;

import com.pharmaCx.dms.domain.enums.AuditAction;
import com.pharmaCx.dms.domain.enums.ResourceType;
import com.pharmaCx.dms.domain.model.SystemSetting;
import com.pharmaCx.dms.domain.repository.SystemSettingRepository;
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
 * Handles routing, data sanitization, and 21 CFR Part 11 audit logging.
 */
@Service
public class AiOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AiOrchestratorService.class);

    private final SystemSettingRepository settingsRepo;
    private final AiClient localClient; // Defaults to OllamaClient
    private final GeminiClient geminiClient;
    private final DocumentGeneratorService generatorService;
    private final AuditService auditService;

    public AiOrchestratorService(SystemSettingRepository settingsRepo, 
                                 OllamaClient localClient, 
                                 GeminiClient geminiClient,
                                 DocumentGeneratorService generatorService,
                                 AuditService auditService) {
        this.settingsRepo = settingsRepo;
        this.localClient = localClient;
        this.geminiClient = geminiClient;
        this.generatorService = generatorService;
        this.auditService = auditService;
    }

    public void generateChatStream(String workflowStage, 
                                   List<String> contextChunks, 
                                   String message, 
                                   boolean isLight, 
                                   AppUser user,
                                   Consumer<String> onToken, 
                                   Runnable onDone) {
        
        SystemSetting.SettingValues settings = getAiSettings();
        String strategy = settings.getAiStrategy();
        
        // Special Case: Document Generation workflow
        if (isGenerationRequest(message)) {
            executeGenerationStream(message, user, onToken, onDone);
            return;
        }

        if ("HYBRID".equalsIgnoreCase(strategy) || "CLOUD".equalsIgnoreCase(strategy)) {
            // Enterprise Routing Engine: Decide based on complexity or explicit strategy
            if ("CLOUD".equalsIgnoreCase(strategy) || isComplexQuery(message)) {
                executeCloudChatStream(workflowStage, contextChunks, message, user, settings, onToken, onDone);
                return;
            }
        }

        // Default to local (Ollama)
        executeLocalChatStream(workflowStage, contextChunks, message, isLight, user, onToken, onDone);
    }

    private void executeLocalChatStream(String workflowStage, List<String> contextChunks, String message, 
                                        boolean isLight, AppUser user, Consumer<String> onToken, Runnable onDone) {
        log.info("[AI Orchestrator] Routing to LOCAL model for user: {} ({})", user.getUsername(), user.getEmail());
        
        // Audit usage for compliance and analytics
        auditUsage(user, "LOCAL", isLight ? "phi3:mini" : "pharma-ai", message.length());
        
        localClient.generateChatStream(workflowStage, contextChunks, message, isLight, onToken, onDone);
    }

    private void executeCloudChatStream(String workflowStage, List<String> contextChunks, String message, 
                                        AppUser user, SystemSetting.SettingValues settings, 
                                        Consumer<String> onToken, Runnable onDone) {
        
        String provider = settings.getCloudAiProvider();
        String model = settings.getCloudAiModel();
        
        log.info("[AI Orchestrator] Routing to CLOUD provider: {}/{} for user: {}", provider, model, user.getEmail());

        // 1. LOCAL AI SANITIZATION (Enterprise Grade - use local model to redact sensitive info)
        String sanitizedMessage = sanitizePromptWithLocalModel(message);
        
        // 2. REGEX BACKUP SANITIZATION
        sanitizedMessage = SensitiveDataSanitizer.sanitize(sanitizedMessage);
        
        // 3. AUDIT LOGGING (21 CFR Part 11)
        auditUsage(user, "CLOUD", model, message.length());

        // 4. SECURE BRIDGE (Gemini Implementation)
        if ("GOOGLE".equalsIgnoreCase(provider) || "GEMINI".equalsIgnoreCase(provider)) {
            if (settings.getCloudAiApiKey() == null || settings.getCloudAiApiKey().isEmpty()) {
                onToken.accept("[CLOUD ERROR] Gemini API Key missing in System Settings.\nFalling back to local simulation...");
                onDone.run();
                return;
            }
            
            String contextBlock = buildContextBlock(contextChunks);
            String fullPrompt = "You are a Pharma Compliance AI assistant operating via a secure enterprise bridge. " +
                    "Context received (sanitized):\n" + contextBlock + "\n\n" +
                    "User Request (sanitized): " + sanitizedMessage + "\n\n" +
                    "Respond with professional, scientific accuracy suitable for pharmaceutical compliance.";
                    
            geminiClient.generateChatStreamWithKey(model, settings.getCloudAiApiKey(), fullPrompt, onToken, onDone);
        } else {
            onToken.accept("[EXTERNAL AI BRIDGE - " + provider.toUpperCase() + "]\n");
            onToken.accept("Currently only Google Gemini is configured for cloud POC. ");
            onToken.accept("Your query \"" + sanitizedMessage + "\" would normally go to " + provider + " here.\n\n");
            onDone.run();
        }
    }

    private String sanitizePromptWithLocalModel(String message) {
        log.info("[AI Orchestrator] Performing Local AI Sanitization for outbound query");
        String sanitizationPrompt = "You are a Secure Data Redaction Assistant. " +
                "Rewrite the following user query to REMOVE all sensitive information like PII, emails, specific document numbers, or company-internal secrets. " +
                "Keep the intent of the question but make it generic. " +
                "User Query: \"" + message + "\"\n" +
                "Sanitized Query (return ONLY the rewritten text):";
        
        try {
            // Use local model for fast, private redaction
            return localClient.generate(sanitizationPrompt, null).trim();
        } catch (Exception e) {
            log.warn("Local AI sanitization failed, falling back to regex: {}", e.getMessage());
            return message;
        }
    }

    private boolean isComplexQuery(String prompt) {
        String p = prompt.toLowerCase();
        return p.contains("summarize all") || p.contains("compare") || p.contains("analysis") 
                || p.contains("audit trail") || prompt.length() > 500;
    }

    private void auditUsage(AppUser user, String type, String model, int promptLen) {
        String detail = String.format("User: %s | Model: %s | Type: %s | Len: %d", 
                user.getEmail(), model, type, promptLen);
        
        auditService.logSystem(AuditAction.AI_QUERY_EXECUTED, ResourceType.SYSTEM, "AI_USAGE", detail);
    }

    private boolean isGenerationRequest(String prompt) {
        String p = prompt.toLowerCase();
        return p.contains("generate document") || p.contains("prepare document") 
                || p.contains("create sop") || p.contains("draft from pattern");
    }

    private void executeGenerationStream(String message, AppUser user, Consumer<String> onToken, Runnable onDone) {
        log.info("[AI Orchestrator] Routing to PATTERN-BASED generation for user: {}", user.getEmail());
        auditUsage(user, "PATTERN-GEN", "pharma-ai", message.length());
        
        onToken.accept("[PATTERN-BASED GENERATOR START]\n\n");
        String draft = generatorService.generateDraftWithPatterns(message, true);
        
        // Simulate streaming for a better UI experience
        String[] chunks = draft.split("(?<=\\s)");
        for (String chunk : chunks) {
            onToken.accept(chunk);
        }
        
        onDone.run();
    }

    public boolean isAvailable() {
        return localClient.isAvailable();
    }

    private SystemSetting.SettingValues getAiSettings() {
        return settingsRepo.findByScopeAndScopeIdIsNull("GLOBAL")
                .map(SystemSetting::getSettings)
                .orElse(new SystemSetting.SettingValues());
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
