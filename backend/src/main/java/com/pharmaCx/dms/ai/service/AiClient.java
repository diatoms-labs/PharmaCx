package com.pharmaCx.dms.ai.service;

import java.util.List;
import java.util.function.Consumer;

/**
 * Enterprise-grade interface for AI providers.
 * Allows switching between Local (Ollama) and Cloud (Gemini, Claude, etc.).
 */
public interface AiClient {
    
    String generate(String prompt, String context);
    
    void generateChatStream(String workflowStage, 
                            List<String> contextChunks, 
                            String message, 
                            boolean isLight, 
                            Consumer<String> onToken, 
                            Runnable onDone);
    
    List<Double> embedText(String text);
    
    boolean isAvailable();
}
