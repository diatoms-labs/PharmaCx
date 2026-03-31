package com.pharmaCx.dms.api.dto;

import java.util.Map;

public class EditorConfigResponse {

    private String documentServerUrl;
    private Map<String, Object> config;
    private String mode;

    public EditorConfigResponse() {
    }

    public EditorConfigResponse(String documentServerUrl, Map<String, Object> config, String mode) {
        this.documentServerUrl = documentServerUrl;
        this.config = config;
        this.mode = mode;
    }

    public String getDocumentServerUrl() {
        return documentServerUrl;
    }

    public void setDocumentServerUrl(String documentServerUrl) {
        this.documentServerUrl = documentServerUrl;
    }

    public Map<String, Object> getConfig() {
        return config;
    }

    public void setConfig(Map<String, Object> config) {
        this.config = config;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
