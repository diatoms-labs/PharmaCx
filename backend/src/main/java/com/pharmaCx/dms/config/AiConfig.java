package com.pharmaCx.dms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enterprise AI Configuration.
 * 
 * Mapped from application.yml (prefix: app.ai) which in turn
 * pull from Docker environment variables (AI_*).
 * 
 * This is now the SOLE source of AI configuration for the application,
 * completely replacing the legacy database-backed SystemSettings.
 */
@Configuration
@ConfigurationProperties(prefix = "app.ai")
public class AiConfig {

    // --- Core Ollama Connectivity ---
    private String ollamaUrl = "http://localhost:11434";
    private String embedModel = "nomic-embed-text";
    private String chatModel = "helix-ai";
    private String lightChatModel = "phi3:mini";

    // --- Enterprise Strategy Management ---
    private String strategy = "LOCAL"; // LOCAL, HYBRID, CLOUD
    
    // --- Cloud Bridge Configuration ---
    private String cloudProvider = "GOOGLE";
    private String cloudApiKey;
    private String cloudModel = "gemini-1.5-flash";

    // --- Indexing & Search Optimization ---
    private int indexChunkWords = 500;
    private int indexChunkOverlap = 50;
    private int searchTopK = 20;

    // --- Infrastructure Paths (Standardized) ---
    private String backgroundKnowledgePath;
    private String publishedDocsPath;
    private String hostKnowledgePath;
    private String hostPublishedDocsPath;
    private boolean copyOnPublish = true;

    // --- Getters & Setters ---
    public String getOllamaUrl() { return ollamaUrl; }
    public void setOllamaUrl(String ollamaUrl) { this.ollamaUrl = ollamaUrl; }

    public String getEmbedModel() { return embedModel; }
    public void setEmbedModel(String embedModel) { this.embedModel = embedModel; }

    public String getChatModel() { return chatModel; }
    public void setChatModel(String chatModel) { this.chatModel = chatModel; }

    public String getLightChatModel() { return lightChatModel; }
    public void setLightChatModel(String lightChatModel) { this.lightChatModel = lightChatModel; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public String getCloudProvider() { return cloudProvider; }
    public void setCloudProvider(String cloudProvider) { this.cloudProvider = cloudProvider; }

    public String getCloudApiKey() { return cloudApiKey; }
    public void setCloudApiKey(String cloudApiKey) { this.cloudApiKey = cloudApiKey; }

    public String getCloudModel() { return cloudModel; }
    public void setCloudModel(String cloudModel) { this.cloudModel = cloudModel; }

    public int getIndexChunkWords() { return indexChunkWords; }
    public void setIndexChunkWords(int indexChunkWords) { this.indexChunkWords = indexChunkWords; }

    public int getIndexChunkOverlap() { return indexChunkOverlap; }
    public void setIndexChunkOverlap(int indexChunkOverlap) { this.indexChunkOverlap = indexChunkOverlap; }

    public int getSearchTopK() { return searchTopK; }
    public void setSearchTopK(int searchTopK) { this.searchTopK = searchTopK; }

    public String getBackgroundKnowledgePath() { return backgroundKnowledgePath; }
    public void setBackgroundKnowledgePath(String backgroundKnowledgePath) { this.backgroundKnowledgePath = backgroundKnowledgePath; }

    public String getPublishedDocsPath() { return publishedDocsPath; }
    public void setPublishedDocsPath(String publishedDocsPath) { this.publishedDocsPath = publishedDocsPath; }

    public String getHostKnowledgePath() { return hostKnowledgePath; }
    public void setHostKnowledgePath(String hostKnowledgePath) { this.hostKnowledgePath = hostKnowledgePath; }

    public String getHostPublishedDocsPath() { return hostPublishedDocsPath; }
    public void setHostPublishedDocsPath(String hostPublishedDocsPath) { this.hostPublishedDocsPath = hostPublishedDocsPath; }

    public boolean isCopyOnPublish() { return copyOnPublish; }
    public void setCopyOnPublish(boolean copyOnPublish) { this.copyOnPublish = copyOnPublish; }
}
