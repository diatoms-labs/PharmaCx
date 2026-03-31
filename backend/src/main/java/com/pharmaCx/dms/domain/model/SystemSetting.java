package com.pharmaCx.dms.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "system_settings")
public class SystemSetting {

    @Id
    private String id;

    // GLOBAL | UNIT | USER
    @Indexed
    private String scope = "GLOBAL";

    // null for GLOBAL; unitId or userId otherwise
    @Indexed
    private String scopeId;

    private SettingValues settings = new SettingValues();

    private String updatedBy;
    private Instant updatedAt;

    public static class SettingValues {
        // ── Access defaults ──────────────────────────────────────────────────────
        private boolean downloadEnabled = false;
        private boolean printEnabled = false;
        private boolean uploadEnabled = true;
        private boolean allowExternalAccess = false;
        private int sessionTimeoutMinutes = 480;
        private int maxFileUploadMb = 50;

        // ── Branding ─────────────────────────────────────────────────────────────
        // Organization name displayed in the sidebar header
        private String orgName = "PharmaCX";
        // App subtitle shown below the org name
        private String orgSubtitle = "Compliance Execution";
        // Logo: base64 data-URL or external HTTPS URL. Null = use default icon.
        private String logoUrl = "/logo.svg";
        // Sidebar background color (hex, e.g. "#1f2937")
        private String sidebarColor = "#0F3D6E";
        // Primary accent / brand color used for buttons, active nav links (hex)
        private String accentColor = "#1E7FC4";
        // Top header background (hex). Null = white/default.
        private String headerColor;

        // AI Configuration
        // LOCAL | HYBRID | CLOUD
        private String aiStrategy = "LOCAL";
        
        // Local (Ollama) Config
        private String ollamaUrl = "http://host.docker.internal:11434";
        private String localEmbedModel = "nomic-embed-text";
        private String localChatModel = "pharma-ai";
        private String localLightModel = "phi3:mini";

        // Cloud Config
        private String cloudAiProvider = "GOOGLE";
        private String cloudAiApiKey;
        private String cloudAiModel = "gemini-1.5-flash";
        
        private String externalKnowledgePath = "/app/background-knowledge";

        // ── Getters & Setters ────────────────────────────────────────────────────
        public boolean isDownloadEnabled() {
            return downloadEnabled;
        }

        public void setDownloadEnabled(boolean downloadEnabled) {
            this.downloadEnabled = downloadEnabled;
        }

        public boolean isPrintEnabled() {
            return printEnabled;
        }

        public void setPrintEnabled(boolean printEnabled) {
            this.printEnabled = printEnabled;
        }

        public boolean isUploadEnabled() {
            return uploadEnabled;
        }

        public void setUploadEnabled(boolean uploadEnabled) {
            this.uploadEnabled = uploadEnabled;
        }

        public boolean isAllowExternalAccess() {
            return allowExternalAccess;
        }

        public void setAllowExternalAccess(boolean allowExternalAccess) {
            this.allowExternalAccess = allowExternalAccess;
        }

        public int getSessionTimeoutMinutes() {
            return sessionTimeoutMinutes;
        }

        public void setSessionTimeoutMinutes(int sessionTimeoutMinutes) {
            this.sessionTimeoutMinutes = sessionTimeoutMinutes;
        }

        public int getMaxFileUploadMb() {
            return maxFileUploadMb;
        }

        public void setMaxFileUploadMb(int maxFileUploadMb) {
            this.maxFileUploadMb = maxFileUploadMb;
        }

        public String getOrgName() {
            return orgName;
        }

        public void setOrgName(String orgName) {
            this.orgName = orgName;
        }

        public String getOrgSubtitle() {
            return orgSubtitle;
        }

        public void setOrgSubtitle(String orgSubtitle) {
            this.orgSubtitle = orgSubtitle;
        }

        public String getLogoUrl() {
            return logoUrl;
        }

        public void setLogoUrl(String logoUrl) {
            this.logoUrl = logoUrl;
        }

        public String getSidebarColor() {
            return sidebarColor;
        }

        public void setSidebarColor(String sidebarColor) {
            this.sidebarColor = sidebarColor;
        }

        public String getAccentColor() {
            return accentColor;
        }

        public void setAccentColor(String accentColor) {
            this.accentColor = accentColor;
        }

        public String getHeaderColor() {
            return headerColor;
        }

        public void setHeaderColor(String headerColor) {
            this.headerColor = headerColor;
        }

        public String getAiStrategy() {
            return aiStrategy;
        }

        public void setAiStrategy(String aiStrategy) {
            this.aiStrategy = aiStrategy;
        }

        public String getCloudAiProvider() {
            return cloudAiProvider;
        }

        public void setCloudAiProvider(String cloudAiProvider) {
            this.cloudAiProvider = cloudAiProvider;
        }

        public String getCloudAiApiKey() {
            return cloudAiApiKey;
        }

        public void setCloudAiApiKey(String cloudAiApiKey) {
            this.cloudAiApiKey = cloudAiApiKey;
        }

        public String getCloudAiModel() {
            return cloudAiModel;
        }

        public void setCloudAiModel(String cloudAiModel) {
            this.cloudAiModel = cloudAiModel;
        }

        public String getExternalKnowledgePath() {
            return externalKnowledgePath;
        }

        public void setExternalKnowledgePath(String externalKnowledgePath) {
            this.externalKnowledgePath = externalKnowledgePath;
        }

        public String getOllamaUrl() {
            return ollamaUrl;
        }

        public void setOllamaUrl(String ollamaUrl) {
            this.ollamaUrl = ollamaUrl;
        }

        public String getLocalEmbedModel() {
            return localEmbedModel;
        }

        public void setLocalEmbedModel(String localEmbedModel) {
            this.localEmbedModel = localEmbedModel;
        }

        public String getLocalChatModel() {
            return localChatModel;
        }

        public void setLocalChatModel(String localChatModel) {
            this.localChatModel = localChatModel;
        }

        public String getLocalLightModel() {
            return localLightModel;
        }

        public void setLocalLightModel(String localLightModel) {
            this.localLightModel = localLightModel;
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    public SettingValues getSettings() {
        return settings;
    }

    public void setSettings(SettingValues settings) {
        this.settings = settings;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
