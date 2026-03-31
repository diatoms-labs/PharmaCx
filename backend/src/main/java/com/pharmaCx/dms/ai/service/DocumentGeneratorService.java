package com.pharmaCx.dms.ai.service;

import com.pharmaCx.dms.config.AiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enterprise Service for Pattern-Based Document Generation.
 * 
 * Uses sample SOPs as structural templates to guide AI in generating
 * compliant document drafts. Integrates "External" knowledge fallback.
 * 
 * Architecture:
 * 1. Pattern Extraction: Reads filenames/metadata from sample-documents.
 * 2. Prompt Engineering: Injects structural patterns into AI prompt.
 * 3. Hybrid Completion: Combines local pattern with "external" data tags.
 */
@Service
public class DocumentGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(DocumentGeneratorService.class);
    private final OllamaClient ollamaClient;

    public DocumentGeneratorService(AiConfig aiConfig, OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    /**
     * Generates a structural outline or draft for a new document
     * based on local patterns and user intent.
     */
    public String generateDraftWithPatterns(String query, boolean includeExternal) {
        log.info("[AI Generator] Processing generation query with pattern awareness: {}", query);
        
        List<String> patterns = getAvailablePatterns();
        String patternContext = patterns.stream()
                .limit(5)
                .collect(Collectors.joining(", ", "Available standard patterns: ", ""));

        String prompt = "You are a PharmaCX Document Specialist. Generate a detailed SOP outline for the following request.\n" +
                "Request: \"" + query + "\"\n" +
                "Context: Use the structural patterns found in: " + patternContext + "\n" +
                "Format: Headers, sections, and placeholders for controlled content.\n" +
                (includeExternal ? "Add an [EXTERNAL] tag to any information that comes from general knowledge rather than local patterns." : "") +
                "\nDraft Content:";

        try {
            // Using local client for privacy-first generation
            return ollamaClient.generate(prompt, null);
        } catch (Exception e) {
            log.error("Pattern-based generation failed: {}", e.getMessage());
            return "Failed to generate document draft. AI service is unavailable.";
        }
    }

    private List<String> getAvailablePatterns() {
        try {
            // Path configured in AiConfig or Docker-Compose volume
            Path patternDir = Paths.get("/app/sample-documents");
            if (!Files.exists(patternDir)) {
                // Fallback for local dev if volume not mounted
                patternDir = Paths.get("Documents");
            }
            
            if (Files.exists(patternDir) && Files.isDirectory(patternDir)) {
                return Files.list(patternDir)
                        .map(p -> p.getFileName().toString())
                        .filter(f -> f.endsWith(".docx"))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Could not load sample document patterns: {}", e.getMessage());
        }
        return Collections.emptyList();
    }
}
